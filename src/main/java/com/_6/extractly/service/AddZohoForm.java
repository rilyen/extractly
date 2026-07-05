package com._6.extractly.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AddZohoForm {
    private final RestClient restClient = RestClient.create();
    private final AccessTokenZoho token;

    public AddZohoForm(AccessTokenZoho token) {
        this.token = token;
    }

    public Map<String, Object> sendInfo(Map<String, Object> extractData) {
        return extractData;
    }
}
