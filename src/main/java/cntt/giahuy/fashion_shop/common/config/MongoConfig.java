package cntt.giahuy.fashion_shop.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = {
        "cntt.giahuy.fashion_shop.review.repository",
        "cntt.giahuy.fashion_shop.chat.repository",
        "cntt.giahuy.fashion_shop.notification.repository",
        "cntt.giahuy.fashion_shop.activitylog.repository"
})
public class MongoConfig {
}
