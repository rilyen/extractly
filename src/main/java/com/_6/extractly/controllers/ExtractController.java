package com._6.extractly.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.PostMapping;

import tools.jackson.databind.json.JsonMapper;

@Controller
public class ExtractController {

    // API key for Gemini
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // use to send HTTP request to Gemini's REST API
    private final RestTemplate restTemplate = new RestTemplate();

    // use to convert into a valid JSON string
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    // redirect or show page based on user role
    @GetMapping("/")
    public String index(HttpSession session) {
        String role = (String) session.getAttribute("role");

        if (role == null) {
            return "redirect:/login.html";
        }

        return "ADMIN".equals(role) ? "display" : "display-view-only";
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

        // Instructions given to Gemini with transcript text appended at the end
        String prompt = """
                You are a data extraction assistant.
                Read the transcript below and extract values for each of the following fields.
                Return ONLY a valid JSON object — no markdown, no explanation, no code fences.
                If a field is not mentioned in the transcript, set its value to null.
                For boolean fields (checkboxes / yes-no), use true or false.
                For date fields, use DD-MMM-YYYY format if possible.
                For Deal_Name name it specifically starting with TEST_G13_(random word here).
                For hours do not need unit hours in json.

                Special field rules:
                - "Integration": extract the company or deal name mentioned in the transcript as a plain string (e.g. "Towels Direct"). Do not invent a value.
                - "Stage": must be exactly one of these options or null: "Internal Testing", "Choice 2", "Choice 3", "Won"
                - "projectClass": must be exactly one of these options or null: "Small (one week)", "Medium (multi week)", "Large (over X weeks)"

                Fields to extract:
                - Integration
                - Assigned_Designer
                - Deal_Name
                - Resources_to_be_used_to_design_and_links
                - ProjectID
                - Design_Link
                - Company_website
                - What_is_the_company_about_for_context
                - What_is_the_purpose_of_this_product"
                - Project_Cost_Modifier
                - Stage
                - Resources_to_be_used_to_design_and_links1
                - Review_Project_Docs_and_Requirements
                - Draft_Roadmap
                - Customer_Feedback_meeting1
                - Update_Roadmap
                - Create_Update_Userstories1
                - Internal_Review_scope_time_budget
                - Submitted_to_Customer_for_Approval
                - Allocated_Budget
                - Used_Hours
                - Project_Class
                - Production_Notes
                - Customer_Concerns
                - On_track_with_IATs
                - On_track_with_Emails_Videos
                - FLAG_as_Problem
                - Dev_Start_Date
                - Design_Due_Date
                - Design_Completion_Date
                - Closing_Date
                - Projected_Delivery_Date1
                - Negotiated_Delivery_Due_Date
                - Projected_Completion_Date1
                - SOW_Estimated_Time
                - Dev_Estimated_Time
                - QC_Turnaround_Time
                - Full_Project_IAT
                - QC_Testing
                - Demo_Video_Recorded
                - Edit_Package_Video
                - Clean_up_database_prep_for_delivery

                Transcript:
                """
                + transcript;

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
}
