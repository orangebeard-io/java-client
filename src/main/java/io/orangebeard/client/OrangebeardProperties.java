package io.orangebeard.client;

import io.orangebeard.client.entity.Attribute;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.orangebeard.client.OrangebeardProperty.ACCESS_TOKEN;
import static io.orangebeard.client.OrangebeardProperty.DESCRIPTION;
import static io.orangebeard.client.OrangebeardProperty.ENDPOINT;
import static io.orangebeard.client.OrangebeardProperty.PROJECT;
import static io.orangebeard.client.OrangebeardProperty.TESTSET;

@ToString
@Getter
public class OrangebeardProperties {
    private static final String ORANGEBEARD_PROPERTY_FILE = "orangebeard.properties";

    private static final Logger LOGGER = LoggerFactory.getLogger(OrangebeardProperties.class);
    private String endpoint;
    private UUID accessToken;
    private String projectName;
    private String testSetName;
    private String description;
    private Set<Attribute> attributes = new HashSet<>();
    private boolean propertyFilePresent;

    private enum PropertyNameStyle {
        DOT, UNDERSCORE
    }

    OrangebeardProperties(String propertyFile) {
        readPropertyFile(propertyFile);
        readSystemProperties();
        readEnvironmentVariables(PropertyNameStyle.DOT);
        readEnvironmentVariables(PropertyNameStyle.UNDERSCORE);
    }

    public OrangebeardProperties() {
        this(ORANGEBEARD_PROPERTY_FILE);
    }

    public boolean requiredValuesArePresent() {
        return endpoint != null && accessToken != null && projectName != null && testSetName != null;
    }

    public void checkPropertiesArePresent() {
        if (!requiredValuesArePresent() && !propertyFilePresent) {
            LOGGER.error("Required Orangebeard properties are missing. Not all environment variables are present, and orangebeard.properties cannot be found!\n" + requiredPropertiesToString());
        }
        if (!requiredValuesArePresent()) {
            LOGGER.error("Required Orangebeard properties are missing. Not all environment variables are present, and/or orangebeard.properties misses required values!\n" + requiredPropertiesToString());
        }
    }

    private String requiredPropertiesToString() {
        return ENDPOINT.getPropertyName() + ": " + endpoint + "\n" +
                ACCESS_TOKEN.getPropertyName() + ": " + (accessToken != null ? "HIDDEN (PRESENT)" : "null\n") +
                PROJECT.getPropertyName() + ": " + projectName + "\n" +
                TESTSET.getPropertyName() + ": " + testSetName + "\n";
    }

    private void readPropertyFile(String name) {
        try {
            Properties properties = new Properties();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(name);

            if (inputStream != null) {
                properties.load(inputStream);
                this.propertyFilePresent = true;
            }
            readPropertiesWith(properties::getProperty);
        } catch (IOException e) {
            this.propertyFilePresent = false;
        }
    }

    private void readSystemProperties() {
        readPropertiesWith(System::getProperty);
    }

    private void readEnvironmentVariables(PropertyNameStyle style) {
        if (style == PropertyNameStyle.UNDERSCORE) {
            readPropertiesWith(n -> System.getenv(n.replace(".", "_")));
        } else {
            readPropertiesWith(System::getenv);
        }
    }

    private void readPropertiesWith(Function<String, String> lookupFunc) {
        this.endpoint = lookupWithDefault(ENDPOINT, lookupFunc, this.endpoint);
        this.accessToken = lookupUUIDWithDefault(ACCESS_TOKEN, lookupFunc, this.accessToken);
        this.projectName = lookupWithDefault(PROJECT, lookupFunc, this.projectName);
        this.testSetName = lookupWithDefault(TESTSET, lookupFunc, this.testSetName);
        this.description = lookupWithDefault(DESCRIPTION, lookupFunc, this.description);
        this.attributes.addAll(extractAttributes(lookupFunc.apply(OrangebeardProperty.ATTRIBUTES.getPropertyName())));
    }

    private String lookupWithDefault(OrangebeardProperty property, Function<String, String> lookupFunc, String defaultValue) {
        String temp = lookupFunc.apply(property.getPropertyName());
        return temp == null ? defaultValue : temp;
    }

    private UUID lookupUUIDWithDefault(OrangebeardProperty property, Function<String, String> lookupFunc, UUID defaultValue) {
        String temp = lookupFunc.apply(property.getPropertyName());
        if (temp == null || temp.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return UUID.fromString(temp);
        } catch (IllegalArgumentException e) {
            LOGGER.warn(ACCESS_TOKEN.getPropertyName() + " is not a valid UUID!");
            return defaultValue;
        }
    }

    private Set<Attribute> extractAttributes(String attributeString) {
        Set<Attribute> attributes = new HashSet<>();

        if (attributeString == null || attributeString.isEmpty()) {
            return attributes;
        }

        for (String attribute : attributeString.split(";")) {
            if (attribute.contains(":")) {
                String[] keyValuePair = attribute.trim().split(":", 2);
                attributes.add(new Attribute(keyValuePair[0].trim(), keyValuePair[1].trim()));
            } else {
                attributes.add(new Attribute(attribute.trim()));
            }
        }
        return attributes;
    }
}
