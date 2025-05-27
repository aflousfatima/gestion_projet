package com.task.taskservice.Configuration;

import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfiguration {

    @Bean
    public Request.Options feignOptions() {
        // Timeout de connexion : 10 secondes
        // Timeout de lecture : 60 secondes
        return new Request.Options(
                10_000, // connectTimeoutMillis
                60_000  // readTimeoutMillis
        );
    }

    @Bean
    public Retryer feignRetryer() {
        // Réessaye jusqu'à 2 fois, avec un délai initial de 2s et un délai max de 4s
        return new Retryer.Default(
                2_000, // period (ms)
                4_000, // maxPeriod (ms)
                2      // maxAttempts
        );
    }
}