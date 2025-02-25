package searchengine.dto.search;

import lombok.Data;

@Data
public class SearchDataItem implements Comparable<SearchDataItem> {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;

    @Override
    public int compareTo(SearchDataItem dataItem) {
        return Double.compare(this.relevance, dataItem.relevance);
    }
}
