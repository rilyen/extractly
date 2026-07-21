package com._6.extractly.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;

// live test: makes a real network call to Gemini API
// requires .env to be fully configured
// @Tag("live") lets us exclude this from normal runs, e.g. -DexcludedGroups=live
@Tag("live")
@SpringBootTest
@AutoConfigureMockMvc
class ExtractGeminiApiTest {
    
    @Autowired
    private MockMvc mockMvc;

    // performs a real POST /extract request through the app to Gemini
    @Test
    void extract_withGeminiApi_returnsWellFormedResponse() throws Exception {
        MvcResult result = mockMvc.perform(post("/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"transcript": "Client meeting for Towels Direct. \
                    Deal name should be TEST_G13_smoketest. \
                    Project is small, about one week of work. \
                    Stage is Internal Testing."}
                    """))
            // confirms request reached Gemini, Gemini replied, and controller passed 200, isOk(), back
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();

        // confirm we got a real, non-empty, well-form JSON response back from Gemini API
        // don't assert on exact values - output is not deterministic
        // contains("candidates") checks it is shaped like a real Gemini response
        assertThat(body).isNotBlank();
        assertThat(body).contains("candidates");
    }
}
