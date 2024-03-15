package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.*;
import searchengine.repositories.PageRepo;
import searchengine.repositories.SiteRepo;
import searchengine.services.StatisticsService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


@RestController
@RequestMapping("/api")
public class ApiController {

    private final SitesList sites;
    private boolean indexingIsRunning = false;
    private ExecutorService service;

    @Autowired
    private SiteRepo siteRepo;
    @Autowired
    private PageRepo pageRepo;

    private final StatisticsService statisticsService;

    public ApiController(SitesList sites, StatisticsService statisticsService) {
        this.sites = sites;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startindexing")
    public Response startIndexing() throws Exception{
        if (indexingIsRunning) {
            Response response = new Response();
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return response;
        } else {
            indexingIsRunning = true;
            List<SiteModel> sitesInFile = new ArrayList<>();
            sites.getSites().forEach(site -> {
                SiteModel siteModel = new SiteModel();
                siteModel.setUrl(site.getUrl());
                siteModel.setName(site.getName());
                sitesInFile.add(siteModel);
            });

            deleteSiteInBase(sitesInFile);
            addSiteInBase(sitesInFile);
            Response response = new Response();
            response.setResult(true);
            indexingIsRunning = false;
            return response;
        }
    }

    //Ищем в БД сайты из конфигурационного файла, удаляем записи
    public void deleteSiteInBase(List<SiteModel> siteList) throws Exception{
        Iterable<SiteModel> siteIterable = siteRepo.findAll();
        for (SiteModel siteInList : siteList) {
            for (SiteModel site : siteIterable) {
                if (siteInList.getUrl().equalsIgnoreCase(site.getUrl())) {
                    siteRepo.delete(site);
                }
            }
        }
    }

    //Создаем новые записи в БД согласно списку
    public void addSiteInBase(List<SiteModel> siteList) throws InterruptedException {
        service = Executors.newFixedThreadPool(siteList.size());
        siteList.forEach(s -> {
            service.submit(() -> {
                s.setStatus(StatusEnum.INDEXING);
                s.setStatusTime(LocalDateTime.now());
                siteRepo.save(s);

                PageModel root = new PageModel();
                root.setSite(s);
                root.setPath("/");
                root.setContent("Indexing");

                try {
                    new ForkJoinPool(8).invoke(new PageParser(s, root, siteRepo, pageRepo));
                } catch (Exception exception) {
                    s.setStatus(StatusEnum.FAILED);
                    s.setLastError("Не удалось индексировать сайт - " + exception.getMessage());
                    siteRepo.save(s);
                    return;
                }
                s.setStatus(StatusEnum.INDEXED);
                s.setStatusTime(LocalDateTime.now());
                siteRepo.save(s);
            });
        });

        service.shutdown();
        if (service.awaitTermination(1, TimeUnit.HOURS)) {
            System.out.println("Все сайты проиндексированы");
        } else {
            service.shutdownNow();
        }
    }

    @GetMapping("/stopindexing")
    public Response stopIndexing() {
        Response response = new Response();
        if (indexingIsRunning) {
            service.shutdownNow();
            response.setResult(true);
            indexingIsRunning = false;
        } else {
            response.setResult(false);
            response.setError("Индексация не запущена");
        }
        return response;
    }
}
