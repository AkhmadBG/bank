package ru.practicum.front.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import ru.practicum.front.exception.GatewayErrorDecoder;
import tools.jackson.databind.ObjectMapper;

public class GatewayFeignClientConfig {

    @Bean
    public RequestInterceptor userBearerTokenInterceptor(OAuth2AuthorizedClientManager authorizedClientManager) {
        return requestTemplate -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                        .withClientRegistrationId(oauthToken.getAuthorizedClientRegistrationId())
                        .principal(authentication)
                        .build();

                OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

                if (authorizedClient != null) {
                    requestTemplate.header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + authorizedClient.getAccessToken().getTokenValue()
                    );
                }
            }
        };
    }

    @Bean
    public ErrorDecoder gatewayErrorDecoder(ObjectMapper objectMapper) {
        return new GatewayErrorDecoder(objectMapper);
    }

}