package org.jeecg.modules.bpm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.bpm.expression.BpmExpressionEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ExpressionTestControllerTest.TestConfig.class,
        properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
class ExpressionTestControllerTest {

    @Configuration
    @EnableWebMvc
    @ComponentScan(
            basePackages = "org.jeecg.modules.bpm.controller",
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = ExpressionTestController.class))
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() { return new ObjectMapper(); }

        @Bean
        BpmExpressionEvaluator bpmExpressionEvaluator() { return new BpmExpressionEvaluator(); }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void simpleMathReturnsResult() throws Exception {
        mvc.perform(post("/bpm/v1/expression/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expression\":\"1+1\",\"formData\":{}}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.result").value(2));
    }

    @Test
    void formDataConditionReturnsTrue() throws Exception {
        mvc.perform(post("/bpm/v1/expression/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expression\":\"form.amount > 100\",\"formData\":{\"amount\":200}}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.result").value(true));
    }

    @Test
    void sandboxBlocksSystemExit() throws Exception {
        mvc.perform(post("/bpm/v1/expression/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expression\":\"System.exit(0)\",\"formData\":{}}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.error").isNotEmpty());
    }
}
