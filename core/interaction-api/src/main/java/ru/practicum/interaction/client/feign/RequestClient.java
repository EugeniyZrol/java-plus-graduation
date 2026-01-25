package ru.practicum.interaction.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {

    @PostMapping("/requests/feign/events/confirmed/count/batch")
    Map<Long, Integer> getConfirmedRequestsCountBatch(@RequestBody List<Long> eventIds);

}