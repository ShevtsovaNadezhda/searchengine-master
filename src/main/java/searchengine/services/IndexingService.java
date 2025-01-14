package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

import java.io.IOException;

public interface IndexingService {
    void deleteSiteInBase();

    void addSiteInBase();

    IndexingResponse startIndexingResponse();
    IndexingResponse stopIndexingResponse();

    void indexPage(String url) throws IOException;

    boolean checkUrl(String url);

    IndexingResponse indexingPageResponse(String url);
}
