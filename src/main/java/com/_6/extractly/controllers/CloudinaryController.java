package com._6.extractly.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudinary.Cloudinary;

@RestController
@RequestMapping("/api/upload-signature")
public class CloudinaryController {

    // initialize Cloudinary
    @Value("${cloudinary.url}")
    private String cloudinaryUrl;

    @GetMapping
    public Map<String, Object> getSignature() {
    Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
        
        // generate a Unix timestamp
        long timestamp = System.currentTimeMillis() / 1000L;

        // define the parameters to sign
        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);

        String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);

        // return data to the frontend
        Map<String, Object> response = new HashMap<>();
        response.put("signature", signature);
        response.put("timestamp", timestamp);
        response.put("apiKey", cloudinary.config.apiKey);
        response.put("cloudName", cloudinary.config.cloudName);

        return response;
    }
}
