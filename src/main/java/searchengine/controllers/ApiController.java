package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.SiteListIndexingServiceImpl;
import searchengine.services.StatisticsService;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final SiteListIndexingServiceImpl indexingService;
    private final StatisticsService statisticsService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

     @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexingResponse());
     }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexingResponse());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestBody String urlPage) {
        return ResponseEntity.ok(indexingService.indexingPageResponse(urlPage));
    }

    @GetMapping("/checkCollection")
    public ResponseEntity<IndexingResponse> checkCollection() {
        return ResponseEntity.ok(indexingService.checkCollectionResponse());
    }

}
