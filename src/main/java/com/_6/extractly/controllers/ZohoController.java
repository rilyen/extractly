package com._6.extractly.controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com._6.extractly.service.AddZohoForm;
import com._6.extractly.service.GetZohoDeals;
import com._6.extractly.service.GetZohoStandardServices;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class ZohoController {
    private final AddZohoForm addZohoForm;
    private final GetZohoDeals getZohoDeals;
    private final GetZohoStandardServices getZohoStandardServices;

    public ZohoController(AddZohoForm addZohoForm, GetZohoDeals getZohoDeals,
            GetZohoStandardServices getZohoStandardServices) {
        this.addZohoForm = addZohoForm;
        this.getZohoDeals = getZohoDeals;
        this.getZohoStandardServices = getZohoStandardServices;
    }

    @PostMapping("/send-to-service")
    public Map<String, Object> sendToZoho(@RequestBody Map<String, Object> extractData) {

        return addZohoForm.sendInfo(extractData);
    }

    @GetMapping("/deals")
    public Map<String, Object> deals() {
        return getZohoDeals.getDeals();
    }

    @GetMapping("/standard-services")
    public Map<String, Object> standardServices() {
        return getZohoStandardServices.getStandardServices();
    }

    @GetMapping("/config-key")
    public Map<String, Object> geminiKey() {
        return getZohoDeals.getGeminiKey();
    }

}