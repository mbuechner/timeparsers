package de.ddb.labs.timeparsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TimeparsersApplication {

    private OkHttpClient httpClient; // http client

    private ObjectMapper objectMapper; // JSON

    public static void main(String[] args) {
        SpringApplication.run(TimeparsersApplication.class, args);
    }

    @Bean
    protected ObjectMapper objectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
        return objectMapper;
    }

    @Bean
    protected OkHttpClient httpClient() {
        if (httpClient == null) {
            final Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequests(64);
            dispatcher.setMaxRequestsPerHost(8);
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(600, TimeUnit.SECONDS)
                    .callTimeout(0, TimeUnit.SECONDS)
                    .dispatcher(dispatcher)
                    .build();
        }
        return httpClient;
    }
}
