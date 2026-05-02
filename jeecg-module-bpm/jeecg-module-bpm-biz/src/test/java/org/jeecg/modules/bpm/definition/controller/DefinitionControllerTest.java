package org.jeecg.modules.bpm.definition.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.bpm.definition.dto.*;
import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionService;
import org.jeecg.modules.bpm.definition.support.BpmnXmlValidator;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = { DefinitionControllerTest.TestConfig.class })
@AutoConfigureMockMvc
class DefinitionControllerTest {

    @Configuration
    @EnableWebMvc
    @ComponentScan(basePackageClasses = DefinitionController.class)
    public static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockBean BpmProcessDefinitionService service;
    @MockBean BpmnXmlValidator validator;

    @Test
    void postCreate201() throws Exception {
        DefinitionCreateRequest req = new DefinitionCreateRequest();
        req.setDefKey("k"); req.setName("N");
        DefinitionVO vo = new DefinitionVO(); vo.setId("uuid-1"); vo.setName("N");
        when(service.createDraft(any())).thenReturn(vo);
        mvc.perform(post("/bpm/v1/definition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").value("uuid-1"));
    }

    @Test
    void postCreateWithBlankNameReturns400() throws Exception {
        DefinitionCreateRequest req = new DefinitionCreateRequest();
        req.setDefKey("k");
        mvc.perform(post("/bpm/v1/definition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(service.getDetail("x")).thenReturn(null);
        mvc.perform(get("/bpm/v1/definition/x"))
           .andExpect(status().isNotFound());
    }

    @Test
    void putUpdateReturnsVo() throws Exception {
        DefinitionUpdateRequest req = new DefinitionUpdateRequest();
        req.setName("M");
        DefinitionVO vo = new DefinitionVO(); vo.setId("x"); vo.setName("M");
        when(service.update(eq("x"), any())).thenReturn(vo);
        mvc.perform(put("/bpm/v1/definition/x")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name").value("M"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/bpm/v1/definition/x"))
           .andExpect(status().isNoContent());
        verify(service).delete("x");
    }

    @Test
    void invalidBpmnInBodyReturns400() throws Exception {
        doThrow(new BpmnXmlValidator.InvalidBpmnException("bad"))
                .when(validator).validate(anyString());
        DefinitionCreateRequest req = new DefinitionCreateRequest();
        req.setDefKey("k"); req.setName("N"); req.setBpmnXml("<x/>");
        mvc.perform(post("/bpm/v1/definition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isBadRequest());
    }
}
