package com.quickbooks.sales.client;

import com.quickbooks.sales.dto.CategoryPredictionResponse;
import com.quickbooks.sales.dto.ForecastRequest;
import com.quickbooks.sales.exception.MlServiceBadRequestException;
import com.quickbooks.sales.exception.MlServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Component
public class MlServiceClient {

    private final WebClient webClient;
    private final String serviceToken;

    public MlServiceClient(
            @Value("${ml.service.url}") String mlServiceUrl,
            @Value("${ml.service.token}") String serviceToken) {

        this.webClient = WebClient.builder()
                .baseUrl(mlServiceUrl)
                .build();

        this.serviceToken = serviceToken;
    }

    public CategoryPredictionResponse forecast(
            ForecastRequest request) {

        try {

            return webClient.post()
                    .uri("/forecast")
                    .header("X-ML-Service-Token", serviceToken)
                    .bodyValue(request)
                    .retrieve()

                    // 4xx = bad request / authorization / client error
                    // Do NOT use Moving Average fallback.
                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            response -> response.createException()
                                    .map(ex ->
                                            new MlServiceBadRequestException(
                                                    "ML service rejected the request. "
                                                            + "HTTP status: "
                                                            + response.statusCode(),
                                                    ex
                                            )
                                    )
                    )

                    // 5xx = ML service/server failure
                    // PredictionService will use Moving Average fallback.
                    .onStatus(
                            status -> status.is5xxServerError(),
                            response -> response.createException()
                                    .map(ex ->
                                            new MlServiceUnavailableException(
                                                    "ML service returned server error. "
                                                            + "HTTP status: "
                                                            + response.statusCode(),
                                                    ex
                                            )
                                    )
                    )

                    .bodyToMono(CategoryPredictionResponse.class)
                    .block();

        } catch (WebClientRequestException ex) {

            // Connection refused, timeout, network failure, etc.
            throw new MlServiceUnavailableException(
                    "ML service is unavailable",
                    ex
            );
        }
    }
}