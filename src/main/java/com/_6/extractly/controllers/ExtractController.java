package com._6.extractly.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com._6.extractly.service.ZohoPromptService;

import jakarta.servlet.http.HttpSession;
import tools.jackson.databind.json.JsonMapper;

@Controller
public class ExtractController {

    // API key for Gemini
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // API key for AssemblyAI
    @Value("${assemblyai.api.key}")
    private String assemblyAiApiKey;

    // use to send HTTP request to Gemini and AssemblyAI's REST API
    private final RestTemplate restTemplate;

    // use to convert into a valid JSON string
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    // use to get the prompt from Zoho
    private final ZohoPromptService zohoPromptService;

    public ExtractController(RestTemplate restTemplate, ZohoPromptService zohoPromptService) {
        this.restTemplate = restTemplate;
        this.zohoPromptService = zohoPromptService;
    }

    // redirect or show page based on user role
    @GetMapping("/")
    public String index(HttpSession session) {
        String role = (String) session.getAttribute("role");
        // Boolean verified = (Boolean) session.getAttribute("verified");

        if (role == null) {
            return "redirect:/login.html";
        }
        // if (verified == null || !verified) {
        //     return "display-view-only";
        // }

        return "ADMIN".equals(role) ? "extract" : "display-view-only"; // CHANGED SOMETHING HERE FOR TESTING display ->
                                                                       // extract
    }

    // Handles POST /extract
    // JS calls this with a recorded meeting transcript
    // Sends the transcript to Gemini with extraction instructions
    // Returns Gemini's raw JSON response to the frontend to parse
    // @ResponseBody: write the return value directly as the HTTP response body
    @PostMapping("/extract")
    @ResponseBody
    public ResponseEntity<String> extract(@RequestBody Map<String, String> body) {

        // Pull transcript
        String transcript = body.get("transcript");

        // Reject empty/missing transcripts
        if (transcript == null || transcript.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\":\"Transcript is empty.\"}");
        }

        // Instructions given to Gemini (from Zoho prompt service) with transcript text appended at the end
        String prompt = zohoPromptService.getPrompt() + "\n\nTranscript:\n" + transcript;

        try {
            // Build Gemini request payload as a plain java object graph that mirrors the
            // JSON shape Gemini's API expects:
            // { "contents": [ { "parts": [ { "text": "..." } ] } ],
            // "generationConfig": { "temperature": 0.1 } }
            //
            // We build it this way so jsonMapper handles translating this structure into
            // valid JSON text for us
            // (no matter what characters end up inside "prompt")
            //
            // Low temperature is more deterministic, since we want consistent output
            // responseMimeType "application/json" forces Gemini into strict JSON mode
            Map<String, Object> requestPayload = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)))),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "responseMimeType", "application/json"));

            // Serialize object graph into valid JSON string to send as HTTP request body
            String requestBody = jsonMapper.writeValueAsString(requestPayload);

            // Tell Gemini outgoing request body is JSON
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Gemini REST endpoint with our API key, using gemini 2.5 flash model
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                    + geminiApiKey;

            // Send POST request to Gemini with JSON body + header
            // response is String
            ResponseEntity<String> geminiResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class);

            // send Gemini's raw JSON response back to frontend JS (to be parsed into
            // fields)
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(geminiResponse.getBody());

        } catch (Exception e) {
            // Return error message for failures
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // Handles POST /extract-from-video
    // JS calls this with an uploaded mp4 file
    // Transcribes the audio via AssemblyAI, then sends the resulting transcript
    // to Gemini with the same extraction instructions as /extract
    // Returns Gemini's raw JSON response to the frontend to parse
    @PostMapping("/extract-from-video")
    @ResponseBody
    public ResponseEntity<String> extractFromVideo(@RequestParam("file") MultipartFile file) {

        String transcript;
        try {
            transcript = transcribe(file.getBytes());
            System.out.println("=== TRANSCRIPT ===\n" + transcript);
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }

        if (transcript == null || transcript.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\":\"Transcript is empty.\"}");
        }

        // Instructions given to Gemini (from Zoho prompt service) with transcript text appended at the end
        String prompt = zohoPromptService.getPrompt() + "\n\nTranscript:\n" + transcript;

        try {
            Map<String, Object> requestPayload = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)))),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "responseMimeType", "application/json"));

            String requestBody = jsonMapper.writeValueAsString(requestPayload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                    + geminiApiKey;

            ResponseEntity<String> geminiResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(geminiResponse.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private String transcribe(byte[] fileBytes) throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", assemblyAiApiKey);

        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<byte[]> uploadEntity = new HttpEntity<>(fileBytes, headers);
        Map uploadResponse = restTemplate.postForObject(
                "https://api.assemblyai.com/v2/upload", uploadEntity, Map.class);
        String audioUrl = (String) uploadResponse.get("upload_url");

        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = Map.of("audio_url", audioUrl);
        HttpEntity<Map<String, Object>> submitEntity = new HttpEntity<>(payload, headers);
        Map submitResponse = restTemplate.postForObject(
                "https://api.assemblyai.com/v2/transcript", submitEntity, Map.class);
        String id = (String) submitResponse.get("id");

        HttpEntity<Void> pollEntity = new HttpEntity<>(headers);
        while (true) {
            Map pollResponse = restTemplate.exchange(
                    "https://api.assemblyai.com/v2/transcript/" + id,
                    HttpMethod.GET, pollEntity, Map.class).getBody();

            String status = (String) pollResponse.get("status");
            if ("completed".equals(status)) {
                return (String) pollResponse.get("text");
            }

            if ("error".equals(status)) {
                throw new RuntimeException("Transcription failed: " + pollResponse.get("error"));
            }
            Thread.sleep(2000);
        }
    }
}
/*
 * //Will need it later Each object in "Custom_Function_Outputs" represents one
 * THEN row and must have exactly these keys:
 * {
 * "Standard_Function_Request" : true or false or null,
 * skip this -> "Select_Trigger": number or null (the Trigger_Number of the IF
 * this THEN belongs to),
 * "Output_Application": "Zoho CRM,
 * "Output_Description": string or null,
 * "Output_Inclusions": string or null,
 * "Output_Exclusions": string or null,
 * "Estimated_Hours": number or null
 * }
 */
