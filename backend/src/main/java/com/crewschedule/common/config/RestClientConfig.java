package com.crewschedule.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** OAuth provider 등 외부 HTTP 호출용 RestClient 빈. */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient oauthRestClient() {
        return RestClient.builder().build();
    }
}
