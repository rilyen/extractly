package com._6.extractly.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GetZohoDeals {
    private final RestClient restClient = RestClient.create();
    private final AccessTokenZoho token;

    @Value("${zoho.api_url}")
    private String base_url;

    @Value("${zoho.accountOwnerName}")
    private String account_owner_name;

    @Value("${zoho.appLinkName}")
    private String app_link_name;

    @Value("${zoho.allProjects}")
    private String report_link_name;

    // @Value("${zoho.reportLinkName}")
    // private String report_link_name;

    public GetZohoDeals(AccessTokenZoho token) {
        this.token = token;
    }

    public Map<String, Object> getDeals() {
        // Map<String, Object> payload = Map.of("data", extractData);

        Map<String, Object> response = restClient.get()
                .uri(base_url + "/creator/v2.1/data/" + account_owner_name + "/" + app_link_name + "/report/"
                        + report_link_name)
                .header("Authorization", "Zoho-oauthtoken " + token.getToken())
                .retrieve()
                .body(Map.class);
        return response;
    }
}
