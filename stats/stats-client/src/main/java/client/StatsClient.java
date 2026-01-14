package client;

import model.EndpointHitDto;
import model.ViewStatsDto;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class StatsClient {
    private static final String STATS_SERVICE_ID = "stats-server";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;

    public StatsClient(DiscoveryClient discoveryClient, RestClient.Builder restClientBuilder) {
        this.discoveryClient = discoveryClient;
        this.restClient = restClientBuilder.build();
    }

    @Retryable(
            value = {StatsServerUnavailableException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    private ServiceInstance getStatsServiceInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVICE_ID);
        if (instances == null || instances.isEmpty()) {
            throw new StatsServerUnavailableException(
                    "Сервис статистики не найден в службе обнаружения. Service ID: " + STATS_SERVICE_ID
            );
        }
        return instances.getFirst();
    }

    private URI buildStatsUri(String path) {
        ServiceInstance instance = getStatsServiceInstance();

        return UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(instance.getHost())
                .port(instance.getPort())
                .path(path.startsWith("/") ? path : "/" + path)
                .build()
                .toUri();
    }

    public void hit(EndpointHitDto endpointHitDto) {
        try {
            URI hitUri = buildStatsUri("/hit");

            restClient.post()
                    .uri(hitUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(endpointHitDto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (StatsServerUnavailableException e) {
            System.err.println("Сервис статистики недоступен: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка при отправке статистики: " + e.getMessage());
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        try {
            ServiceInstance instance = getStatsServiceInstance();

            UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                    .scheme("http")
                    .host(instance.getHost())
                    .port(instance.getPort())
                    .path("/stats");

            builder.queryParam("start", start.format(FORMATTER))
                    .queryParam("end", end.format(FORMATTER))
                    .queryParam("unique", unique != null ? unique : false);

            if (uris != null && !uris.isEmpty()) {
                for (String uri : uris) {
                    builder.queryParam("uris", uri);
                }
            }

            URI url = builder.build().toUri();

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {}
            );

            return response.getBody();
        } catch (StatsServerUnavailableException e) {
            System.err.println("Сервис статистики недоступен: " + e.getMessage());
            return List.of();
        } catch (Exception e) {
            System.err.println("Ошибка при получении статистики: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
}