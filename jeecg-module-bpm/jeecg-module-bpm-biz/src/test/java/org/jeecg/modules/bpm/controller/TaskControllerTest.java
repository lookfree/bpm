package org.jeecg.modules.bpm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.bpm.service.task.BpmTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TaskControllerTest.TestConfig.class,
        properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
class TaskControllerTest {

    @Configuration
    @EnableWebMvc
    @ComponentScan(
        basePackages = "org.jeecg.modules.bpm.controller",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TaskController.class)
    )
    static class TestConfig {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @Autowired MockMvc mvc;
    @MockBean BpmTaskService taskService;

    @Test void todoReturnsOk() throws Exception {
        when(taskService.listTodo()).thenReturn(Collections.emptyList());
        mvc.perform(get("/bpm/v1/task/todo")).andExpect(status().isOk());
    }

    @Test void completeApproveReturnsOk() throws Exception {
        doNothing().when(taskService).complete(eq("t1"), eq("APPROVE"), any(), any(), any());
        mvc.perform(post("/bpm/v1/task/t1/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"APPROVE\",\"comment\":\"ok\"}"))
           .andExpect(status().isOk());
    }

    @Test void completeUnsupportedActionReturns400() throws Exception {
        doThrow(new IllegalArgumentException("unsupported_action: UNKNOWN"))
                .when(taskService).complete(eq("t1"), eq("UNKNOWN"), any(), any(), any());
        mvc.perform(post("/bpm/v1/task/t1/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"UNKNOWN\"}"))
           .andExpect(status().isBadRequest());
    }
}
