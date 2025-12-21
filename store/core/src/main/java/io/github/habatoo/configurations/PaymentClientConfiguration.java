package io.github.habatoo.configurations;

import io.github.habatoo.store.payment.api.PaymentsApi;
import io.github.habatoo.store.payment.invoker.ApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@AutoConfiguration
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
public class PaymentClientConfiguration {

    @Value("${payment.url}")
    private String paymentsServiceUrl;

    @Bean
    public PaymentsApi paymentsApi(ApiClient apiClient) {
        return new PaymentsApi(apiClient);
    }

    @Bean
    public ApiClient apiClient(WebClient.Builder webClientBuilder) {
        WebClient webClient = webClientBuilder
                .baseUrl(paymentsServiceUrl)
                .filter(tokenRelayFilter())
                .build();

        return new ApiClient(webClient);
    }

    private ExchangeFilterFunction tokenRelayFilter() {
        return (request, next) -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    String token = getToken(auth);

                    if (token != null) {
                        return Mono.just(ClientRequest.from(request)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .build());
                    }
                    return Mono.just(request);
                })
                .flatMap(next::exchange);
    }

    private String getToken(Authentication auth) {
        String token = null;

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            token = jwtAuth.getToken().getTokenValue();
        } else if (auth instanceof OAuth2AuthenticationToken oauth) {
            if (oauth.getPrincipal() instanceof OidcUser oidcUser) {
                token = oidcUser.getIdToken().getTokenValue();
            }
        }

        return token;
    }
}

