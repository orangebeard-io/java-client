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
     * New attribute. Colons and Semicolons are removed because these are special values.
     */
    public Attribute(String key, String value) {
        this.key = key.replace(":", "").replace(";", "").trim();
        this.value = value.replace(":", "").replace(";", "").trim();
    }

    /**
     * New attribute. Colons and Semicolons are removed because these are special values.
     */
    public Attribute(String value) {
        this.value = value.replace(":", "").replace(";", "").trim();
    }

    @Override
    public String toString() {
        return format("key: %s; value:%s", key, value);
    }
}


