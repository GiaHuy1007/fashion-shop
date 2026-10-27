package cntt.giahuy.fashion_shop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FashionShopApplicationTests extends AbstractIntegrationTest {

    @Test
    @DisplayName("Should load Spring context and start all reusable Testcontainers")
    void contextLoads() {
        assertThat(POSTGRES_CONTAINER.isRunning()).isTrue();
        assertThat(MONGO_CONTAINER.isRunning()).isTrue();
        assertThat(REDIS_CONTAINER.isRunning()).isTrue();
        assertThat(ELASTICSEARCH_CONTAINER.isRunning()).isTrue();
        assertThat(MINIO_CONTAINER.isRunning()).isTrue();
    }

}
