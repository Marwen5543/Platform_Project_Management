package company.Config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
@Configuration
public class ErrorHandlingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandlingConfiguration.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    @Order(-2)
    public ErrorWebExceptionHandler customErrorWebExceptionHandler() {
        return new CustomErrorWebExceptionHandler();
    }

    private class CustomErrorWebExceptionHandler implements ErrorWebExceptionHandler {
        @Override
        public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
            log.error("Error processing request: ", ex);

            Map<String, Object> errorResponse = new HashMap<>();
            HttpStatus status;

            if (ex instanceof ResponseStatusException) {
                ResponseStatusException responseError = (ResponseStatusException) ex;
                status = (HttpStatus) responseError.getStatusCode();
                errorResponse.put("message", responseError.getReason());
            } else {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                errorResponse.put("message", "Internal Server Error");
            }

            errorResponse.put("status", status.value());
            errorResponse.put("error", status.getReasonPhrase());
            errorResponse.put("path", exchange.getRequest().getPath().value());
            errorResponse.put("timestamp", System.currentTimeMillis());

            byte[] errorResponseBytes;
            try {
                errorResponseBytes = objectMapper.writeValueAsBytes(errorResponse);
            } catch (JsonProcessingException e) {
                log.error("Error converting error response to JSON", e);
                errorResponseBytes = "Internal Server Error".getBytes();
            }

            exchange.getResponse().setStatusCode(status);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(errorResponseBytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }
}