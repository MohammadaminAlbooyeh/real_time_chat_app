package com.chatapp.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "elasticsearch")
public class SearchProperties {
    private String uris = "http://localhost:9200";
    private String indexName = "messages";

    public String getUris() { return uris; }
    public void setUris(String uris) { this.uris = uris; }
    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
}
