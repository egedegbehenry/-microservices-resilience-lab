
package com.henry.resilience.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AvailabilityClientConfig {

    //@Bean registers our named REST Client with Spring
    //RestClient.Builder creates the HTTP client
    //@Value reads the URL from application.yaml

    @Bean("availabilityRestClient")
    public RestClient availabilityRestClient(
            RestClient.Builder builder,
            @Value("${services.availability.base-url}") String baseUrl) {

        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
