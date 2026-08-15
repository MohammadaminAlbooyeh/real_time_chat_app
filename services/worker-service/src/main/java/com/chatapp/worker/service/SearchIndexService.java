package com.chatapp.worker.service;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SearchIndexService {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexService.class);

    private final RestTemplate restTemplate;
    private final String searchServiceUrl;

    public SearchIndexService(RestTemplate restTemplate,
                              @Value("${app.search-service.url}") String searchServiceUrl) {
        this.restTemplate = restTemplate;
        this.searchServiceUrl = searchServiceUrl;
    }

    public void indexMessage(String messageId, String content, String roomId, String senderId) {
        logger.info("Indexing message {} in search service", messageId);
        try {
            restTemplate.postForEntity(
                searchServiceUrl + "/api/v1/search/index",
                Map.of("id", messageId, "content", content, "room_id", roomId, "sender_id", senderId),
                Void.class
            );
            logger.info("Message {} indexed successfully", messageId);
        } catch (Exception e) {
            logger.error("Failed to index message {}: {}", messageId, e.getMessage());
        }
    }

    public void deleteMessage(String messageId) {
        logger.info("Deleting message {} from search index", messageId);
        try {
            restTemplate.delete(searchServiceUrl + "/api/v1/search/messages/{messageId}", messageId);
            logger.info("Message {} deleted from search index", messageId);
        } catch (Exception e) {
            logger.error("Failed to delete message {} from index: {}", messageId, e.getMessage());
        }
    }
}
