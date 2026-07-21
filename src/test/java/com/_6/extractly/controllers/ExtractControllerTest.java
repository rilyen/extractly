package com._6.extractly.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

@WebMvcTest(ExtractController.class)
class ExtractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    void extract_withBlankTranscript_returnsBadRequest() throws Exception {
        String body = """
            {"transcript": ""}
            """;

        mockMvc.perform(post("/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void extract_withMissingTranscriptKey_returnsBadRequest() throws Exception {
        String body = """
            {}
            """;

        mockMvc.perform(post("/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void extract_withValidTranscript_returnsGeminiResponse() throws Exception {
        String fakeGeminiJson = """
            {"candidates": [{"content": {"parts": [{"text": "{\\"Deal_Name\\":\\"TEST_G13_demo\\"}"}]}}]}
            """;

        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(HttpMethod.POST),
                any(),
                org.mockito.ArgumentMatchers.eq(String.class)))
            .thenReturn(ResponseEntity.ok(fakeGeminiJson));

        String body = """
            {"transcript": "This is a meeting about Towels Direct project."}
            """;

        mockMvc.perform(post("/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());
    }

    @Test
    void extract_whenGeminiThrowsException_returnsInternalServerError() throws Exception {
        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(HttpMethod.POST),
                any(),
                org.mockito.ArgumentMatchers.eq(String.class)))
            .thenThrow(new RuntimeException("Gemini API unreachable"));

        String body = """
            {"transcript": "Some transcript text."}
            """;

        mockMvc.perform(post("/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isInternalServerError());
    }
}