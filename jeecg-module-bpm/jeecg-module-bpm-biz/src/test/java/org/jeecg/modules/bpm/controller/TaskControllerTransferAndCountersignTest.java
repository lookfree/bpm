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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TaskControllerTransferAndCountersignTest.TestConfig.class,
        properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
class TaskControllerTransferAndCountersignTest {

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

    @Test void transferReturnsOk() throws Exception {
        doNothing().when(taskService).complete(eq("task_1"), eq("TRANSFER"), any(), eq("u_b"), any());
        mvc.perform(post("/bpm/v1/task/task_1/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"TRANSFER\",\"targetUserId\":\"u_b\",\"comment\":\"to B\"}"))
           .andExpect(status().isOk());
        verify(taskService).complete(eq("task_1"), eq("TRANSFER"), any(), eq("u_b"), any());
    }

    @Test void countersignReturnsOk() throws Exception {
        doNothing().when(taskService).complete(eq("task_1"), eq("COUNTERSIGN"), any(), eq("u_c"), any());
        mvc.perform(post("/bpm/v1/task/task_1/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"COUNTERSIGN\",\"targetUserId\":\"u_c\",\"comment\":\"add\"}"))
           .andExpect(status().isOk());
        verify(taskService).complete(eq("task_1"), eq("COUNTERSIGN"), any(), eq("u_c"), any());
    }
}
