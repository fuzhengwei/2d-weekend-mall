package cn.xiaofuge.mall.assistant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class MallApiClient {
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int REQUEST_TIMEOUT_SECONDS = 15;

    private final HttpClient httpClient;
    private volatile String baseUrl;
    private volatile String serviceToken;

    MallApiClient(String baseUrl) {
        this.baseUrl = normalize(baseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .build();
    }

    void setBaseUrl(String value) {
        this.baseUrl = normalize(value);
    }

    void setServiceToken(String value) {
        this.serviceToken = value == null || value.isBlank() ? null : value.trim();
    }

    String get(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .header("Accept", "application/json")
                .header("X-Service-Token", serviceToken == null ? "" : serviceToken)
                .GET()
                .build();
        return execute(request);
    }

    private String execute(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = response.body();
                throw new IllegalStateException("HTTP " + response.statusCode() + ": " + body);
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Mall API request interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Mall API unavailable: " + exception.getMessage(), exception);
        }
    }

    private String normalize(String value) {
        String result = value == null || value.isBlank() ? "http://127.0.0.1:18080" : value.trim();
        return result.endsWith("/") ? result.substring(0, result.length() - 1) : result;
    }
}
