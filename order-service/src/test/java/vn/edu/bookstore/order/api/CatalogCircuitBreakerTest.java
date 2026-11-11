package vn.edu.bookstore.order.api;

/**
 * TODO(TH3-6): viết kiểm thử cầu dao:
 *   - WireMock trả chậm 1500 ms cho /api/v1/books/.*, read-timeout = 300ms
 *   - circuit-breaker: sliding-window-size=4, minimum-number-of-calls=2, failure-rate-threshold=50
 *   - gửi 5 POST /api/v1/orders: mọi lần 503 upstream-unavailable, mỗi lần < 1400 ms
 *   - WireMock chỉ nhận đúng 2 yêu cầu (3 lần sau bị cầu dao chặn)
 */

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import vn.edu.bookstore.order.TestcontainersConfig;
import vn.edu.bookstore.order.support.CatalogStub;


/** TH3-6: catalog chậm hơn timeout đọc -> 503 nhanh; sau đủ số lỗi cầu dao MỞ và không gọi catalog nữa. */
@SpringBootTest(properties = {
    "bookstore.catalog.read-timeout=300ms",
    "bookstore.catalog.circuit-breaker.sliding-window-size=4",
    "bookstore.catalog.circuit-breaker.minimum-number-of-calls=2",
    "bookstore.catalog.circuit-breaker.failure-rate-threshold=50",
    "bookstore.catalog.circuit-breaker.wait-duration-in-open-state=60s"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class CatalogCircuitBreakerTest {

    static final WireMockServer CATALOG = CatalogStub.start();

    static {
        CATALOG.stubFor(WireMock.get(urlMatching("/api/v1/books/.*"))
            .willReturn(aResponse()
                .withFixedDelay(1500)
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{}")));
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("bookstore.catalog.base-url", CATALOG::baseUrl);
    }

    @AfterAll
    static void stop() {
        CATALOG.stop();
    }

    @Autowired
    MockMvc mvc;

    @Test
    void timeoutRoiCauDaoMo() throws Exception {
        String body = "{\"customerId\":88,\"lines\":[{\"isbn\":\"9780134685991\",\"quantity\":1}]}";
        for (int i = 0; i < 5; i++) {
            long t0 = System.nanoTime();
            mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "10"))
                .andExpect(jsonPath("$.type").value("https://api.bookstore.vn/problems/upstream-unavailable"));
            
            long ms = (System.nanoTime() - t0) / 1_000_000;
            assertThat(ms).as("không chờ hết 1,5 s của catalog chậm").isLessThan(1400);
        }
        
        // Chỉ 2 lời gọi đầu chạm tới WireMock; 3 lời gọi sau bị cầu dao chặn ngay tại client
        assertThat(CATALOG.findAll(getRequestedFor(urlMatching("/api/v1/books/.*")))).hasSize(2);
    }
}