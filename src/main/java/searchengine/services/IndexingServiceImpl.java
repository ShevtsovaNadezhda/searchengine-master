package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.StatusEnum;
import searchengine.repositories.PageRepo;
import searchengine.repositories.SiteRepo;

import java.io.IOException;
import java.time.LocalDateTime;
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

    @Override
    public void addSiteInBase() {
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

                if(isIndexing) {
                    s.setStatus(StatusEnum.INDEXED);

                    long countIndexed = sites.stream()
                            .filter(site -> site.getStatus().equals(StatusEnum.INDEXING))
                            .count();

                    if (countIndexed == 0) {
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
            addSiteInBase();
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
    public void indexPage(String url) throws IOException {
        Iterable<PageModel> pageIterable = pageRepo.findAll();
        boolean pageInRepo = false;
        for (PageModel page : pageIterable) {
            if(page.pageUrl().equals(url)) {
                page.checkStatusCode();
                if(page.getCode() == 200) {
                    page.getPageContent();
                } else {
                    page.setContent("Страница недоступна");
                }
                pageRepo.save(page);
                pageInRepo = true;
                System.out.println("Страница обновлена");
                break;
            }
        }
        if(!pageInRepo) {
            PageModel newPage = new PageModel();

            Iterable<SiteModel> siteIterable = siteRepo.findAll();
            for (SiteModel site : siteIterable) {
                if(url.contains(site.getUrl())) {
                    newPage.setSite(site);
                    String path = url.replaceAll(site.getUrl(), "");
                    newPage.setPath(path);
                    break;
                }
            }
            newPage.checkStatusCode();
            if (newPage.getCode() == 200) {
                newPage.getPageContent();
            } else {
                newPage.setContent("Страница недоступна");
            }
            pageRepo.save(newPage);
            System.out.println("Новая страница проиндексирована");
        }
    }

    @Override
    public boolean checkUrl(String url) {
        return siteService.getSiteList().stream().anyMatch(siteModel -> url.contains(siteModel.getUrl()));
    }

    @Override
    public IndexingResponse indexingPageResponse(String url) {
        IndexingResponse response = new IndexingResponse();
        if(checkUrl(url)) {
            try {
                indexPage(url);
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
