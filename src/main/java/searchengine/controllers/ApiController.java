package searchengine.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;


@RestController
@RequestMapping("/api")
public class ApiController {

    private final SitesList sites;
    private boolean isRunning = false;

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
    public boolean startIndexing() throws Exception{
        List<SiteModel> sitesInFile = new ArrayList<>();
        sites.getSites().forEach(site -> {
            SiteModel siteModel = new SiteModel();
            siteModel.setUrl(site.getUrl());
            siteModel.setName(site.getName());
            sitesInFile.add(siteModel);
        });

        deleteSiteInBase(sitesInFile);
        addSiteInBase(sitesInFile);
        return true;

    }

    //Ищем в БД сайты из конфигурационного файла, удаляем записи
    public void deleteSiteInBase(List<SiteModel> siteList) throws Exception{
        Iterable<SiteModel> siteIterable = siteRepo.findAll();
        Iterable<PageModel> pageIterable = pageRepo.findAll();
        for (SiteModel siteInList : siteList) {
            for (SiteModel site : siteIterable) {
                if (siteInList.getUrl().equalsIgnoreCase(site.getUrl())) {
                    for (PageModel page : pageIterable) {
                        if (page.getSite().getId() == site.getId()) {
                            pageRepo.delete(page);
                        }
                    }
                    siteRepo.delete(site);
                }
            }
        }
    }

    //Создаем новые записи в БД согласно списку
    public void addSiteInBase(List<SiteModel> siteList) {
        /*siteList.forEach(s -> {
            SiteParsing siteParsing = new SiteParsing(s);
            Thread thread = new Thread(siteParsing);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            siteRepo.save(s);
            s.getPages().forEach(p -> pageRepo.save(p));
        });*/

        siteList.forEach(s -> {
            s.setStatus(StatusEnum.INDEXING);
            s.setStatusTime(LocalDateTime.now());
            siteRepo.save(s);

            PageModel root = new PageModel();
            root.setSite(s);
            root.setPath("/");
            root.setContent("Indexing");

            s.add(root);

            new ForkJoinPool(8).invoke(new PageParser(s, root));

            s.getPages().forEach(page -> pageRepo.save(page));
            s.setStatus(StatusEnum.INDEXED);
            s.setStatusTime(LocalDateTime.now());
            siteRepo.save(s);
        });
    }

    @GetMapping("/stopindexing")
    public boolean stopIndexing() {
        if (isRunning) {
            return true;
        } else {
            return false;
        }
    }
}
