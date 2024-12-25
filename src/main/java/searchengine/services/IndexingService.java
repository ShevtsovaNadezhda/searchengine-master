package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    void deleteSiteInBase();

    void addSiteInBase();

    IndexingResponse startIndexingResponse();
    IndexingResponse stopIndexingResponse();
}
