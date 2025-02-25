package searchengine.services;

import searchengine.dto.search.SearchResponse;

public interface SearchService {
    public SearchResponse searching(String query, String site, Integer offset, Integer limit);
}
