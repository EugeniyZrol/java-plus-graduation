package server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.EndpointHitDto;
import model.ViewStatsDto;
import org.springframework.stereotype.Service;
import server.repository.StatsRepository;
import server.entity.EndpointHitEntity;
import server.mapper.EndpointHitMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final StatsRepository statsRepository;
    private final EndpointHitMapper endpointHitMapper;

    public EndpointHitDto saveHit(EndpointHitDto endpointHitDto) {
        log.info("Сохранение hit: {}", endpointHitDto);
        EndpointHitEntity entity = endpointHitMapper.toEntity(endpointHitDto);
        EndpointHitEntity savedEntity = statsRepository.save(entity);
        return endpointHitMapper.toDto(savedEntity);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("Получение статистики: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        List<ViewStatsDto> result;
        if (Boolean.TRUE.equals(unique)) {
            result = statsRepository.findUniqueStats(start, end, uris);
        } else {
            result = statsRepository.findStats(start, end, uris);
        }

        log.info("Результат статистики: {}", result);
        return result;
    }
}