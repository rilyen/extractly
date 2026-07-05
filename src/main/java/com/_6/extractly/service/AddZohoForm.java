package com._6.extractly.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AddZohoForm {
    private final RestClient restClient = RestClient.create();
    private final AccessTokenZoho token;

    @Value("${zoho.api_url}")
    private String base_url;

    @Value("${zoho.accountOwnerName}")
    private String account_owner_name;

    @Value("${zoho.appLinkName}")
    private String app_link_name;

    @Value("${zoho.formLinkName}")
    private String form_link_name;

    public AddZohoForm(AccessTokenZoho token) {
        this.token = token;
    }

    public Map<String, Object> sendInfo(Map<String, Object> extractData) {
        Map<String, Object> payload = Map.of("data", extractData);

        Map<String, Object> response = restClient.post()
                .uri(base_url + "/creator/v2.1/data/" + account_owner_name + "/" + app_link_name + "/form/"
                        + form_link_name)
                .header("Authorization", "Zoho-oauthtoken " + token.getToken()).contentType(MediaType.APPLICATION_JSON)
                .body(payload).retrieve().body(Map.class);

        return response;
    }
}
