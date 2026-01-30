package edu.gju.chatbot.config.etl;

import edu.gju.chatbot.etl.OcrScanner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({ OcrScannerProperties.class })
public class OcrScannerConfig {

    @Bean
    public OcrScanner ocrScanner(
        OcrScannerProperties properties,
        RestClient.Builder restClientBuilder,
        RetryTemplate retryTemplate
    ) {
        RestClient restClient = restClientBuilder
            .baseUrl(properties.getBaseUrl() + properties.getScannerPath())
            .build();

        return new OcrScanner(restClient, retryTemplate);
    }
}
