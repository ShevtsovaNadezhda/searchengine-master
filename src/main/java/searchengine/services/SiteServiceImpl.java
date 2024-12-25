package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;

import java.util.ArrayList;
import java.util.List;

@Service
public class SiteServiceImpl implements SiteService{
    private final SitesList sites;

    public SiteServiceImpl(SitesList sites) {
        this.sites = sites;
    }

    @Override
    public List<SiteModel> getSiteList() {
        List<SiteModel> sitesInConfig = new ArrayList<>();
        sites.getSites().forEach(site -> {
            SiteModel siteModel = new SiteModel();
            siteModel.setUrl(site.getUrl());
            siteModel.setName(site.getName());
            sitesInConfig.add(siteModel);
        });
        return sitesInConfig;
    }
}
