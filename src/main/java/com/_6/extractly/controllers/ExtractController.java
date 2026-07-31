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

  // redirect or show page based on user role
  @GetMapping("/")
  public String index(HttpSession session) {
    String role = (String) session.getAttribute("role");
    // Boolean verified = (Boolean) session.getAttribute("verified");

    if (role == null) {
      return "redirect:/login.html";
    }
    // if (verified == null || !verified) {
    // return "display-view-only";
    // }

    return "ADMIN".equals(role) ? "extract" : "display-view-only"; // CHANGED SOMETHING HERE FOR TESTING display ->
                                                                   // extract
  }

  // Handles POST /extract
  // JS calls this with a recorded meeting transcript or meeting video url
  // Sends the transcript to Gemini with extraction instructions
  // Returns Gemini's raw JSON response to the frontend to parse
  // @ResponseBody: write the return value directly as the HTTP response body
  @PostMapping(value = "/extract")
  @ResponseBody
  public ResponseEntity<?> extract(
      @RequestParam(value = "videoUrl", required = false) String videoUrl,
      @RequestParam(value = "transcript", required = false) String transcript) {

    try {
      // ROUTE 1: Frontend sent a video URL
      if (videoUrl != null && !videoUrl.isEmpty()) {
        String transcribedText = transcribeFromUrl(videoUrl);
        ;
        System.out.println("=== TRANSCRIPT ===\n" + transcribedText);

        if (transcribedText == null || transcribedText.isBlank()) {
          return ResponseEntity.badRequest().body("{\"error\":\"Transcript is empty.\"}");
        }
        // return just the text so frontend can place it in the text box
        return ResponseEntity.ok().body(Map.of("transcript", transcribedText));
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

            Each product object must have EXACTLY this structure and these keys:

            {

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

              "Latest_Review_Date": string or null,
              "Passed_IAT": boolean,
              "General_Comments": string or null,

              "User_Story_Created": boolean or null
            }

            Each object in "Automation_Triggers" represents one IF row and must have exactly these keys:
            {
              "Trigger_Number": number (sequential, starting at 1),
              "Name_of_Trigger_Application": string or null,
              "Is_the_application_a_Zoho_App": "Yes" or "No" or null,
              "Trigger_Event_Description": string or null (describe the trigger event in detail),
              "Filters": string or null (multiple filters separated with ';'),
              "Trigger_Assumptions": string or null,
              "Hours": number or null
            }

            Each object in "Standard_Function_Outputs" represents one THEN row and must have exactly these keys:
            {
              "Inclusions": string or null,
              "Exclusions": string or null,
              "Detailed_Description": string or null,
              "Estimated_Hours": number or null
            }

            Special field rules:
            - "Product_Description1": ALWAYS generate this field when the transcript describes any work
              to be delivered. Summarize what Aether will build for the client in your own words, phrased like
              "Aether will create within the Client's Zoho CRM Application Workflows and custom functions that: ..."
              Only use null if the transcript contains no deliverables at all.

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
      }

      // ROUTE 3: No video url or transcript was provided
      return ResponseEntity.badRequest().body("{\"error\":\"Must provide either a file or a transcript.\"}");

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    }
  }

  private String transcribeFromUrl(String videoUrl) throws InterruptedException {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", assemblyAiApiKey);
    headers.setContentType(MediaType.APPLICATION_JSON);

    // submit the Cloudinary URL directly to AssemblyAI
    Map<String, Object> payload = Map.of("audio_url", videoUrl);
    HttpEntity<Map<String, Object>> submitEntity = new HttpEntity<>(payload, headers);
    Map submitResponse = restTemplate.postForObject(
        "https://api.assemblyai.com/v2/transcript", submitEntity, Map.class);
    String id = (String) submitResponse.get("id");

    // check the API status continuously until the transcription process finishes
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

  @GetMapping("/api/assembly-key")
  @ResponseBody
  public ResponseEntity<?> getAssemblyKey() {
    // Returns your key as a simple JSON object: {"apiKey": "your-key-here"}
    return ResponseEntity.ok(Map.of("apiKey", assemblyAiApiKey));
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