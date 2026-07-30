package com._6.extractly.controllers;

import org.springframework.web.bind.annotation.RestController;

import com._6.extractly.service.AddZohoForm;
import com._6.extractly.service.GetZohoDeals;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class ZohoController {
    private final AddZohoForm addZohoForm;
    private final GetZohoDeals getZohoDeals;

    public ZohoController(AddZohoForm addZohoForm, GetZohoDeals getZohoDeals) {
        this.addZohoForm = addZohoForm;
        this.getZohoDeals = getZohoDeals;
    }

    @PostMapping("/send-to-service")
    public Map<String, Object> sendToZoho(@RequestBody Map<String, Object> extractData) {

        return addZohoForm.sendInfo(extractData);
    }

    @GetMapping("/deals")
    public Map<String, Object> deals() {
        return getZohoDeals.getDeals();
    }

}
