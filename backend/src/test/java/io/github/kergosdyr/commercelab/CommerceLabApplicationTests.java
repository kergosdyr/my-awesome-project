package io.github.kergosdyr.commercelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kergosdyr.commercelab.domain.order.OrderService;
import io.github.kergosdyr.commercelab.domain.product.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommerceLabApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Value("${spring.jpa.open-in-view}")
    private boolean openInView;

    @Test
    void loadsTheCommerceContextWithOsivDisabled() {
        assertThat(applicationContext.getBean(ProductService.class)).isNotNull();
        assertThat(applicationContext.getBean(OrderService.class)).isNotNull();
        assertThat(openInView).isFalse();
    }

    @Test
    void exposesOnlyTheIntendedOperationalEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string(containsString("http_server_requests_seconds_count")));

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isNotFound());
    }
}
