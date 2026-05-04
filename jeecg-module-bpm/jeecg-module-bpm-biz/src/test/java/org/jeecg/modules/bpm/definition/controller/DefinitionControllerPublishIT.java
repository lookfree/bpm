package org.jeecg.modules.bpm.definition.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.bpm.definition.dto.DefinitionCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = { org.jeecg.modules.bpm.test.BpmTestApplication.class })
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class DefinitionControllerPublishIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.27")
            .withDatabaseName("bpm_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        r.add("bpm.schema.auto-init", () -> "true");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String loadXml() throws Exception {
        try (InputStream in = new ClassPathResource("bpm/valid-definition.bpmn20.xml").getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    @Test
    void createDraftThenPublishThenListVersions() throws Exception {
        DefinitionCreateRequest req = new DefinitionCreateRequest();
        req.setDefKey("demo"); req.setName("Demo"); req.setBpmnXml(loadXml());
        MvcResult created = mvc.perform(post("/bpm/v1/definition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = om.readTree(created.getResponse().getContentAsString());
        String id = body.get("id").asText();
        assertThat(body.get("state").asText()).isEqualTo("DRAFT");

        // Two-step publish: DRAFT → TESTING → PUBLISHED
        mvc.perform(post("/bpm/v1/definition/" + id + "/publish")
                .param("changeNote", "first publish"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.state").value("TESTING"));

        mvc.perform(post("/bpm/v1/definition/" + id + "/publish")
                .param("changeNote", "first publish"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.state").value("PUBLISHED"));

        mvc.perform(get("/bpm/v1/definition/" + id + "/versions"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(1))
           .andExpect(jsonPath("$[0].version").value(1))
           .andExpect(jsonPath("$[0].changeNote").value("first publish"));
    }

    @Test
    void publishingNonDraftReturns409() throws Exception {
        DefinitionCreateRequest req = new DefinitionCreateRequest();
        req.setDefKey("demo2"); req.setName("Demo2"); req.setBpmnXml(loadXml());
        MvcResult res = mvc.perform(post("/bpm/v1/definition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        String id = om.readTree(res.getResponse().getContentAsString()).get("id").asText();
        // Two-step publish: DRAFT → TESTING → PUBLISHED
        mvc.perform(post("/bpm/v1/definition/" + id + "/publish")).andExpect(status().isOk());
        mvc.perform(post("/bpm/v1/definition/" + id + "/publish")).andExpect(status().isOk());
        // Now PUBLISHED — further publish must be rejected
        mvc.perform(post("/bpm/v1/definition/" + id + "/publish"))
           .andExpect(status().isConflict());
    }
}
