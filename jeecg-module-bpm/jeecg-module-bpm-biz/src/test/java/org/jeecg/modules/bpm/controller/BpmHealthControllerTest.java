package org.jeecg.modules.bpm.controller;

import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = { BpmHealthControllerTest.TestConfig.class })
@AutoConfigureMockMvc
class BpmHealthControllerTest {

    @Configuration
    @EnableWebMvc
    @ComponentScan("org.jeecg.modules.bpm.controller")
    public static class TestConfig {
    }

    @Autowired MockMvc mvc;

    @MockBean ProcessEngine processEngine;

    @Test
    void healthzReturnsUp() throws Exception {
        Mockito.when(processEngine.getName()).thenReturn("default");
        mvc.perform(get("/bpm/v1/healthz"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("UP"))
           .andExpect(jsonPath("$.engine").value("flowable"))
           .andExpect(jsonPath("$.version").exists())
           .andExpect(jsonPath("$.name").value("default"));
    }
}
