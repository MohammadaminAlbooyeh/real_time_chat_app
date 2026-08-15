package com.chatapp.worker.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class SearchIndexServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SearchIndexService searchIndexService;

    @BeforeEach
    void setUp() {
        searchIndexService = new SearchIndexService(restTemplate, "http://search-service:8002");
    }

    @Test
    void indexMessageShouldPostToSearchService() {
        searchIndexService.indexMessage("msg-1", "hello world", "room-1", "user-1");

        verify(restTemplate).postForEntity(
            eq("http://search-service:8002/api/v1/search/index"),
            eq(Map.of("id", "msg-1", "content", "hello world", "room_id", "room-1", "sender_id", "user-1")),
            eq(Void.class)
        );
    }

    @Test
    void deleteMessageShouldDeleteFromSearchService() {
        searchIndexService.deleteMessage("msg-1");

        verify(restTemplate).delete(
            eq("http://search-service:8002/api/v1/search/messages/{messageId}"),
            eq("msg-1")
        );
    }
}
