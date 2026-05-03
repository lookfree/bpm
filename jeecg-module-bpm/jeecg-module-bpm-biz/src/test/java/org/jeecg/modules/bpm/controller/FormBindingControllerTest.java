package org.jeecg.modules.bpm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.bpm.domain.entity.FormBinding;
import org.jeecg.modules.bpm.service.form.FormBindingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FormBindingControllerTest.TestConfig.class)
@AutoConfigureMockMvc
class FormBindingControllerTest {

    @Configuration
    @EnableWebMvc
    @ComponentScan(basePackageClasses = FormBindingController.class,
                   useDefaultFilters = false,
                   includeFilters = @ComponentScan.Filter(
                           type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                           classes = FormBindingController.class))
    static class TestConfig {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @Autowired MockMvc mvc;
    @MockBean FormBindingService service;

    @Test void postBindReturnsId() throws Exception {
        when(service.bind(any(), any(), any())).thenReturn("binding-123");
        mvc.perform(post("/bpm/v1/form-binding")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"defId\":\"d1\",\"formId\":\"f1\",\"purpose\":\"APPLY\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value("binding-123"));
    }

    @Test void getListByDefId() throws Exception {
        FormBinding fb = new FormBinding();
        fb.setId("b1"); fb.setDefId("d1"); fb.setFormId("f1"); fb.setPurpose("APPLY");
        when(service.listByDef("d1")).thenReturn(List.of(fb));
        mvc.perform(get("/bpm/v1/form-binding?defId=d1"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value("b1"));
    }

    @Test void deleteUnbind() throws Exception {
        mvc.perform(delete("/bpm/v1/form-binding/b1"))
           .andExpect(status().isNoContent());
    }
}
