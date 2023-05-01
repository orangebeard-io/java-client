package io.orangebeard.client;

import io.orangebeard.client.entity.FinishV3TestRun;
import io.orangebeard.client.entity.LogFormat;
import io.orangebeard.client.entity.Response;

import io.orangebeard.client.entity.StartTestRun;

import io.orangebeard.client.entity.StartV3TestRun;

import io.orangebeard.client.entity.attachment.Attachment;
import io.orangebeard.client.entity.log.Log;
import io.orangebeard.client.entity.log.LogLevel;
import io.orangebeard.client.entity.step.FinishStep;
import io.orangebeard.client.entity.step.StartStep;
import io.orangebeard.client.entity.suite.StartSuite;
import io.orangebeard.client.entity.suite.Suite;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

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

    /*     TEST RUN     */

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
        StartV3TestRun startTestRun = StartV3TestRun.builder()
                .testSetName("test set#1")
                .description("test")
                .startTime(ZonedDateTime.now())
                .build();
        HttpEntity<StartV3TestRun> httpEntity = new HttpEntity<>(startTestRun, headers);
        ResponseEntity<UUID> response = new ResponseEntity<>(uuid, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(POST), eq(httpEntity), eq(UUID.class))).thenReturn(response);

        UUID testRunUUID = orangebeardV3Client.startTestRun(startTestRun);

        assert testRunUUID.equals(uuid);
        verify(restTemplate, times(1)).exchange(format("%s/listener/v3/%s/test-run/start", endpoint, projectName), POST, httpEntity, UUID.class);
    }

    @Test
    void when_the_connection_is_valid_test_run_can_be_finished() {
        UUID testRunUUID = UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534");
        FinishV3TestRun finishTestRun = new FinishV3TestRun();
        HttpEntity<FinishV3TestRun> httpEntity = new HttpEntity<>(finishTestRun, headers);

        when(restTemplate.exchange(anyString(), eq(PUT), eq(httpEntity), eq(Void.class))).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        orangebeardV3Client.finishTestRun(testRunUUID, finishTestRun);

        verify(restTemplate, times(1)).exchange(format("http://localhost:8080/listener/v3/%s/test-run/finish/%s", projectName, testRunUUID), PUT, httpEntity, Void.class);
    }

    /*     SUITE     */

    @Test
    void when_the_connection_is_valid_suite_can_be_started() {
        UUID uuid = UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534");
        UUID parentUUID = UUID.randomUUID();
        List<String> suiteNames = new ArrayList<>(2);
        suiteNames.add("suite1");
        suiteNames.add("suite2");
        StartSuite suite = StartSuite.builder()
                .testRunUUID(uuid)
                .parentSuiteUUID(parentUUID)
                .description("test")
                .suiteNames(suiteNames)
                .build();
        HttpEntity<StartSuite> httpEntity = new HttpEntity<>(suite, headers);

        Suite suite1 = new Suite(UUID.randomUUID(), parentUUID, "IntegrationTests", Collections.singletonList("ApiTests.IntegrationTests"));
        Suite suite2 = new Suite(UUID.randomUUID(), parentUUID, "UnitTests", Collections.singletonList("ApiTests.UnitTests"));
        Suite[] suiteList = new Suite[]{suite1, suite2};
        ResponseEntity<Suite[]> response = new ResponseEntity<>(suiteList, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(POST), eq(httpEntity), eq(Suite[].class))).thenReturn(response);

        List<Suite> suites = orangebeardV3Client.startSuite(suite);

        assert suites.get(0).equals(suite1);
        assert suites.get(1).equals(suite2);
        verify(restTemplate, times(1)).exchange(format("%s/listener/v3/%s/suite/start", endpoint, projectName), POST, httpEntity, Suite[].class);
    }

    /*      TEST      */

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

    /*      STEP      */

    @Test
    void when_the_connection_is_valid_step_be_started() {
        UUID testRunUUID = UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534");
        UUID testUUID = UUID.fromString("9fc9eb6e-264e-4d0e-806e-85a65fe99da9");
        StartStep startStep = StartStep.builder()
                .testRunUUID(testRunUUID)
                .testUUID(testUUID)
                .parentStepUUID(UUID.randomUUID())
                .stepName("Test-Step")
                .description("test")
                .startTime(ZonedDateTime.now())
                .build();
        HttpEntity<StartStep> httpEntity = new HttpEntity<>(startStep, headers);
        ResponseEntity<UUID> response = new ResponseEntity<>(UUID.randomUUID(), HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(POST), eq(httpEntity), eq(UUID.class))).thenReturn(response);

        UUID uuid = orangebeardV3Client.startStep(startStep);

        assert uuid.equals(response.getBody());
        verify(restTemplate, times(1)).exchange(format("%s/listener/v3/%s/step/start", endpoint, projectName), POST, httpEntity, UUID.class);
    }

    @Test
    void when_the_connection_is_valid_step_can_be_finished_properly() {
        UUID testRunUUID = UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534");
        FinishStep finishStep = FinishStep.builder()
                .testRunUUID(testRunUUID)
                .status(TestStatus.PASSED)
                .endTime(ZonedDateTime.now())
                .build();
        HttpEntity<FinishStep> httpEntity = new HttpEntity<>(finishStep, headers);

        when(restTemplate.exchange(anyString(), eq(PUT), eq(httpEntity), eq(Void.class))).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        orangebeardV3Client.finishStep(testRunUUID, finishStep);

        verify(restTemplate, times(1)).exchange(format("%s/listener/v3/%s/step/finish/%s", endpoint, projectName, testRunUUID), PUT, httpEntity, Void.class);
    }

    /*      LOG      */

    @Test
    void when_the_connection_is_valid_log_can_be_sent() {
        UUID testRunUUID = UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534");
        UUID testUUID = UUID.fromString("9fc9eb6e-264e-4d0e-806e-85a65fe99da9");
        Log log = Log.builder()
                .testRunUUID(testRunUUID)
                .testUUID(testUUID)
                .stepUUID(UUID.randomUUID())
                .message("Log message")
                .logLevel(LogLevel.WARN)
                .logFormat(LogFormat.PLAIN_TEXT)
                .build();
        HttpEntity<Log> httpEntity = new HttpEntity<>(log, headers);
        ResponseEntity<UUID> response = new ResponseEntity<>(UUID.randomUUID(), HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(POST), eq(httpEntity), eq(UUID.class))).thenReturn(response);

        UUID uuid = orangebeardV3Client.log(log);

        assert uuid.equals(response.getBody());
        verify(restTemplate, times(1)).exchange(format("%s/listener/v3/%s/log", endpoint, projectName), POST, httpEntity, UUID.class);
    }

    /*      ATTACHMENT      */

    @Test
    void when_the_connection_is_valid_attachment_can_be_sent() {
        UUID stepUUID = UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534");
        UUID testUUID = UUID.fromString("9fc9eb6e-264e-4d0e-806e-85a65fe99da9");
        byte[] content = new byte[]{(byte) 0x91, 0x19, 0x38, 0x14, 0x47, 0x21, 0x11};

        Attachment.AttachmentFile file = Attachment.AttachmentFile.builder()
                .name("img_file")
                .content(content)
                .contentType("image/jpg")
                .build();
        Attachment.AttachmentMetaData metaData = Attachment.AttachmentMetaData.builder()
                .logUUID(UUID.randomUUID())
                .testUUID(testUUID)
                .stepUUID(stepUUID)
                .attachmentTime(ZonedDateTime.now())
                .build();
        Attachment attachment = new Attachment(file, metaData);

        LinkedMultiValueMap<String, String> filePartHeaders = new LinkedMultiValueMap<>();
        filePartHeaders.add(CONTENT_DISPOSITION, format("form-data; name=\"attachment\"; filename=\"%s\"", file.getName()));
        filePartHeaders.add(CONTENT_TYPE, file.getContentType());

        HttpEntity<byte[]> filePart = new HttpEntity<>(content, filePartHeaders);

        LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("json", metaData);
        parts.add("attachment", filePart);

        Objects.requireNonNull(headers.get(CONTENT_TYPE)).set(0, String.valueOf(MULTIPART_FORM_DATA));
        HttpEntity<LinkedMultiValueMap<String, Object>> request = new HttpEntity<>(parts, headers);

        ResponseEntity<UUID> response = new ResponseEntity<>(UUID.randomUUID(), HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(POST), eq(request), eq(UUID.class))).thenReturn(response);

        UUID uuid = orangebeardV3Client.sendAttachment(attachment);

        assert uuid.equals(response.getBody());
        verify(restTemplate, times(1)).exchange(format("%s/listener/v3/%s/attachment", endpoint, projectName), POST, request, UUID.class);
    }
}
