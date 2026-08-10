package io.github.kergosdyr.commercelab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CommerceLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommerceLabApplication.class, args);
    }
}
