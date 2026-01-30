package edu.gju.chatbot.config.etl;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(OcrScannerProperties.CONFIG_PREFIX)
public class OcrScannerProperties {

    public static final String CONFIG_PREFIX = "service.ocr-scanner";

    public static final String DEFAULT_BASE_URL = "http://localhost:8000";

    public static final String DEFAULT_SCANNER_PATH = "/scan";

    private String baseUrl = DEFAULT_BASE_URL;

    private String scannerPath = DEFAULT_SCANNER_PATH;
}
