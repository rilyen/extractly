package com._6.extractly.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ZohoPromptService {

    private final RestClient restClient = RestClient.create();
    private final AccessTokenZoho token;

    @Value("${zoho.api_url}")
    private String base_url;

    @Value("${zoho.accountOwnerName}")
    private String account_owner_name;

    @Value("${zoho.appLinkName}")
    private String app_link_name;

    @Value("${zoho.promptReportLinkName}")
    private String prompt_report_link_name;

    public ZohoPromptService(AccessTokenZoho token) {
        this.token = token;
    }

    public String getPrompt() {
        Map response = restClient.get()
                .uri(base_url + "/creator/v2.1/data/" + account_owner_name + "/" + app_link_name + "/report/"
                        + prompt_report_link_name)
                .header("Authorization", "Zoho-oauthtoken " + token.getToken())
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

        if (data == null || data.isEmpty()) {
            throw new RuntimeException("Prompt Report returned no records.");
        }

        return data.stream()
                .filter(record -> "Extractly".equals(record.get("Name")))
                .map(record -> (String) record.get("Prompt"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No prompt record found with Name = 'Extractly'."));
    }
}