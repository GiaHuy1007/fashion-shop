package cntt.giahuy.fashion_shop;

import com.redis.testcontainers.RedisContainer;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/**
 * Base class for integration tests using reusable Singleton Testcontainers
 * (PostgreSQL, MongoDB, Redis, Elasticsearch, MinIO).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    private static final String PUBLIC_BUCKET = "fashion-public";
    private static final String PRIVATE_BUCKET = "fashion-private";

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer(
            DockerImageName.parse("postgres:18.6-alpine"))
            .withDatabaseName("fashion_shop")
            .withUsername("fashion")
            .withPassword("fashion_secret")
            .withReuse(true);

    @ServiceConnection
    protected static final MongoDBContainer MONGO_CONTAINER = new MongoDBContainer(
            DockerImageName.parse("mongo:9.0.4"))
            .withReuse(true);

    @ServiceConnection
    protected static final RedisContainer REDIS_CONTAINER = new RedisContainer(
            DockerImageName.parse("redis:8.8.3-alpine"))
            .withReuse(true);

    protected static final ElasticsearchContainer ELASTICSEARCH_CONTAINER = new ElasticsearchContainer(
            DockerImageName.parse("elasticsearch:9.5.5"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("xpack.security.http.ssl.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withReuse(true);

    protected static final MinIOContainer MINIO_CONTAINER = new MinIOContainer(
            DockerImageName.parse("cgr.dev/chainguard/minio:latest")
                    .asCompatibleSubstituteFor("minio/minio"))
            .withUserName("minioadmin")
            .withPassword("minioadmin123")
            .withReuse(true);

    static {
        Startables.deepStart(
                POSTGRES_CONTAINER,
                MONGO_CONTAINER,
                REDIS_CONTAINER,
                ELASTICSEARCH_CONTAINER,
                MINIO_CONTAINER
        ).join();

        initMinioBuckets();
    }

    private static void initMinioBuckets() {
        try (MinioClient minioClient = MinioClient.builder()
                .endpoint(MINIO_CONTAINER.getS3URL())
                .credentials(MINIO_CONTAINER.getUserName(), MINIO_CONTAINER.getPassword())
                .build()) {
            for (String bucket : List.of(PUBLIC_BUCKET, PRIVATE_BUCKET)) {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucket).build()
                );
                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(bucket).build()
                    );
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize MinIO buckets for integration tests", e);
        }
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        // MongoDB
        registry.add("spring.mongodb.uri", () -> MONGO_CONTAINER.getReplicaSetUrl("fashion_shop"));
        registry.add("spring.data.mongodb.uri", () -> MONGO_CONTAINER.getReplicaSetUrl("fashion_shop"));

        // Redis
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));

        // Elasticsearch
        registry.add("spring.elasticsearch.uris", () -> "http://" + ELASTICSEARCH_CONTAINER.getHttpHostAddress());

        // MinIO
        registry.add("app.minio.endpoint", MINIO_CONTAINER::getS3URL);
        registry.add("app.minio.access-key", MINIO_CONTAINER::getUserName);
        registry.add("app.minio.secret-key", MINIO_CONTAINER::getPassword);
        registry.add("app.minio.cdn-base-url", () -> MINIO_CONTAINER.getS3URL() + "/" + PUBLIC_BUCKET);
    }
}
