package io.hexlet.project_devops_deploy.controller;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.hexlet.project_devops_deploy.dto.FileUploadResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void testUploadAndView() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "test-image.png",
                        MediaType.IMAGE_PNG_VALUE,
                        "fake-image-content".getBytes());

        var uploadResult =
                mockMvc.perform(multipart("/api/files/upload").file(file))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.key", not(blankOrNullString())))
                        .andReturn();

        FileUploadResponse response =
                objectMapper.readValue(
                        uploadResult.getResponse().getContentAsString(), FileUploadResponse.class);

        mockMvc.perform(get("/api/files/view").param("key", response.getKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key", equalTo(response.getKey())))
                .andExpect(jsonPath("$.url", not(blankOrNullString())));
    }
}
