package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.exception.ExceptionResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.exceptions.SearchException;
import searchengine.exceptions.IndexingException;
import searchengine.services.implementation.SearchServiceImpl;
import searchengine.services.implementation.IndexingServiceImpl;
import searchengine.services.StatisticsService;

import java.io.IOException;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final IndexingServiceImpl indexingService;
    private final StatisticsService statisticsService;
    private final SearchServiceImpl searchService;

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    @GetMapping("/startIndexing")
    public IndexingResponse startIndexing() throws Exception {
        return indexingService.startIndexingResponse();
    }

    @GetMapping("/stopIndexing")
    public IndexingResponse stopIndexing() throws IndexingException {
        return indexingService.stopIndexingResponse();
    }

    @PostMapping("/indexPage")
    public IndexingResponse indexPage(@RequestParam String url) throws IndexingException {
        return indexingService.indexingPageResponse(url);
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String query,
                                 @RequestParam(required = false) String site,
                                 @RequestParam(required = false, defaultValue = "0") int offset,
                                 @RequestParam(required = false, defaultValue = "10") int limit)
            throws Exception {
        return searchService.searching(query, site, offset, limit);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({IndexingException.class, SearchException.class})
    public ExceptionResponse handleException(Exception exception) {
        return new ExceptionResponse(false, exception.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(IOException.class)
    public ExceptionResponse handleIOException(IOException exception) {
        return new ExceptionResponse(false, exception.getMessage());
    }

}
