package com._6.extractly.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class AccessTokenZoho {
    private final RestClient restClient = RestClient.create();
    // Gets all the fields for the api call from the env file, loaded by application
    // properties from the env
    // Got these fields from zoho self client authorization code flow,
    // Code is generated on zoho with appropirate scope and then did a post request
    // on postman to get the refresh token
    // This token will be sent with every request to Zoho to authenticate the user.

    @Value("${zoho.clientID}")
    private String clinetId;

    @Value("${zoho.clientSecret}")
    private String clientSecret;

    @Value("${zoho.refreshToken}")
    private String refreshToken;

    @Value("${zoho.baseAccountUrl}")
    private String baseUrl;

    /*
     * Format : https://accounts.zoho.in/oauth/v2/token
     * ?client_id=1000.GMB0YULZHJK411248S8I5GZ4CHUEX0
     * &grant_type=refresh_token
     * &client_secret=122c324d3496d5d777ceeebc129470715fbb856b7
     * &refresh_token=1000.18e983526f0ca8575ea9c53b0cd5bb58.
     * 1bd83a6f2e22c3a7e1309d96ae439cc1 <- this whole class just filters this token
     * and
     * sends a new one at every request(expires in 60 mins)
     */

    public String getToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("refresh_token", refreshToken);
        body.add("client_id", clinetId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "refresh_token");

        Map<String, Object> response = restClient.post().uri(baseUrl + "/oauth/v2/token").body(body).retrieve()
                .body(Map.class);

        return (String) response.get("access_token");
    }

}