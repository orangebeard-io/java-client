package io.orangebeard.client;

import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.ChangedComponent;
import io.orangebeard.client.entity.log.LogLevel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.orangebeard.client.OrangebeardProperty.ACCESS_TOKEN;
import static io.orangebeard.client.OrangebeardProperty.DESCRIPTION;
import static io.orangebeard.client.OrangebeardProperty.ENDPOINT;
import static io.orangebeard.client.OrangebeardProperty.LOGS_AT_END_OF_TEST;
import static io.orangebeard.client.OrangebeardProperty.LOG_LEVEL;
import static io.orangebeard.client.OrangebeardProperty.PROJECT;
import static io.orangebeard.client.OrangebeardProperty.REFERENCE_URL;
import static io.orangebeard.client.OrangebeardProperty.TESTSET;
import static io.orangebeard.client.OrangebeardProperty.TEST_RUN_UUID;
import static io.orangebeard.client.OrangebeardProperty.TOKEN;

@ToString
@Getter
@Setter
@AllArgsConstructor
public class OrangebeardProperties {
    private static final String ORANGEBEARD_PROPERTY_FILE = "orangebeard.properties";
    private static final String ORANGEBEARD_JSON_FILE = "orangebeard.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(OrangebeardProperties.class);
    private String endpoint;
    private UUID accessToken;
    private String projectName;
    private String testSetName;
    private String description;
    private Set<Attribute> attributes = new HashSet<>();
    private boolean propertyFilePresent;
    private LogLevel logLevel = LogLevel.INFO;
    private boolean logsAtEndOfTest = false;
    private UUID testRunUUID;
    private Set<ChangedComponent> sutComponents = new HashSet<>();

    private enum PropertyNameStyle {
        DOT, UNDERSCORE
    }

    //Keep AllArgsConstructor without sutComponents for backward compatibility
    public OrangebeardProperties(String endpoint, UUID accessToken, String projectName, String testSetName, String description, Set<Attribute> attributes, LogLevel logLevel, boolean logsAtEndOfTest, UUID testRunUUID) {
        this(endpoint, accessToken, projectName, testSetName, description, attributes, logLevel, logsAtEndOfTest);
        this.testRunUUID = testRunUUID;
    }

    public OrangebeardProperties(String endpoint, UUID accessToken, String projectName, String testSetName, String description, Set<Attribute> attributes, LogLevel logLevel, boolean logsAtEndOfTest) {
        this.endpoint = endpoint;
        this.accessToken = accessToken;
        this.projectName = projectName;
        this.testSetName = testSetName;
        this.description = description;
        this.attributes = attributes;
        this.propertyFilePresent = false;
        this.logLevel = logLevel;
        this.logsAtEndOfTest = logsAtEndOfTest;
    }

    /**
     * Required args constructor in order to allow listeners to read their properties in a custom way.
     */
    public OrangebeardProperties(String endpoint, UUID accessToken, String projectName, String testSetName) {
        this.endpoint = endpoint;
        this.accessToken = accessToken;
        this.projectName = projectName;
        this.testSetName = testSetName;
        this.propertyFilePresent = false;
    }

    OrangebeardProperties(String propertyFile, String jsonFileName) {
        readPropertyFile(propertyFile);
        readPropertyJsonFile(jsonFileName);
        readSystemProperties();
        readEnvironmentVariables(PropertyNameStyle.DOT);
        readEnvironmentVariables(PropertyNameStyle.UNDERSCORE);
    }

    OrangebeardProperties(String propertyFile) {
        readPropertyFile(propertyFile);
        readPropertyJsonFile(ORANGEBEARD_JSON_FILE);
        readSystemProperties();
        readEnvironmentVariables(PropertyNameStyle.DOT);
        readEnvironmentVariables(PropertyNameStyle.UNDERSCORE);
    }

    /**
     * Automatic configuration constructor
     */
    @SuppressWarnings("unused")
    public OrangebeardProperties() {
        this(ORANGEBEARD_PROPERTY_FILE, ORANGEBEARD_JSON_FILE);
    }

    public boolean requiredValuesArePresent() {
        return endpoint != null && accessToken != null && projectName != null && testSetName != null;
    }

    public boolean isAnnouncedUUIDPresent() {
        return testRunUUID != null;
    }

    public void checkPropertiesArePresent() {
        if (!requiredValuesArePresent()) {
            LOGGER.error("Required Orangebeard properties are missing. Not all environment variables are present, " +
                    "and/or orangebeard.properties misses required values!\n{}", requiredPropertiesToString());
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

    private void readPropertyJsonFile(String jsonFileName) {
        JSONObject jsonConfig = getJsonConfig(jsonFileName);
        if (jsonConfig != null) {
            readPropertiesWith(key -> getJsonConfigValue(key, jsonConfig));
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

    private static JSONObject getJsonConfig(String jsonFileName) {
        Path currentDir = Paths.get("").toAbsolutePath();
        while (currentDir != null) {
            Path filePath = currentDir.resolve(jsonFileName);
            if (Files.exists(filePath)) {
                try {
                    String content = Files.readString(filePath, StandardCharsets.UTF_8);
                    return new JSONObject(content);
                } catch (IOException e) {
                    return null;
                }
            }
            currentDir = currentDir.getParent();
        }
        return null;
    }

    private String getJsonConfigValue(String key, JSONObject jsonConfiguration) {
        try {
            return jsonConfiguration.get(key.replace("orangebeard.", "")).toString();
        } catch (JSONException e) {
            return null;
        }
    }

    private void readPropertiesWith(UnaryOperator<String> lookupFunc) {
        this.endpoint = lookupWithDefault(ENDPOINT, lookupFunc, this.endpoint);
        this.accessToken = lookupUUIDWithDefault(TOKEN, lookupFunc, this.accessToken);
        if (this.accessToken == null) {
            this.accessToken = lookupUUIDWithDefault(ACCESS_TOKEN, lookupFunc, null);
        }
        this.projectName = lookupWithDefault(PROJECT, lookupFunc, this.projectName);
        this.testSetName = lookupWithDefault(TESTSET, lookupFunc, this.testSetName);
        this.description = lookupWithDefault(DESCRIPTION, lookupFunc, this.description);
        this.logLevel = lookupLogLevel(lookupFunc);
        this.logsAtEndOfTest = lookUpBooleanWithDefault(LOGS_AT_END_OF_TEST, lookupFunc, this.logsAtEndOfTest);
        this.attributes.addAll(extractAttributes(lookupFunc.apply(OrangebeardProperty.ATTRIBUTES.getPropertyName())));
        this.sutComponents.addAll(extractComponents(lookupFunc.apply(OrangebeardProperty.SUT_COMPONENTS.getPropertyName())));
        this.testRunUUID = lookupUUIDWithDefault(TEST_RUN_UUID, lookupFunc, this.testRunUUID);
        if (lookupWithDefault(REFERENCE_URL, lookupFunc, null) != null) {
            this.attributes.add(new Attribute("reference_url", lookupWithDefault(REFERENCE_URL, lookupFunc, null)));
        }
    }

    @SuppressWarnings("SameParameterValue")
    private boolean lookUpBooleanWithDefault(OrangebeardProperty property, UnaryOperator<String> lookupFunc, boolean defaultValue) {
        String temp = lookupFunc.apply(property.getPropertyName());
        if (temp == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(temp);
    }

    private String lookupWithDefault(OrangebeardProperty property, UnaryOperator<String> lookupFunc, String defaultValue) {
        String temp = lookupFunc.apply(property.getPropertyName());
        return temp == null ? defaultValue : temp;
    }

    @SuppressWarnings("SameParameterValue")
    private UUID lookupUUIDWithDefault(OrangebeardProperty property, UnaryOperator<String> lookupFunc, UUID defaultValue) {
        String temp = lookupFunc.apply(property.getPropertyName());
        if (temp == null || temp.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return UUID.fromString(temp);
        } catch (IllegalArgumentException e) {
            LOGGER.warn(ACCESS_TOKEN.getPropertyName(), "{0} is not a valid UUID!");
            return defaultValue;
        }
    }

    private LogLevel lookupLogLevel(UnaryOperator<String> lookupFunc) {
        String logLevelProperty = lookupFunc.apply(LOG_LEVEL.getPropertyName());
        try {
            return logLevelProperty == null ? logLevel : LogLevel.valueOf(logLevelProperty);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("{} is not a valid log level! Choose DEBUG, INFO, WARN or ERROR. INFO is now used by default.", LOG_LEVEL.getPropertyName());
            return logLevel;
        }
    }

    public static Set<Attribute> extractAttributes(String attributeString) {
        Set<Attribute> attrs = new HashSet<>();

        if (attributeString == null || attributeString.isEmpty()) {
            return attrs;
        }

        if (attributeString.startsWith("[")) {
            JSONArray jsonAttrs = new JSONArray(attributeString);
            for (Object attribute : jsonAttrs) {
                JSONObject attr = (JSONObject) attribute;
                attrs.add(attr.has("key") ?
                        new Attribute(attr.getString("key"), attr.getString("value")) :
                        new Attribute(attr.getString("value")));
            }
        } else {
            for (String attribute : attributeString.split(";")) {
                if (attribute.contains(":")) {
                    String[] keyValuePair = attribute.trim().split(":", 2);
                    attrs.add(new Attribute(keyValuePair[0].trim(), keyValuePair[1].trim()));
                } else {
                    attrs.add(new Attribute(attribute.trim()));
                }
            }
        }

        return attrs;
    }

    private Set<ChangedComponent> extractComponents(String componentsString) {
        Set<ChangedComponent> components = new HashSet<>();

        if (componentsString == null || componentsString.isEmpty()) {
            return components;
        }

        if (componentsString.startsWith("[")) {
            JSONArray jsonComponents = new JSONArray(componentsString);
            for (Object c : jsonComponents) {
                JSONObject component = (JSONObject) c;
                components.add(new ChangedComponent(component.getString("name"), component.getString("version")));
            }
        } else {
            for (String component : componentsString.split(";")) {
                if (component.contains(":")) {
                    String[] keyValuePair = component.trim().split(":", 2);
                    components.add(new ChangedComponent(keyValuePair[0].trim(), keyValuePair[1].trim()));
                }
            }
        }

        return components;
    }

    public boolean logShouldBeDispatchedToOrangebeard(LogLevel individualLogLevel) {
        int logLevelVal = convertToInt(this.logLevel);
        int individualLogLevelVal = convertToInt(individualLogLevel);
        return individualLogLevelVal >= logLevelVal;
    }

    private int convertToInt(LogLevel logLevel) {
        switch (logLevel) {
            case DEBUG:
                return 0;
            case INFO:
                return 1;
            case WARN:
                return 2;
            case ERROR:
        }
        return 1;
    }
}
