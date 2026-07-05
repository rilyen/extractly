package com._6.extractly.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AddZohoForm {
    private final RestClient restClient = RestClient.create();
    private final AccessTokenZoho token;

    public AddZohoForm(AccessTokenZoho token) {
        this.token = token;
    }

    public void sendInfo() {

    }
}
