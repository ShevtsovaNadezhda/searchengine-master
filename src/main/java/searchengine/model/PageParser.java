package searchengine.model;

import searchengine.repositories.PageRepo;
import searchengine.repositories.SiteRepo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.concurrent.RecursiveAction;

public class PageParser extends RecursiveAction {
    private SiteModel site;
    private PageModel nodePage;

    private volatile SiteRepo siteRepo;
    private volatile PageRepo pageRepo;


    public PageParser(SiteModel site, PageModel nodePage, SiteRepo siteRepo, PageRepo pageRepo) {
        this.site = site;
        this.nodePage = nodePage;
        this.siteRepo = siteRepo;
        this.pageRepo = pageRepo;
    }

    @Override
    protected void compute() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        HashSet<PageParser> taskList = new HashSet<>();
        HashSet<PageModel> nodes = null;
        try {
            nodes = nodePage.getChildren();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!site.getPages().contains(nodePage)) {
            site.add(nodePage);
            pageRepo.save(nodePage);
            //site.setStatusTime(LocalDateTime.now());
            //siteRepo.save(site);
        }

        for (PageModel node : nodes) {
            if (!site.getPages().contains(node)) {
                PageParser task = new PageParser(site, node, siteRepo, pageRepo);
                taskList.add(task);
                task.fork();
            }
        }

        for (PageParser task : taskList) {
            task.join();
        }
    }
}
