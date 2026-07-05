package com._6.extractly.controllers;

import org.springframework.web.bind.annotation.RestController;

import com._6.extractly.service.AddZohoForm;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class SendToZohoService {
    private final AddZohoForm addZohoForm;

    public SendToZohoService(AddZohoForm addZohoForm) {
        this.addZohoForm = addZohoForm;
    }

    @PostMapping("/send-to-service")
    public Map<String, Object> postMethodName(@RequestBody Map<String, Object> extractData) {
        // TODO: process POST request

        return addZohoForm.sendInfo(extractData);
    }

}
