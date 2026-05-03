package org.jeecg.modules.bpm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.bpm.service.instance.InstanceService;
import org.jeecg.modules.bpm.service.instance.StartResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = InstanceControllerTest.TestConfig.class,
        properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
class InstanceControllerTest {

    @Configuration
    @EnableWebMvc
    @ComponentScan(
        basePackages = "org.jeecg.modules.bpm.controller",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = InstanceController.class)
    )
    static class TestConfig {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @Autowired MockMvc mvc;
    @MockBean InstanceService service;

    @Test void postStartReturnsResponse() throws Exception {
        StartResponse resp = new StartResponse();
        resp.setInstanceId("i1");
        resp.setActInstId("act1");
        resp.setBusinessKey("bk1");
        when(service.start(any())).thenReturn(resp);

        mvc.perform(post("/bpm/v1/instance")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"defId\":\"d1\",\"formId\":\"F1\",\"formData\":{}}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.instanceId").value("i1"))
           .andExpect(jsonPath("$.actInstId").value("act1"));
    }

    @Test void getByIdReturns404WhenNotFound() throws Exception {
        when(service.getById("missing")).thenReturn(null);
        mvc.perform(get("/bpm/v1/instance/missing"))
           .andExpect(status().isNotFound());
    }
}
