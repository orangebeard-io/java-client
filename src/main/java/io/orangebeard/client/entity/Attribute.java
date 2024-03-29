package io.orangebeard.client.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static java.lang.String.format;

@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class Attribute {
    private String key;
    private String value;

    /**
     * New attribute.
     */
    public Attribute(String key, String value) {
        this.key = key.trim();
        this.value = value.trim();
    }


    /**
     * New attribute.
     */
    public Attribute(String value) {
        this.value = value.trim();
    }

    @Override
    public String toString() {
        return format("key: %s; value:%s", key, value);
    }
}


