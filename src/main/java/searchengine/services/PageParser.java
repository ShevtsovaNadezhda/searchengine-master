package searchengine.services;

import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.PageRepo;
import searchengine.repositories.SiteRepo;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.RecursiveAction;

public class PageParser extends RecursiveAction {

    private SiteModel site;
    private PageModel nodePage;

    private SiteRepo siteRepo;
    private PageRepo pageRepo;
    private IndexingServiceImpl indexingService;

    public PageParser(SiteModel site, PageModel nodePage, SiteRepo siteRepo, PageRepo pageRepo, IndexingServiceImpl indexingService) {
        this.site = site;
        this.nodePage = nodePage;
        this.siteRepo = siteRepo;
        this.pageRepo = pageRepo;
        this.indexingService = indexingService;
    }

    @Override
    protected void compute() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(indexingService.getIndexing()) {
            HashSet<PageParser> taskList = new HashSet<>();
            HashSet<PageModel> nodes = null;
            try {
                nodes = nodePage.getChildren();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (!site.getPages().contains(nodePage)) {
                site.addPageInSet(nodePage);
                pageRepo.save(nodePage);
                //site.setStatusTime(LocalDateTime.now());
                //siteRepo.save(site);
            }

            for (PageModel node : nodes) {
                if (!site.getPages().contains(node)) {
                    PageParser task = new PageParser(site, node, siteRepo, pageRepo, indexingService);
                    taskList.add(task);
                    task.fork();
                }
            }

            for (PageParser task : taskList) {
                task.join();
            }
        }
    }
}
