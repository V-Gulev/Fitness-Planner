package bg.fitnessplanner.planning.application;

import bg.fitnessplanner.profile.application.ProfileSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Component
public class SnapshotCodec {
    private final ObjectMapper mapper;
    public SnapshotCodec(ObjectMapper mapper) { this.mapper = mapper; }

    public String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException failure) { throw new IllegalStateException("Could not encode plan evidence.", failure); }
    }

    /** Inputs contain unordered sets/catalogs. Do not use this to reorder a prescribed workout. */
    public String canonicalJson(Object value) { return json(canonical(mapper.valueToTree(value))); }

    public String hash(Object value) { return digest(canonicalJson(value)); }

    public String profileHash(ProfileSnapshot profile) {
        ObjectNode content = mapper.valueToTree(profile);
        content.remove("version"); // Activation changes the aggregate version, not training preferences.
        return digest(json(canonical(content)));
    }

    public <T> T read(String json, Class<T> type) {
        try { return mapper.readValue(json, type); }
        catch (JsonProcessingException failure) { throw new IllegalStateException("Stored plan evidence is invalid.", failure); }
    }

    public <T> T read(String json, TypeReference<T> type) {
        try { return mapper.readValue(json, type); }
        catch (JsonProcessingException failure) { throw new IllegalStateException("Stored plan evidence is invalid.", failure); }
    }

    private JsonNode canonical(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = mapper.createObjectNode();
            List<String> keys = new ArrayList<>();
            node.fieldNames().forEachRemaining(keys::add);
            Collections.sort(keys);
            keys.forEach(key -> result.set(key, canonical(node.get(key))));
            return result;
        }
        if (node.isArray()) {
            List<JsonNode> elements = new ArrayList<>();
            node.forEach(element -> elements.add(canonical(element)));
            elements.sort(Comparator.comparing(JsonNode::toString));
            ArrayNode result = mapper.createArrayNode();
            elements.forEach(result::add);
            return result;
        }
        return node;
    }

    private String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 is unavailable.", impossible); }
    }
}
