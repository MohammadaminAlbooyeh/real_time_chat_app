package com.chatapp.search.dto;

import java.util.List;
import java.util.Map;

public class SearchResult {
    private long total;
    private List<Map<String, Object>> results;

    public SearchResult() {}

    public SearchResult(long total, List<Map<String, Object>> results) {
        this.total = total;
        this.results = results;
    }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public List<Map<String, Object>> getResults() { return results; }
    public void setResults(List<Map<String, Object>> results) { this.results = results; }
}
