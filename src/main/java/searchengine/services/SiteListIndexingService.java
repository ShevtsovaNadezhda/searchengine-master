package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

import java.io.IOException;

public interface SiteListIndexingService {
    public boolean getIndexing();
    void deleteSiteInBase();

    void indexingSiteList();

    IndexingResponse startIndexingResponse();
    IndexingResponse stopIndexingResponse();
    IndexingResponse indexingPageResponse(String url);
}
