package io.orangebeard.client;

import io.orangebeard.client.entity.Response;

import io.orangebeard.client.entity.StartTestRun;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;

import static java.lang.String.format;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(MockitoExtension.class)
class OrangebeardV3ClientTest {
    @Mock
    private RestTemplate restTemplate;
    private String projectName;
    private UUID accessToken;

    @BeforeEach
    void setup() {
        projectName = "orangebeard";
        accessToken = UUID.fromString("49d20b01-1085-449a-94e3-1d7718242c55");
    }

    @Test
    void when_the_connection_is_valid_test_run_can_be_started() {
        UUID testRunUUID = UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534");
        OrangebeardV3Client orangebeardClient = new OrangebeardV3Client(restTemplate, "http://localhost:8080", accessToken, projectName, true);

        orangebeardClient.startAnnouncedTestRun(testRunUUID);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(String.valueOf(accessToken));
        headers.setContentType(APPLICATION_JSON);
        HttpEntity<StartTestRun> httpEntity = new HttpEntity<>(headers);

        verify(restTemplate, times(1)).exchange(
                format("http://localhost:8080/listener/v3/%s/test-run/start/%s", projectName, testRunUUID),
                PUT, httpEntity, Response.class);
    }
}
