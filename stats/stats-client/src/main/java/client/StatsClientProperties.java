package client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "stats-client")
public class StatsClientProperties {
    private String serviceId = "stats-server";
    private String endpointPath = "";
}