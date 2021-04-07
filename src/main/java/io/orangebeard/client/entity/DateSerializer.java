package io.orangebeard.client.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

class DateSerializer extends JsonSerializer<LocalDateTime> {

    private final ZoneId zoneId;

    public DateSerializer() {
        this.zoneId = ZoneId.systemDefault();
    }

    public DateSerializer(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
        jgen.writeRawValue(String.valueOf(value.atZone(zoneId).toInstant().toEpochMilli()));
    }
}
