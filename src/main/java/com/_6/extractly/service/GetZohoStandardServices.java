package com._6.extractly.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GetZohoStandardServices {
    private final RestClient restClient = RestClient.create();
    private final AccessTokenZoho token;

    @Value("${zoho.api_url}")
    private String base_url;

    @Value("${zoho.accountOwnerName}")
    private String account_owner_name;

    @Value("${zoho.appLinkName}")
    private String app_link_name;

    private static final String STANDARD_SERVICES_REPORT = "All_Standard_Products";

    public GetZohoStandardServices(AccessTokenZoho token) {
        this.token = token;
    }

    public Map<String, Object> getStandardServices() {
        Map<String, Object> response = restClient.get()
                .uri(base_url + "/creator/v2.1/data/" + account_owner_name + "/" + app_link_name + "/report/"
                        + STANDARD_SERVICES_REPORT)
                .header("Authorization", "Zoho-oauthtoken " + token.getToken())
                .retrieve()
                .body(Map.class);

        return response;
    }
}