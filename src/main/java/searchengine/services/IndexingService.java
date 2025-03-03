package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.IndexingException;
import searchengine.model.PageModel;

import java.io.IOException;

public interface IndexingService {
    public boolean getIndexing();

    void deleteSiteInBase();

    void indexingSiteList();

    IndexingResponse startIndexingResponse() throws Exception;

    IndexingResponse stopIndexingResponse() throws IndexingException;

    boolean urlContainsInConfig(String url);

    boolean urlContainsInSiteRepo(String url);

    boolean urlContainsInPageRepo(String url);

    void addUrlInSiteRepo(String url);

    void addUrlInPageRepo(String url) throws IOException;

    void deletePageInfoInBase(String url) throws IOException;

    IndexingResponse indexingPageResponse(String url) throws IndexingException;

    void indexingPage(String url) throws IOException;

    void collectLemmas(PageModel page) throws IOException;
}
