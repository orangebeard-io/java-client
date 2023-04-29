package io.orangebeard.client;

import io.orangebeard.client.entity.Attribute;

import io.orangebeard.client.entity.log.LogLevel;

import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static io.orangebeard.client.OrangebeardProperty.ACCESS_TOKEN;
import static io.orangebeard.client.OrangebeardProperty.DESCRIPTION;
import static io.orangebeard.client.OrangebeardProperty.ENDPOINT;
import static io.orangebeard.client.OrangebeardProperty.LOGS_AT_END_OF_TEST;
import static io.orangebeard.client.OrangebeardProperty.LOG_LEVEL;
import static io.orangebeard.client.OrangebeardProperty.PROJECT;
import static io.orangebeard.client.OrangebeardProperty.TESTSET;
import static io.orangebeard.client.OrangebeardProperty.TEST_RUN_UUID;

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
    private LogLevel logLevel = LogLevel.INFO;
    private boolean logsAtEndOfTest = false;
    private UUID testRunUUID;

    private enum PropertyNameStyle {
        DOT, UNDERSCORE
    }

    /**
     * all args constructor in order to allow listeners to read their properties in a custom way.
     */
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

    OrangebeardProperties(String propertyFile) {
        readPropertyFile(propertyFile);
        readSystemProperties();
        readEnvironmentVariables(PropertyNameStyle.DOT);
        readEnvironmentVariables(PropertyNameStyle.UNDERSCORE);
    }

    @SuppressWarnings("unused")
    public OrangebeardProperties() {
        this(ORANGEBEARD_PROPERTY_FILE);
    }

    public boolean requiredValuesArePresent() {
        return endpoint != null && accessToken != null && projectName != null && testSetName != null;
    }

    public boolean isAnnouncedUUIDPresent() {
        return testRunUUID != null;
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
        this.logLevel = lookupLogLevel(lookupFunc);
        this.logsAtEndOfTest = lookUpBooleanWithDefault(LOGS_AT_END_OF_TEST, lookupFunc, this.logsAtEndOfTest);
        this.attributes.addAll(extractAttributes(lookupFunc.apply(OrangebeardProperty.ATTRIBUTES.getPropertyName())));
        this.testRunUUID = lookupUUIDWithDefault(TEST_RUN_UUID, lookupFunc, this.testRunUUID);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean lookUpBooleanWithDefault(OrangebeardProperty property, Function<String, String> lookupFunc, boolean defaultValue) {
        String temp = lookupFunc.apply(property.getPropertyName());
        if (temp == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(temp);
    }

    private String lookupWithDefault(OrangebeardProperty property, Function<String, String> lookupFunc, String defaultValue) {
        String temp = lookupFunc.apply(property.getPropertyName());
        return temp == null ? defaultValue : temp;
    }

    @SuppressWarnings("SameParameterValue")
    private UUID lookupUUIDWithDefault(OrangebeardProperty property, Function<String, String> lookupFunc, UUID defaultValue) {
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

    private LogLevel lookupLogLevel(Function<String, String> lookupFunc) {
        String logLevel = lookupFunc.apply(LOG_LEVEL.getPropertyName());
        try {
            return logLevel == null ? this.logLevel : LogLevel.valueOf(logLevel);
        } catch (IllegalArgumentException e) {
            LOGGER.warn(LOG_LEVEL.getPropertyName() + "is not a valid log level! Choose DEBUG, INFO, WARN or ERROR. INFO is now used by default.");
            return this.logLevel;
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

    public boolean logShouldBeDispatchedToOrangebeard(LogLevel individualLogLevel) {
        int logLevelVal = convertToInt(this.logLevel);
        int individualLogLevelVal = convertToInt(individualLogLevel);
        return individualLogLevelVal >= logLevelVal;
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
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
