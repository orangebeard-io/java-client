package io.orangebeard.client;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared Jackson configuration. Dates are written as epoch seconds (with nanos), which is the wire format
 * the client has always used; Jackson 3 would otherwise default to ISO-8601 strings.
 */
final class Json {
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private Json() {
    }

    static JsonMapper mapper() {
        return MAPPER;
    }
}
