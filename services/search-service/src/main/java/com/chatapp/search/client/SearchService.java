package com.chatapp.search.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.chatapp.search.config.SearchProperties;
import com.chatapp.search.dto.SearchResult;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import jakarta.annotation.PostConstruct;

@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final ElasticsearchClient client;
    private final SearchProperties props;

    public SearchService(ElasticsearchClient client, SearchProperties props) {
        this.client = client;
        this.props = props;
    }

    @PostConstruct
    public void ensureIndex() {
        try {
            String index = props.getIndexName();
            BooleanResponse exists = client.indices().exists(e -> e.index(index));
            if (!exists.value()) {
                Map<String, Property> properties = new HashMap<>();
                properties.put("id", Property.of(p -> p.keyword(k -> k)));
                properties.put("content", Property.of(p -> p.text(t -> t.analyzer("message_analyzer"))));
                properties.put("room_id", Property.of(p -> p.keyword(k -> k)));
                properties.put("sender_id", Property.of(p -> p.keyword(k -> k)));
                properties.put("sender_username", Property.of(p -> p.keyword(k -> k)));
                properties.put("message_type", Property.of(p -> p.keyword(k -> k)));
                properties.put("created_at", Property.of(p -> p.date(d -> d)));

                Map<String, co.elastic.clients.elasticsearch._types.analysis.Analyzer> analyzers = new HashMap<>();
                analyzers.put("message_analyzer", co.elastic.clients.elasticsearch._types.analysis.Analyzer.of(a -> a
                    .custom(c -> c
                        .tokenizer("standard")
                        .filter("lowercase", "stop")
                    )
                ));

                IndexSettings settings = IndexSettings.of(s -> s
                    .numberOfShards("1")
                    .numberOfReplicas("0")
                    .analysis(an -> an.analyzer(analyzers))
                );

                TypeMapping mapping = TypeMapping.of(m -> m.properties(properties));

                CreateIndexResponse response = client.indices().create(c -> c
                    .index(index)
                    .settings(settings)
                    .mappings(mapping)
                );
                log.info("Created index: {} acknowledged={}", index, response.acknowledged());
            } else {
                log.info("Index {} already exists", index);
            }
        } catch (IOException e) {
            log.error("Failed to ensure elasticsearch index", e);
        }
    }

    public void indexMessage(String id, String content, String roomId, String senderId,
                              String senderUsername, String messageType, String createdAt) {
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("id", id);
            doc.put("content", content);
            doc.put("room_id", roomId);
            doc.put("sender_id", senderId);
            doc.put("sender_username", senderUsername != null ? senderUsername : "");
            doc.put("message_type", messageType != null ? messageType : "text");
            doc.put("created_at", createdAt);

            IndexResponse response = client.index(i -> i
                .index(props.getIndexName())
                .id(id)
                .document(doc)
                .refresh(co.elastic.clients.elasticsearch._types.Refresh.WaitFor)
            );
            log.info("Indexed message {} result={}", id, response.result());
        } catch (IOException e) {
            log.error("Failed to index message {}", id, e);
            throw new RuntimeException("Indexing failed: " + e.getMessage(), e);
        }
    }

    public SearchResult searchMessages(String query, String roomId, int limit, int offset) {
        try {
            SearchResponse<Map> response = client.search(s -> {
                var boolQ = s.index(props.getIndexName())
                    .query(q -> q.bool(b -> {
                        b.must(m -> m.match(t -> t.field("content").query(query).operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)));
                        if (roomId != null && !roomId.isEmpty()) {
                            b.filter(f -> f.term(t -> t.field("room_id").value(roomId)));
                        }
                        return b;
                    }))
                    .sort(so -> so.field(f -> f.field("created_at").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
                    .from(offset)
                    .size(limit);
                return boolQ;
            }, Map.class);

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            List<Map<String, Object>> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                results.add(hit.source());
            }
            return new SearchResult(total, results);
        } catch (IOException e) {
            log.error("Search failed for query: {}", query, e);
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    public void deleteMessage(String messageId) {
        try {
            DeleteResponse response = client.delete(d -> d
                .index(props.getIndexName())
                .id(messageId)
            );
            log.info("Deleted message {} from index result={}", messageId, response.result());
        } catch (IOException e) {
            log.error("Failed to delete message {} from index", messageId, e);
            throw new RuntimeException("Deletion failed: " + e.getMessage(), e);
        }
    }
}
