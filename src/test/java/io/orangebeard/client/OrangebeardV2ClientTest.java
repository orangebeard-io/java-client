package io.orangebeard.client;

import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;

@ExtendWith(MockitoExtension.class)
class OrangebeardV2ClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    public void when_the_connection_is_valid_a_log_is_reported_to_orangebeard() {
        OrangebeardV2Client orangebeardClient = new OrangebeardV2Client(restTemplate, "http://localhost:8080", UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534"), "project", true);

        orangebeardClient.log(Log.builder().build());

        verify(restTemplate, times(1)).exchange(eq("http://localhost:8080/listener/v2/project/log"), eq(POST), any(), eq(Response.class));
    }

    @Test
    public void when_the_connection_is_invalid_no_log_is_reported_to_orangebeard() {
        OrangebeardV2Client orangebeardClient = new OrangebeardV2Client(restTemplate, "http://localhost:8080", UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534"), "project", false);

        orangebeardClient.log(Log.builder().build());

        verify(restTemplate, times(0)).exchange(eq("http://localhost:8080/listener/v2/project/log"), eq(POST), any(), eq(Response.class));
    }

    @Test
    public void when_the_connection_throws_a_timeout_exception_this_is_properly_handled() {
        when(restTemplate.exchange(eq("http://localhost:8080/listener/v2/project/log"), eq(POST), any(), eq(Response.class))).thenThrow(new ResourceAccessException("error!"));

        OrangebeardV2Client orangebeardClient = new OrangebeardV2Client(restTemplate, "http://localhost:8080", UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534"), "project", true);

        assertThatCode(() -> orangebeardClient.log(Log.builder().build())).doesNotThrowAnyException();
    }

    @Test
    public void when_the_connection_throws_a_gateway_timeout_exception_this_is_properly_handled() {
        when(restTemplate.exchange(eq("http://localhost:8080/listener/v2/project/log"), eq(POST), any(), eq(Response.class))).thenThrow(new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT, "error!"));

        OrangebeardV2Client orangebeardClient = new OrangebeardV2Client(restTemplate, "http://localhost:8080", UUID.fromString("92580f91-073a-4bf7-aa10-bb4f8dbcb534"), "project", true);

        assertThatCode(() -> orangebeardClient.log(Log.builder().build())).doesNotThrowAnyException();
    }
}
