package searchengine.dto.search;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result = true;
    private int count;
    private List<SearchDataItem> date;
}
