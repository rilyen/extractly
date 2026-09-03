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
  // @Value("${gemini.api.key}")
  // private String geminiApiKey;

  // NOTE: Nothing is stored in .env, or a database
  // Every logged-in user must supply their own Gemini API Key, which is stored in
  // their HttpSession
  // API stays available for every /extract call made during the same login
  // session and
  // disappears when the user logs out or explicityl clears it
  private static final String SESSION_GEMINI_KEY_ATTR = "geminiApiKey";

  // API key for AssemblyAI
  // @Value("${assemblyai.api.key}")
  // private String assemblyAiApiKey;

  // NOTE: same approach as geminiApiKey above
  private static final String SESSION_ASSEMBLY_KEY_ATTR = "assemblyAiApiKey";

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
  public String index() {
    // String role = (String) session.getAttribute("role");
    // Boolean verified = (Boolean) session.getAttribute("verified");

    // if (role == null) {
    //   return "redirect:/login.html";
    // }
    // if (verified == null || !verified) {
    // return "display-view-only";
    // }

    // return "ADMIN".equals(role) ? "extract" : "display-view-only"; // CHANGED SOMETHING HERE FOR TESTING display ->
                                                                   // extract
    return "extract";
  }

  // Handles POST /gemini-key
  // Frontend calls this once the user has typed/pasted their Gemini API key
  // Stores it in the logged-in user's HttpSession
  // It stays there for the rest of this login session and is used automatically
  // by /extract and /extract-from-video
  // so the frontend does not need to resend it with every call
  @PostMapping("/gemini-key")
  @ResponseBody
  public ResponseEntity<String> saveGeminiKey(@RequestBody Map<String, String> body, HttpSession session) {

    String geminiApiKey = body.get("geminiApiKey");
    if (geminiApiKey == null || geminiApiKey.isBlank()) {
      return ResponseEntity.badRequest().body("{\"error\":\"Gemini API key is required.\"}");
    }

    session.setAttribute(SESSION_GEMINI_KEY_ATTR, geminiApiKey);
    return ResponseEntity.ok("{\"message\":\"Gemini API key saved for this session.\"}");
  }

  // Handles POST /assembly-key
  @PostMapping("/assembly-key")
  @ResponseBody
  public ResponseEntity<String> saveAssemblyKey(@RequestBody Map<String, String> body, HttpSession session) {

    String assemblyAiApiKey = body.get("assemblyAiApiKey");
    if (assemblyAiApiKey == null || assemblyAiApiKey.isBlank()) {
      return ResponseEntity.badRequest().body("{\"error\":\"AssemblyAI API key is required.\"}");
    }
    session.setAttribute(SESSION_ASSEMBLY_KEY_ATTR, assemblyAiApiKey);
    return ResponseEntity.ok("{\"message\":\"AssemblyAI API key saved for this session.\"}");
  }

  // Handles Post/gemini-key/clear
  // Frontend class this when the user clicks "Clear key". Removes just the
  // Gemini key attribute from the session (the rest of the login session is
  // untouched
  // e.g. email or role attributes are untouched), so this does not log the user
  // out
  @PostMapping("/gemini-key/clear")
  @ResponseBody
  public ResponseEntity<String> clearGeminiKey(HttpSession session) {
    session.removeAttribute(SESSION_GEMINI_KEY_ATTR);
    return ResponseEntity.ok("{\"message\":\"Gemini API key cleared.\"}");
  }

  // Handles POST /assembly-key/clear
  @PostMapping("/assembly-key/clear")
  @ResponseBody
  public ResponseEntity<String> clearAssemblyKey(HttpSession session) {
    session.removeAttribute(SESSION_ASSEMBLY_KEY_ATTR);
    return ResponseEntity.ok("{\"message\":\"AssemblyAI API key cleared.\"}");
  }

  // Handles GET /gemini-key/status
  // lets the frontend show "a key is currently saved" / "no key saved" without
  // exposing the key's actual value
  @GetMapping("/gemini-key/status")
  @ResponseBody
  public ResponseEntity<String> geminiKeyStatus(HttpSession session) {
    boolean hasKey = session.getAttribute(SESSION_GEMINI_KEY_ATTR) != null;
    return ResponseEntity.ok("{\"hasKey\": " + hasKey + "}");
  }

  // Handles GET /assembly-key/status
  @GetMapping("/assembly-key/status")
  @ResponseBody
  public ResponseEntity<String> assemblyKeyStatus(HttpSession session) {
    boolean hasKey = session.getAttribute(SESSION_ASSEMBLY_KEY_ATTR) != null;
    return ResponseEntity.ok("{\"hasKey\": " + hasKey + "}");
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
      @RequestParam(value = "transcript", required = false) String transcript,
      HttpSession session) {

    // Every user must have saved their own Gemini API key to this seession first
    // The key lives only in this HttpSession (never persisted or logged
    // server-side)
    String geminiApiKey = (String) session.getAttribute(SESSION_GEMINI_KEY_ATTR);
    if (geminiApiKey == null || geminiApiKey.isBlank()) {
      return ResponseEntity.badRequest().body("{\"error\":\"Gemini API key is required.\"}");
    }

    try {
      // ROUTE 1: Frontend sent a video URL
      if (videoUrl != null && !videoUrl.isEmpty()) {

        String assemblyAiApiKey = (String) session.getAttribute(SESSION_ASSEMBLY_KEY_ATTR);
        if (assemblyAiApiKey == null || assemblyAiApiKey.isBlank()) {
          return ResponseEntity.badRequest().body("{\"error\":\"AssemblyAI API key is required.\"}");
        }

        String transcribedText = transcribeFromUrl(videoUrl, assemblyAiApiKey);
        ;

        if (transcribedText == null || transcribedText.isBlank()) {
          return ResponseEntity.badRequest().body("{\"error\":\"Transcript is empty.\"}");
        }
        // return just the text so frontend can place it in the text box
        return ResponseEntity.ok().body(Map.of("transcript", transcribedText));
      }

      // ROUTE 2: Frontend sent text
      if (transcript != null && !transcript.isBlank()) {
        String prompt = zohoPromptService.getPrompt() + "\n\nTranscript:\n" + transcript;

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

  private String transcribeFromUrl(String videoUrl, String assemblyAiApiKey) throws InterruptedException {
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
  public ResponseEntity<?> getAssemblyKey(HttpSession session) {

    String assemblyAiApiKey = (String) session.getAttribute(SESSION_ASSEMBLY_KEY_ATTR);
    if (assemblyAiApiKey == null || assemblyAiApiKey.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "AssemblyAI API key is required."));
    }

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