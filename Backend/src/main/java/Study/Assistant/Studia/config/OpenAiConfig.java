package Study.Assistant.Studia.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

public class OpenAiConfig {
    @Value("${openai.api-key}")
    public String apiKey;
}