package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepo;
import searchengine.repositories.LemmaRepo;
import searchengine.repositories.PageRepo;
import searchengine.repositories.SiteRepo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private static volatile boolean isIndexing = false;
    private final SiteServiceImpl siteService;
    private final SiteRepo siteRepo;
    private final PageRepo pageRepo;
    private final LemmaRepo lemmaRepo;
    private final IndexRepo indexRepo;

    private final SitesList sites;


    @Override
    public boolean getIndexing() {
        return isIndexing;
    }

    @Override
    public void deleteSiteInBase() {
        Iterable<SiteModel> siteIterable = siteRepo.findAll();
        for (SiteModel siteInConfig : siteService.getSiteList()) {
            for (SiteModel site : siteIterable) {
                if (siteInConfig.getUrl().equalsIgnoreCase(site.getUrl())) {
                    siteRepo.delete(site);
                }
            }
        }
    }

    //Индексация всего списка сайтов
    @Override
    public void indexingSiteList() {
        List<SiteModel> sites = siteService.getSiteList();

        ExecutorService service = Executors.newFixedThreadPool(sites.size());
        sites.forEach(s -> {
            service.submit(() -> {
                s.setStatus(StatusEnum.INDEXING);
                s.setStatusTime(LocalDateTime.now());
                siteRepo.save(s);

                PageModel root = new PageModel();
                root.setSite(s);
                root.setPath("/");
                root.setContent("Indexing");

                ForkJoinPool pool = new ForkJoinPool(8);
                pool.invoke(new PageParser(s, root, siteRepo, pageRepo, this));

                if (isIndexing) {
                    s.setStatus(StatusEnum.INDEXED);

                    long countIndexing = sites.stream()
                            .filter(site -> site.getStatus().equals(StatusEnum.INDEXING))
                            .count();

                    if (countIndexing == 0) {
                        isIndexing = false;
                        System.out.println("Парсинг прошел до конца. Флаг сменился на false");
                    }
                } else {
                    s.setStatus(StatusEnum.FAILED);
                    s.setLastError("Индексация остановлена пользователем");
                }

                s.setStatusTime(LocalDateTime.now());
                siteRepo.save(s);
            });
        });

        service.shutdown();
    }

    @Override
    public IndexingResponse startIndexingResponse() {
        if (isIndexing) {
            IndexingResponse response = new IndexingResponse();
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        } else {
            isIndexing = true;
            deleteSiteInBase();
            indexingSiteList();

            IndexingResponse response = new IndexingResponse();
            response.setResult(true);
            return response;
        }
    }

    @Override
    public IndexingResponse stopIndexingResponse() {
        IndexingResponse response = new IndexingResponse();
        if (isIndexing) {
            isIndexing = false;
            response.setResult(true);
        } else {
            response.setResult(false);
            response.setError("Индексация не запущена");
        }
        return response;
    }

    @Override
    public void indexingPage(String url) throws IOException {
        if (urlContainsInSiteRepo(url)) {
            if (urlContainsInPageRepo(url)) {
                deletePageInfoInBase(url);
                addUrlInPageRepo(url);
            } else {
                addUrlInPageRepo(url);
            }
        } else {
            addUrlInSiteRepo(url);
            addUrlInPageRepo(url);
        }
    }

    @Override
    public void collectLemmas(PageModel page) throws IOException {
        HashMap<String, Integer> lemmas = page.pageLemmatization();

        lemmas.keySet().forEach(lemmaKey -> {
            LemmaModel lemma = new LemmaModel();
            lemma.setLemma(lemmaKey);
            lemma.setSite(page.getSite());
            lemma.setFrequency(1);
            IndexModel index = new IndexModel();
            index.setPage(page);
            index.setRanks(lemmas.get(lemmaKey));
            Iterable<LemmaModel> lemmaIterable = lemmaRepo.findAll();
            boolean lemmaInRepo = false;
            for (LemmaModel l : lemmaIterable) {
                if (l.equals(lemma)) {
                    l.setFrequency(l.getFrequency() + 1);
                    lemmaRepo.save(l);
                    index.setLemma(l);
                    lemmaInRepo = true;
                    break;
                }
            }
            if (!lemmaInRepo) {
                lemmaRepo.save(lemma);
                index.setLemma(lemma);
            }
            indexRepo.save(index);
        });

    }

    @Override
    public boolean urlContainsInConfig(String url) {
        return siteService.getSiteList().stream().anyMatch(siteModel -> url.contains(siteModel.getUrl()));
    }

    @Override
    public boolean urlContainsInSiteRepo(String url) {
        Iterable<SiteModel> siteModelIterable = siteRepo.findAll();
        boolean urlInSiteRepo = false;
        for (SiteModel site : siteModelIterable) {
            if (url.contains(site.getUrl())) {
                urlInSiteRepo = true;
                break;
            }
        }
        return urlInSiteRepo;
    }

    @Override
    public boolean urlContainsInPageRepo(String url) {
        Iterable<PageModel> pageModelIterable = pageRepo.findAll();
        boolean urlInPageRepo = false;
        for (PageModel page : pageModelIterable) {
            if (page.getPageUrl().equals(url)) {
                urlInPageRepo = true;
                break;
            }
        }
        return urlInPageRepo;
    }

    @Override
    public void addUrlInSiteRepo(String url) {
        List<SiteModel> sites = siteService.getSiteList();
        for (SiteModel site : sites) {
            if (url.contains(site.getUrl())) {
                site.setStatus(StatusEnum.INDEXED);
                site.setStatusTime(LocalDateTime.now());
                siteRepo.save(site);
                break;
            }
        }
    }

    @Override
    public void addUrlInPageRepo(String url) throws IOException {
        PageModel newPage = new PageModel();

        Iterable<SiteModel> siteIterable = siteRepo.findAll();
        for (SiteModel site : siteIterable) {
            if (url.contains(site.getUrl())) {
                newPage.setSite(site);
                String path = url.replaceAll(site.getUrl(), "");
                newPage.setPath(path);
                break;
            }
        }
        newPage.checkStatusCode();
        if (newPage.getCode() == 200) {
            newPage.setContent(newPage.getPageContentInDoc().toString());
            pageRepo.save(newPage);
            collectLemmas(newPage);
            System.out.println("Новая страница проиндексирована");
        } else {
            newPage.setContent("Страница недоступна");
        }
    }

    @Override
    public void deletePageInfoInBase(String url) throws IOException {
        Iterable<PageModel> pageIterable = pageRepo.findAll();
        for (PageModel page : pageIterable) {
            if (page.getPageUrl().equals(url)) {
                HashMap<String, Integer> lemmasMap = page.pageLemmatization();
                Iterable<LemmaModel> lemmaModelIterable = lemmaRepo.findAll();
                lemmasMap.keySet().forEach(lemmaKey -> {
                    for (LemmaModel lemma : lemmaModelIterable) {
                        if (lemma.getLemma().equals(lemmaKey) && lemma.getFrequency() == 1) {
                            lemmaRepo.delete(lemma);
                            break;
                        } else if (lemma.getLemma().equals(lemmaKey) && lemma.getFrequency() > 1) {
                            lemma.setFrequency(lemma.getFrequency() - 1);
                            lemmaRepo.save(lemma);
                            break;
                        }
                    }
                });
                pageRepo.delete(page);

                System.out.println("Страница удалена из базы данных");
                break;
            }
        }
    }

    @Override
    public IndexingResponse indexingPageResponse(String url) {
        IndexingResponse response = new IndexingResponse();
        if (urlContainsInConfig(url)) {
            try {
                indexingPage(url);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            response.setResult(true);
        } else {
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        return response;
    }

}
