package io.orangebeard.client.entity.alerting.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Evidence {
    String url;
    String method;
    int httpStatusCode;
    Map<String, String> requestHeaders;
    String requestBody = null;
    Map<String, String> responseHeaders;
    String responseBody = null;
    String other = null;
}

