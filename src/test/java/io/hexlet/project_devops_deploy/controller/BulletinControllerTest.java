package io.hexlet.project_devops_deploy.controller;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.hexlet.project_devops_deploy.dto.BulletinRequest;
import io.hexlet.project_devops_deploy.model.bulletin.BulletinState;
import io.hexlet.project_devops_deploy.repository.BulletinRepository;
import io.hexlet.project_devops_deploy.util.ModelGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class BulletinControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private ModelGenerator modelGenerator;

    @Autowired private BulletinRepository bulletinRepository;

    @BeforeEach
    void setup() {
        bulletinRepository.deleteAll();
    }

    @Test
    void testCreate() throws Exception {
        BigDecimal price = BigDecimal.valueOf(2500);
        String imageKey = "bulletins/create.png";
        BulletinRequest request =
                buildRequest(
                        "Create title",
                        "Create description",
                        BulletinState.DRAFT,
                        "create@example.com",
                        price,
                        imageKey);

        mockMvc.perform(
                        post("/api/bulletins")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Create title")))
                .andExpect(jsonPath("$.price", equalTo(price.intValue())))
                .andExpect(jsonPath("$.imageKey", equalTo(imageKey)));

        mockMvc.perform(get("/api/bulletins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.total", equalTo(1)));
    }

    @Test
    void testIndex() throws Exception {
        bulletinRepository.save(modelGenerator.generateBulletin());
        bulletinRepository.save(modelGenerator.generateBulletin());

        mockMvc.perform(get("/api/bulletins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.total", equalTo(2)));
    }

    @Test
    void testShow() throws Exception {
        var bulletin = bulletinRepository.save(modelGenerator.generateBulletin());

        mockMvc.perform(get("/api/bulletins/" + bulletin.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(bulletin.getId().intValue())))
                .andExpect(jsonPath("$.title", equalTo(bulletin.getTitle())))
                .andExpect(jsonPath("$.price", closeTo(bulletin.getPrice().doubleValue(), 0.001)))
                .andExpect(jsonPath("$.imageKey", equalTo(bulletin.getImageKey())));
    }

    @Test
    void testUpdate() throws Exception {
        var bulletin = bulletinRepository.save(modelGenerator.generateBulletin());

        BigDecimal updatedPrice = BigDecimal.valueOf(9999);
        String imageKey = "bulletins/updated.png";
        BulletinRequest request =
                buildRequest(
                        "Updated title",
                        "Updated description",
                        BulletinState.PUBLISHED,
                        "updated@example.com",
                        updatedPrice,
                        imageKey);

        mockMvc.perform(
                        put("/api/bulletins/" + bulletin.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo("Updated title")))
                .andExpect(jsonPath("$.state", equalTo("PUBLISHED")))
                .andExpect(jsonPath("$.price", equalTo(updatedPrice.intValue())))
                .andExpect(jsonPath("$.imageKey", equalTo(imageKey)));
    }

    @Test
    void testDelete() throws Exception {
        var bulletin = bulletinRepository.save(modelGenerator.generateBulletin());

        mockMvc.perform(delete("/api/bulletins/" + bulletin.getId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/bulletins/" + bulletin.getId())).andExpect(status().isNotFound());
    }

    private BulletinRequest buildRequest(
            String title,
            String description,
            BulletinState state,
            String contact,
            BigDecimal price,
            String imageKey) {
        return BulletinRequest.builder()
                .title(title)
                .description(description)
                .state(state)
                .contact(contact)
                .price(price)
                .imageKey(imageKey)
                .build();
    }
}
