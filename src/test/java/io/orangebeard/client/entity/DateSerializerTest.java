package io.orangebeard.client.entity;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DateSerializerTest {

    @Test
    public void test() throws IOException {
        DateSerializer dateSerializer = new DateSerializer(ZoneId.of("UTC"));

        JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        dateSerializer.serialize(LocalDateTime.parse("2021-04-07T14:30:53.314089"), jsonGenerator, null);

        verify(jsonGenerator).writeRawValue(eq("1617805853314"));
    }

}
