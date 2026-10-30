package cntt.giahuy.fashion_shop.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {
        "cntt.giahuy.fashion_shop.user.repository",
        "cntt.giahuy.fashion_shop.catalog.repository",
        "cntt.giahuy.fashion_shop.inventory.repository",
        "cntt.giahuy.fashion_shop.voucher.repository",
        "cntt.giahuy.fashion_shop.order.repository",
        "cntt.giahuy.fashion_shop.payment.repository",
        "cntt.giahuy.fashion_shop.search.repository"
})
public class JpaConfig {
}
