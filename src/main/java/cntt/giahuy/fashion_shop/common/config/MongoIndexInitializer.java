package cntt.giahuy.fashion_shop.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexInitializer {

    private static final Duration RETENTION_90_DAYS = Duration.ofDays(90);

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initMongoCollectionsAndIndexes() {
        log.info("Initializing MongoDB collections and indexes...");
        initUserActivitiesTimeSeries();
        initConversationIndexes();
        initChatMessageIndexes();
        initProductReviewIndexes();
        initNotificationIndexes();
        log.info("MongoDB collections and indexes initialized successfully.");
    }

    private void initUserActivitiesTimeSeries() {
        String collectionName = "user_activities";
        if (!mongoTemplate.collectionExists(collectionName)) {
            CollectionOptions options = CollectionOptions.timeSeries(
                    "ts",
                    ts -> ts.metaField("meta").expireAfter(RETENTION_90_DAYS)
            );
            mongoTemplate.createCollection(collectionName, options);
            log.info("Created MongoDB time-series collection: {}", collectionName);
        }
    }

    private void initConversationIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("conversations");

        indexOps.createIndex(new Index()
                .on("participants.userId", Sort.Direction.ASC)
                .on("updatedAt", Sort.Direction.DESC)
                .named("idx_conversations_participant_updated"));

        indexOps.createIndex(new Index()
                .on("assignedAgentId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("updatedAt", Sort.Direction.DESC)
                .named("idx_conversations_agent_status_updated"));

        indexOps.createIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.ASC)
                .named("idx_conversations_status_created"));

        indexOps.createIndex(new Index()
                .on("customerId", Sort.Direction.ASC)
                .unique()
                .partial(PartialIndexFilter.of(Criteria.where("status").is("OPEN")))
                .named("uq_conversations_open_per_customer"));
    }

    private void initChatMessageIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("chat_messages");

        indexOps.createIndex(new Index()
                .on("conversationId", Sort.Direction.ASC)
                .on("_id", Sort.Direction.DESC)
                .named("idx_chat_messages_conv_id"));

        indexOps.createIndex(new Index()
                .on("conversationId", Sort.Direction.ASC)
                .on("senderId", Sort.Direction.ASC)
                .on("clientMessageId", Sort.Direction.ASC)
                .unique()
                .named("uq_chat_messages_client_msg"));

        indexOps.createIndex(new Index()
                .on("conversationId", Sort.Direction.ASC)
                .on("senderId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_chat_messages_mark_seen"));
    }

    private void initProductReviewIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("product_reviews");

        indexOps.createIndex(new Index()
                .on("productId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_reviews_product_status_created"));

        indexOps.createIndex(new Index()
                .on("orderItemId", Sort.Direction.ASC)
                .unique()
                .named("uq_reviews_order_item"));

        indexOps.createIndex(new Index()
                .on("productId", Sort.Direction.ASC)
                .on("rating", Sort.Direction.ASC)
                .named("idx_reviews_product_rating"));
    }

    private void initNotificationIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("notifications");

        indexOps.createIndex(new Index()
                .on("userId", Sort.Direction.ASC)
                .on("read", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_notifications_user_read_created"));

        indexOps.createIndex(new Index()
                .on("createdAt", Sort.Direction.ASC)
                .expire(RETENTION_90_DAYS)
                .named("idx_notifications_ttl"));
    }
}
