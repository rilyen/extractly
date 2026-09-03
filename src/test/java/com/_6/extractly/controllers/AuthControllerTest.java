// package com._6.extractly.controllers;

// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.when;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;
// import org.springframework.http.MediaType;
// import org.springframework.test.web.servlet.MockMvc;

// import com._6.extractly.repositories.UserRepository;

// @WebMvcTest(AuthController.class)
// class AuthControllerTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @MockitoBean
//     private UserRepository userRepository;

//     @Test
//     void register_withBlankEmail_returnsEmailRequiredError() throws Exception {
//         String body = """
//             {"email": "", "password": "password123"}
//             """;

//         mockMvc.perform(post("/register")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(body))
//             .andExpect(status().isBadRequest())
//             .andExpect(jsonPath("$.email").value("Email is required."));
//     }

//     @Test
//     void register_withInvalidEmailFormat_returnsEmailInvalidError() throws Exception {
//         String body = """
//             {"email": "notanemail", "password": "password123"}
//             """;

//         mockMvc.perform(post("/register")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(body))
//             .andExpect(status().isBadRequest())
//             .andExpect(jsonPath("$.email").value("Email must be valid."));
//     }

//     @Test
//     void register_withBlankPassword_returnsPasswordRequiredError() throws Exception {
//         String body = """
//             {"email": "test@aetherautomation.com", "password": ""}
//             """;

//         mockMvc.perform(post("/register")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(body))
//             .andExpect(status().isBadRequest())
//             .andExpect(jsonPath("$.password").value("Password is required."));
//     }

//     @Test
//     void register_withShortPassword_returnsPasswordLengthError() throws Exception {
//         String body = """
//             {"email": "test@aetherautomation.com", "password": "123"}
//             """;

//         mockMvc.perform(post("/register")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(body))
//             .andExpect(status().isBadRequest())
//             .andExpect(jsonPath("$.password").value("Password must be at least 8 characters."));
//     }

//     @Test
//     void register_withExistingEmail_returnsEmailAlreadyRegisteredError() throws Exception {
//         when(userRepository.existsByEmail(anyString())).thenReturn(true);

//         String body = """
//             {"email": "test@aetherautomation.com", "password": "password123"}
//             """;

//         mockMvc.perform(post("/register")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(body))
//             .andExpect(status().isBadRequest())
//             .andExpect(jsonPath("$.email").value("Email already registered."));
//     }

//     @Test
//     void register_withValidNewUser_returnsSuccessMessage() throws Exception {
//         when(userRepository.existsByEmail(anyString())).thenReturn(false);

//         String body = """
//             {"email": "test@aetherautomation.com", "password": "password123"}
//             """;

//         mockMvc.perform(post("/register")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(body))
//             .andExpect(status().isOk())
//             .andExpect(jsonPath("$.message").value("User registered successfully."));
//     }

//     @Test
//     void login_withBlankEmail_returnsEmailRequiredError() throws Exception {
//         String body = """
//             {"email": "", "password": "password123"}
//             """;

//         mockMvc.perform(post("/login")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(body))
//             .andExpect(status().isBadRequest())
//             .andExpect(jsonPath("$.email").value("Email is required."));
//     }

//     @Test
//     void login_withNonExistentEmail_returnsGenericInvalidCredentialsError() throws Exception {
//         when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.empty());

//         String body = """
//             {"email": "nouser@aetherautomation.com", "password": "password123"}
//             """;

//         mockMvc.perform(post("/login")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .content(body))
//             .andExpect(status().isBadRequest())
//             .andExpect(jsonPath("$.credentials").value("Invalid email or password."));
//     }
// }