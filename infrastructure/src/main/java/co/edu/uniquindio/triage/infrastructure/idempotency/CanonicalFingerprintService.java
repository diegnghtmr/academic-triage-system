package co.edu.uniquindio.triage.infrastructure.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

@Component
public class CanonicalFingerprintService {

    private final ObjectMapper objectMapper;

    public CanonicalFingerprintService(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "El objectMapper no puede ser null");
    }

    public String fingerprint(IdempotencyFingerprintSource source) {
        Objects.requireNonNull(source, "El source no puede ser null");

        var root = JsonNodeFactory.instance.objectNode();
        root.put("scope", source.scope());
        root.put("httpMethod", source.httpMethod().trim().toUpperCase());
        root.put("path", source.path().trim());
        root.put("contentType", source.contentType() == null ? "" : source.contentType().trim().toLowerCase());
        root.set("queryParameters", canonicalizeQueryParameters(source.queryParameters()));
        root.set("body", canonicalizeNode(toBodyNode(source)));

        return sha256Hex(writeCanonical(root));
    }

    private JsonNode toBodyNode(IdempotencyFingerprintSource source) {
        if (source.body() == null) {
            return JsonNodeFactory.instance.nullNode();
        }
        if (source.body() instanceof String textBody && isJsonContentType(source.contentType())) {
            try {
                return objectMapper.readTree(textBody);
            } catch (JsonProcessingException exception) {
                return JsonNodeFactory.instance.textNode(textBody);
            }
        }
        return objectMapper.valueToTree(source.body());
    }

    private ArrayNode canonicalizeQueryParameters(Map<String, java.util.List<String>> queryParameters) {
        var entries = JsonNodeFactory.instance.arrayNode();
        queryParameters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    var param = JsonNodeFactory.instance.objectNode();
                    param.put("name", entry.getKey());
                    var values = JsonNodeFactory.instance.arrayNode();
                    entry.getValue().stream().sorted(Comparator.naturalOrder()).forEach(values::add);
                    param.set("values", values);
                    entries.add(param);
                });
        return entries;
    }

    private JsonNode canonicalizeNode(JsonNode node) {
        if (node == null || node.isNull() || node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            var array = JsonNodeFactory.instance.arrayNode();
            node.forEach(child -> array.add(canonicalizeNode(child)));
            return array;
        }

        var object = JsonNodeFactory.instance.objectNode();
        var fields = new java.util.ArrayList<Map.Entry<String, JsonNode>>();
        for (Iterator<Map.Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext(); ) {
            fields.add(iterator.next());
        }
        fields.stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> object.set(entry.getKey(), canonicalizeNode(entry.getValue())));
        return object;
    }

    private String writeCanonical(ObjectNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar el fingerprint canónico", exception);
        }
    }

    private boolean isJsonContentType(String contentType) {
        return contentType != null && contentType.toLowerCase().contains("json");
    }

    private String sha256Hex(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
}
