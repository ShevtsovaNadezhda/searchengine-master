package searchengine.services;

import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.SearchException;

public interface SearchService {
    public SearchResponse searching(String query, String site, int offset, int limit) throws Exception;
}
