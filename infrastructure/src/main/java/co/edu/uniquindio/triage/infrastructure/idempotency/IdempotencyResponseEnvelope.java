package co.edu.uniquindio.triage.infrastructure.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record IdempotencyResponseEnvelope(
        int statusCode,
        String contentType,
        Map<String, List<String>> headers,
        String body
) {
    public IdempotencyResponseEnvelope {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("El statusCode es inválido");
        }
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static IdempotencyResponseEnvelope fromResponseEntity(ResponseEntity<?> responseEntity, ObjectMapper objectMapper) {
        Objects.requireNonNull(responseEntity, "La responseEntity no puede ser null");
        Objects.requireNonNull(objectMapper, "El objectMapper no puede ser null");

        var normalizedHeaders = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        responseEntity.getHeaders().forEach((name, values) -> normalizedHeaders.put(name, List.copyOf(values)));

        return new IdempotencyResponseEnvelope(
                responseEntity.getStatusCode().value(),
                responseEntity.getHeaders().getContentType() == null ? null : responseEntity.getHeaders().getContentType().toString(),
                normalizedHeaders,
                serializeBody(responseEntity.getBody(), objectMapper)
        );
    }

    public ResponseEntity<String> toResponseEntity() {
        var httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::put);
        if (contentType != null && !httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            httpHeaders.add(HttpHeaders.CONTENT_TYPE, contentType);
        }
        return ResponseEntity.status(statusCode).headers(httpHeaders).body(body);
    }

    public ResponseEntity<byte[]> toRawResponseEntity() {
        var httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::put);
        if (contentType != null && !httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            httpHeaders.set(HttpHeaders.CONTENT_TYPE, contentType);
        }
        var bytes = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
        return ResponseEntity.status(statusCode).headers(httpHeaders).body(bytes);
    }

    private static String serializeBody(Object body, ObjectMapper objectMapper) {
        if (body == null) {
            return null;
        }
        if (body instanceof String textBody) {
            return textBody;
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("No fue posible serializar el body para idempotencia", exception);
        }
    }
}
