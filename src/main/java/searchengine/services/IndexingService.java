package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    public boolean getIndexing();
    void deleteSiteInBase();

    void indexingSiteList();

    IndexingResponse startIndexingResponse();
    IndexingResponse stopIndexingResponse();
    IndexingResponse indexingPageResponse(String url);
}
