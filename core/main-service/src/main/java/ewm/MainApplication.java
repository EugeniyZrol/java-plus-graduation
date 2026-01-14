package ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@ComponentScan(basePackages = {
        "ewm",
        "client"
})
@EnableDiscoveryClient
@EnableRetry
public class MainApplication {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}