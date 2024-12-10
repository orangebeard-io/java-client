package io.orangebeard.client.entity.alerting.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Evidence {
    private String url;
    private String request;
    private String response;
    private String other;
}
