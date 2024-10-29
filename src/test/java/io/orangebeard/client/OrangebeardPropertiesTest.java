package io.orangebeard.client;

import io.orangebeard.client.entity.Attribute;

import io.orangebeard.client.entity.log.LogLevel;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrangebeardPropertiesTest {

    @Test
    void property_file_is_read_correctly_with_specified_filename() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("orangebeardpropertiestest.properties", "no.json");

        assertThat(orangebeardProperties.requiredValuesArePresent()).isTrue();
        assertThat(orangebeardProperties.isPropertyFilePresent()).isTrue();

        assertThat(orangebeardProperties.getEndpoint()).isEqualTo("https://company.orangebeard.app");
        assertThat(orangebeardProperties.getAccessToken()).isEqualTo(UUID.fromString("043584a0-8081-4270-a32a-ad79ead2dc34"));
        assertThat(orangebeardProperties.getTestSetName()).isEqualTo("piet_TEST_EXAMPLE");
        assertThat(orangebeardProperties.getProjectName()).isEqualTo("piet_personal");
        assertThat(orangebeardProperties.getDescription()).isEqualTo("My awesome testrun");
        assertThat(orangebeardProperties.getLogLevel()).isEqualTo(LogLevel.WARN);
        assertThat(orangebeardProperties.isLogsAtEndOfTest()).isTrue();
        assertThat(orangebeardProperties.getAttributes()).containsOnly(new Attribute("key", "value"), new Attribute("value"));
    }

    @Test
    void properties_can_be_loaded_from_json_file() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("foo.properties", "orangebeard.json");
        assertThat(orangebeardProperties.requiredValuesArePresent()).isTrue();
        assertThat(orangebeardProperties.getAttributes()).containsOnly(
                new Attribute("Key 1", "Some value"),
                new Attribute("Tag value"));
    }

    @Test
    void when_no_property_file_or_json_file_is_present_no_exception_is_thrown() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("piet.properties", "bar.json");

        assertThat(orangebeardProperties.requiredValuesArePresent()).isFalse();
        assertThat(orangebeardProperties.isPropertyFilePresent()).isFalse();
    }

    @Test
    void splitting_attributes() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("attributestest01.properties", "no.json");

        assertThat(orangebeardProperties.getAttributes()).containsOnly(
                new Attribute("key", "value"),
                new Attribute("value"),
                new Attribute("test", "testsys tem:slim"));
    }

    @Test
    @SetEnvironmentVariable(key = "orangebeard.attributes", value = "env:value;piet:pietersen")
    void reading_attributes_from_environment_variables() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("attributestest01.properties", "no.json");
        assertThat(orangebeardProperties.getAttributes()).containsOnly(
                new Attribute("key", "value"),
                new Attribute("value"),
                new Attribute("test", "testsys tem:slim"),
                new Attribute("env", "value"),
                new Attribute("piet", "pietersen"));
    }

    @Test
    void when_loglevel_is_invalid_it_is_set_to_info_by_default() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("invalidloglevel.properties");

        assertThat(orangebeardProperties.getLogLevel()).isEqualTo(LogLevel.INFO);
    }

    @Test
    void when_loglevel_is_info_debug_logs_are_not_sent() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("invalidloglevel.properties");

        assertThat(orangebeardProperties.getLogLevel()).isEqualTo(LogLevel.INFO);
        assertThat(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.ERROR)).isTrue();
        assertThat(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.WARN)).isTrue();
        assertThat(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.INFO)).isTrue();
        assertThat(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.DEBUG)).isFalse();
    }

    @Test
    void when_the_loglevel_is_warn_a_log_with_level_info_should_not_be_sent() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("orangebeardpropertiestest.properties");

        assertThat(orangebeardProperties.getLogLevel()).isEqualTo(LogLevel.WARN);
        assertThat(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.ERROR)).isFalse();
        assertThat(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.WARN)).isTrue();
        assertThat(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.INFO)).isFalse();
        assertThat(orangebeardProperties.logShouldBeDispatchedToOrangebeard(LogLevel.DEBUG)).isFalse();
    }

    @Test
    void when_logs_at_end_is_invalid_it_is_set_to_false_by_default() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("invalidbooleanlogsatendoftest.properties");
        assertThat(orangebeardProperties.isLogsAtEndOfTest()).isFalse();
    }

    @Test
    void reading_attributes_from_system_properties() {
        System.setProperty("orangebeard.attributes", "env:value;piet :pietersen ;");

        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("attributestest01.properties", "no.json");
        assertThat(orangebeardProperties.getAttributes()).containsOnly(
                new Attribute("key", "value"),
                new Attribute("value"),
                new Attribute("test", "testsys tem:slim"),
                new Attribute("env", "value"),
                new Attribute("piet", "pietersen"));
        System.clearProperty("orangebeard.attributes");
    }

    @Test
    void reading_attributes_from_null_system_properties() {
        System.setProperty("orangebeard.attributes", "");

        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("attributestest01.properties", "no.json");
        assertThat(orangebeardProperties.getAttributes()).containsOnly(
                new Attribute("key", "value"),
                new Attribute("value"),
                new Attribute("test", "testsys tem:slim"));
        System.clearProperty("orangebeard.attributes");
    }

    @Test
    void reading_invalid_UUID_does_not_prevent_reading_other_properties() {
        System.setProperty("orangebeard.attributes", "");

        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("attributestest01_invalid_accessToken.properties", "no.json");
        assertThat(orangebeardProperties.getProjectName()).isEqualTo("piet_personal");

        System.clearProperty("orangebeard.attributes");
    }

    @Test
    void checkPropertiesArePresent_error() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("");
        orangebeardProperties.checkPropertiesArePresent();
    }
}
