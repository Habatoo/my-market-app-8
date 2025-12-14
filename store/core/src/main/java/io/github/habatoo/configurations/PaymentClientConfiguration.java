package io.github.habatoo.configurations;

import io.github.habatoo.store.payment.api.PaymentsApi;
import io.github.habatoo.store.payment.invoker.ApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@RequiredArgsConstructor
public class PaymentClientConfiguration {

    @Value("${payment.url}")
    private String paymentUrl;

    @Bean
    public ApiClient paymentApiClient() {
        return new ApiClient().setBasePath(paymentUrl);
    }

    @Bean
    public PaymentsApi paymentsApi(ApiClient paymentApiClient) {
        return new PaymentsApi(paymentApiClient);
    }
}

