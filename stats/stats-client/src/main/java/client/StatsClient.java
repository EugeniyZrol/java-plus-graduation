package client;

import model.EndpointHitDto;
import model.ViewStatsDto;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
public class StatsClient {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final StatsClientProperties properties;

    public StatsClient(DiscoveryClient discoveryClient,
                       RestClient.Builder restClientBuilder,
                       StatsClientProperties properties) {
        this.discoveryClient = discoveryClient;
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Retryable(
            value = {StatsServerUnavailableException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    private ServiceInstance getStatsServiceInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(properties.getServiceId());
        if (instances == null || instances.isEmpty()) {
            throw new StatsServerUnavailableException(
                    "Сервис статистики не найден в службе обнаружения. Service ID: " + properties.getServiceId()
            );
        }
        return instances.getFirst();
    }

    private URI buildStatsUri(String path) {
        ServiceInstance instance = getStatsServiceInstance();

        String fullPath = properties.getEndpointPath() +
                (path.startsWith("/") ? path : "/" + path);

        return UriComponentsBuilder.newInstance()
                .scheme("http")
                .host(instance.getHost())
                .port(instance.getPort())
                .path(fullPath)
                .build()
                .toUri();
    }

    public void hit(EndpointHitDto endpointHitDto) {
        try {
            URI hitUri = buildStatsUri("/hit");

            log.debug("Отправка hit статистики: {}, URI: {}", endpointHitDto, hitUri);

            restClient.post()
                    .uri(hitUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(endpointHitDto)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Hit статистики успешно отправлен");

        } catch (StatsServerUnavailableException e) {
            log.warn("Сервис статистики недоступен: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики", e);
        }
    }

    public void hit(String path, String ip) {
        EndpointHitDto endpointHitDto = new EndpointHitDto(
                properties.getAppName(),
                path,
                ip,
                LocalDateTime.now()
        );
        hit(endpointHitDto);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        try {
            ServiceInstance instance = getStatsServiceInstance();

            String basePath = properties.getEndpointPath() + "/stats";

            UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                    .scheme("http")
                    .host(instance.getHost())
                    .port(instance.getPort())
                    .path(basePath);

            builder.queryParam("start", start.format(FORMATTER))
                    .queryParam("end", end.format(FORMATTER))
                    .queryParam("unique", unique != null ? unique : false);

            if (uris != null && !uris.isEmpty()) {
                for (String uri : uris) {
                    builder.queryParam("uris", uri);
                }
            }

            URI url = builder.build().toUri();

            log.debug("Запрос статистики: start={}, end={}, uris={}, unique={}, url={}",
                    start, end, uris, unique, url);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {}
            );

            List<ViewStatsDto> result = response.getBody();
            log.debug("Получено {} записей статистики", result != null ? result.size() : 0);

            return result != null ? result : List.of();

        } catch (StatsServerUnavailableException e) {
            log.warn("Сервис статистики недоступен: {}", e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Ошибка при получении статистики", e);
            return List.of();
        }
    }
}