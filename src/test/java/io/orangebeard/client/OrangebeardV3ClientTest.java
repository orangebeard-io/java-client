package io.orangebeard.client;

import io.orangebeard.client.entity.FinishTestRun;
import io.orangebeard.client.entity.Response;

import io.orangebeard.client.entity.StartTestRun;

import io.orangebeard.client.entity.Status;

import io.orangebeard.client.entity.test.FinishTest;

import io.orangebeard.client.entity.test.StartTest;
import io.orangebeard.client.entity.test.TestStatus;

import io.orangebeard.client.entity.test.TestType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(MockitoExtension.class)
class OrangebeardV3ClientTest {
    private static RestTemplate restTemplate;
    private static String endpoint;
    private static String projectName;
    private static HttpHeaders headers;
    @InjectMocks
    private static OrangebeardV3Client orangebeardV3Client;

    @BeforeAll
    static void setup() {
        endpoint = "http://localhost:8080";
        projectName = "orangebeard";
        UUID accessToken = UUID.fromString("49d20b01-1085-449a-94e3-1d7718242c55");

        headers = new HttpHeaders();
        headers.setBearerAuth(String.valueOf(accessToken));
        headers.setContentType(APPLICATION_JSON);
        restTemplate = mock(RestTemplate.class);

        orangebeardV3Client =
                new OrangebeardV3Client(restTemplate, endpoint, accessToken,
                        projectName, true);
    }

    @Test
    void when_the_connection_is_valid_test_run_can_be_announced() {
        UUID testRunUUID = UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534");
        HttpEntity<StartTestRun> httpEntity = new HttpEntity<>(headers);

        when(restTemplate.exchange(anyString(), eq(PUT), eq(httpEntity), eq(Void.class))).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        orangebeardV3Client.startAnnouncedTestRun(testRunUUID);

        verify(restTemplate, times(1)).exchange(format("%s/listener/v3/%s/test-run/start/%s", endpoint, projectName, testRunUUID), PUT, httpEntity, Response.class);
    }

    @Test
    void when_the_connection_is_valid_test_run_can_be_started() {
        UUID uuid = UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534");
        StartTestRun startTestRun = StartTestRun.builder()
                .name("test set#1")
                .description("test")
                .startTime(LocalDateTime.now())
                .build();
        HttpEntity<StartTestRun> httpEntity = new HttpEntity<>(startTestRun, headers);
        ResponseEntity<UUID> response = new ResponseEntity<>(uuid, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(POST), eq(httpEntity), eq(UUID.class))).thenReturn(response);

        UUID testRunUUID = orangebeardV3Client.startTestRun(startTestRun);

        assert testRunUUID.equals(uuid);
        verify(restTemplate, times(1)).exchange(format("%s/listener/v3/%s/test-run/start", endpoint, projectName), POST, httpEntity, Response.class);
    }

    @Test
    void when_the_connection_is_valid_test_run_can_be_finished() {
        UUID testRunUUID = UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534");
        FinishTestRun finishTestRun = FinishTestRun.builder().status(Status.PASSED).build();
        HttpEntity<FinishTestRun> httpEntity = new HttpEntity<>(finishTestRun, headers);

        when(restTemplate.exchange(anyString(), eq(PUT), eq(httpEntity), eq(Void.class))).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        orangebeardV3Client.finishTestRun(testRunUUID, finishTestRun);

        verify(restTemplate, times(1)).exchange(format("http://localhost:8080/listener/v3/%s/test-run/finish/%s", projectName, testRunUUID), PUT, httpEntity, Void.class);
    }

    @Test
    void when_the_connection_is_valid_test_can_be_started() {
        UUID testRunUUID = UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534");
        StartTest startTest = StartTest.builder()
                .testRunUUID(testRunUUID)
                .suiteUUID(UUID.randomUUID())
                .testName("unit-tests")
                .testType(TestType.TEST)
                .startTime(ZonedDateTime.now())
                .build();
        HttpEntity<StartTest> httpEntity = new HttpEntity<>(startTest, headers);
        ResponseEntity<UUID> response = new ResponseEntity<>(UUID.randomUUID(), HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(POST), eq(httpEntity), eq(UUID.class))).thenReturn(response);

        UUID uuid = orangebeardV3Client.startTest(startTest);

        assert uuid.equals(response.getBody());
        verify(restTemplate, times(1)).exchange(format("%s/listener/v3/%s/test/start", endpoint, projectName), POST, httpEntity, UUID.class);
    }

    @Test
    void when_the_connection_is_valid_test_can_be_finished_properly() {
        UUID testRunUUID = UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534");
        FinishTest finishTest = FinishTest.builder()
                .testRunUUID(testRunUUID)
                .status(TestStatus.PASSED)
                .endTime(ZonedDateTime.now())
                .build();
        HttpEntity<FinishTest> httpEntity = new HttpEntity<>(finishTest, headers);

        when(restTemplate.exchange(anyString(), eq(PUT), eq(httpEntity), eq(Void.class))).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        orangebeardV3Client.finishTest(testRunUUID, finishTest);

        verify(restTemplate, times(1)).exchange(format("%s/listener/v3/%s/test/finish/%s", endpoint, projectName, testRunUUID), PUT, httpEntity, Void.class);
    }
}
