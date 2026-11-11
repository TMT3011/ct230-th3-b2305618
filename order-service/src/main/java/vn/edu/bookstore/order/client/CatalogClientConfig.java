package vn.edu.bookstore.order.client;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CatalogClientConfig {

    @Bean
    RestClient catalogRestClient(RestClient.Builder builder, CatalogClientProperties props,
                                 ObjectProvider<DeferringLoadBalancerInterceptor> loadBalancer) {
        // TODO(TH3-2): tạo JdkClientHttpRequestFactory với connectTimeout = props.connectTimeout()
        //   và setReadTimeout(props.readTimeout()); gắn vào builder.requestFactory(...)
        // TODO(TH3-2): nếu props.loadBalanced() thì builder.requestInterceptor(loadBalancer.getIfAvailable())
        //   để "http://catalog-service" được tra qua Eureka
        return builder.baseUrl(props.baseUrl()).build();
    }

    @Bean
    Customizer<Resilience4JCircuitBreakerFactory> catalogCircuitBreaker(CatalogClientProperties props) {
        // TODO(TH3-3): factory.configure(b -> b.circuitBreakerConfig(CircuitBreakerConfig.custom()
        //   .slidingWindowSize(..).minimumNumberOfCalls(..).failureRateThreshold(..)
        //   .waitDurationInOpenState(..).ignoreExceptions(UnknownBookException.class).build()), "catalog");
        //   và ghi log mỗi lần cầu dao chuyển trạng thái (addCircuitBreakerCustomizer + onStateTransition)
        return factory -> { };
    }
}
