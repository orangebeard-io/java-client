package io.orangebeard.client;

import io.orangebeard.client.entity.Attribute;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;

class OrangebeardPropertiesTest {

    @Test
    public void property_file_is_read_correctly() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("orangebeardpropertiestest.properties");

        assertThat(orangebeardProperties.requiredValuesArePresent()).isTrue();
        assertThat(orangebeardProperties.isPropertyFilePresent()).isTrue();

        assertThat(orangebeardProperties.getEndpoint()).isEqualTo("https://company.orangebeard.app");
        assertThat(orangebeardProperties.getAccessToken()).isEqualTo(UUID.fromString("043584a0-8081-4270-a32a-ad79ead2dc34"));
        assertThat(orangebeardProperties.getTestSetName()).isEqualTo("piet_TEST_EXAMPLE");
        assertThat(orangebeardProperties.getProjectName()).isEqualTo("piet_personal");
        assertThat(orangebeardProperties.getDescription()).isEqualTo("My awesome testrun");
        assertThat(orangebeardProperties.getAttributes()).containsOnly(new Attribute("key", "value"), new Attribute("value"));
    }

    @Test
    public void when_no_property_file_is_present_no_exception_is_thrown() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("piet.properties");

        assertThat(orangebeardProperties.requiredValuesArePresent()).isFalse();
        assertThat(orangebeardProperties.isPropertyFilePresent()).isFalse();
    }

    @Test
    public void splitting_properties() {
        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("attributestest01.properties");

        assertThat(orangebeardProperties.getAttributes()).containsOnly(
                new Attribute("key", "value"),
                new Attribute("value"),
                new Attribute("test", "testsys temslim"));
    }

    @Test
    public void reading_attributes_from_environment_variables() throws Exception {
        withEnvironmentVariable("orangebeard.attributes", "env:value;piet:pietersen")
                .execute(() -> {
                    OrangebeardProperties orangebeardProperties = new OrangebeardProperties("attributestest01.properties");
                    assertThat(orangebeardProperties.getAttributes()).containsOnly(
                            new Attribute("key", "value"),
                            new Attribute("value"),
                            new Attribute("test", "testsys temslim"),
                            new Attribute("env", "value"),
                            new Attribute("piet", "pietersen"));
                });
    }

    @Test
    public void reading_attributes_from_system_properties() {
        System.setProperty("orangebeard.attributes", "env:value;piet :pietersen ;");

        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("attributestest01.properties");
        assertThat(orangebeardProperties.getAttributes()).containsOnly(
                new Attribute("key", "value"),
                new Attribute("value"),
                new Attribute("test", "testsys temslim"),
                new Attribute("env", "value"),
                new Attribute("piet", "pietersen"));
        System.clearProperty("orangebeard.attributes");
    }

    @Test
    public void reading_attributes_from_null_system_properties() {
        System.setProperty("orangebeard.attributes", "");

        OrangebeardProperties orangebeardProperties = new OrangebeardProperties("attributestest01.properties");
        assertThat(orangebeardProperties.getAttributes()).containsOnly(
                new Attribute("key", "value"),
                new Attribute("value"),
                new Attribute("test", "testsys temslim"));
        System.clearProperty("orangebeard.attributes");
    }
}
