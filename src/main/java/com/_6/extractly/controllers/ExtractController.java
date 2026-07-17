package com._6.extractly.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.PostMapping;

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
                Read the transcript below and extract every product discussed for the Product Creator form described here.
                A single transcript can describe MULTIPLE products. Create one product object per distinct
                product or deliverable discussed.
                Return ONLY a valid JSON object, no markdown, no explanation, no code fences.
                If a field is not mentioned in the transcript, set its value to null.
                For boolean fields (checkboxes), use true or false.
                For date fields, use DD-MMM-YYYY format (example: 15-Mar-2026).
                For hours fields, return a plain number with no units.
                Do not invent values that are not supported by the transcript.
 
                The top-level JSON object must have EXACTLY this structure:
 
                {
                  "Products": [ one product object per product discussed, in the order discussed ]
                }
 
                Each product object must have EXACTLY this structure and these keys:
 
                {
                  "Project": string or null,
                  "Deal_ID": number or null,
                  "Deal_Name_Account_Contact": string or null,
                  "Product_Name": string or null,
                  "Product_Description": string or null,
                  "Delivery_Rate": string,
                  "Service_Types": ["Custom Functions"],
                  "Automation_Triggers": array of trigger objects (see below),
                  "Standard_Function_Outputs": array of output objects (see below),
                  "Total_Product_Cost_Hours": number or null,
                  "Estimated_Duration_to_Implement_Days": number or null,
                  "Reviewer_Approver": string or null,
                  "Latest_Review_Date": string or null,
                  "Passed_IAT": boolean,
                  "General_Comments": string or null,
                  "Task_ID": string or null,
                  "User_Story_Created": boolean
                }
 
                Each object in "Automation_Triggers" represents one IF row and must have exactly these keys:
                {
                  "Trigger_Number": number (sequential, starting at 1),
                  "Name_of_Trigger_Application": string or null,
                  "Is_the_application_a_Zoho_App": "Yes" or "No" or null,
                  "Trigger_Event_Description": string or null (describe the trigger event in detail),
                  "Filters": string or null (multiple filters separated with ';'),
                  "Trigger_Assumptions": string or null,
                  "Estimated_Hours": number or null
                }
 
                Each object in "Standard_Function_Outputs" represents one THEN row and must have exactly these keys:
                {
                  "Select_Trigger": number or null (the Trigger_Number of the IF this THEN belongs to),
                  "Standard_Services": string or null,
                  "Inclusions": string or null,
                  "Exclusions": string or null,
                  "Detailed_Description": string or null,
                  "Estimated_Hours": number or null
                }
 
                Special field rules:
                - "Product_Description": ALWAYS generate this field when the transcript describes any work
                  to be delivered. Summarize what Aether will build for the client in your own words, phrased like
                  "Aether will create within the Client's Zoho CRM Application Workflows and custom functions that: ..."
                  Only use null if the transcript contains no deliverables at all.
                - "Delivery_Rate": use "Normal" unless the transcript clearly states a different delivery rate.
                - "Service_Types": always output exactly ["Custom Functions"] for every product,
                  regardless of what the transcript says.
 
                Rules for identifying Automation_Triggers and Standard_Function_Outputs:
                - Triggers are almost never stated with literal "IF/THEN" wording. Treat ANY
                  event-then-action pattern in the transcript as a trigger and output pair.
                  Phrases like "when...", "whenever...", "once...", "after...", "as soon as...",
                  "every time...", "on submission...", "at the end of the month..." all signal triggers.
                - The EVENT part becomes one Automation_Triggers row.
                  Example: "when a customer places an order" becomes a trigger with
                  Trigger_Event_Description "A customer places an order".
                - The ACTION part becomes one Standard_Function_Outputs row linked to that trigger
                  via "Select_Trigger".
                  Example: "...send a confirmation email" becomes an output with
                  Detailed_Description "Send a confirmation email to the customer".
                - One trigger can have multiple outputs. Create one output row per distinct action.
                - Conditions restricting the event ("only for orders over $500") belong in "Filters".
                - Unstated things you must presume for the automation to work belong in "Trigger_Assumptions".
                - Number triggers sequentially starting at 1 WITHIN each product. Trigger numbering
                  restarts for every product, and "Select_Trigger" always refers to a Trigger_Number
                  in the SAME product.
                - Only use empty arrays if the transcript truly contains no event-then-action
                  behaviour anywhere for that product.
 
                Rules for multiple products:
                - Fields discussed once but applying to the whole engagement (example: Deal_ID,
                  Deal_Name_Account_Contact, Project) should be repeated in every product object.
                - Fields discussed per product (example: Product_Name, hours, delivery rate,
                  reviewer, triggers) belong only to the product they were discussed for.
                - Do not merge triggers, outputs, hours, or comments from different products together.
 
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
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }

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
                For date fields, use YYYY-MM-DD format if possible.

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
