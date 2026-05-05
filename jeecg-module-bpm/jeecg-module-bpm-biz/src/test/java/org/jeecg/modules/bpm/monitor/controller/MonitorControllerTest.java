package org.jeecg.modules.bpm.monitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.bpm.monitor.service.MonitorQueryService;
import org.jeecg.modules.bpm.monitor.service.MonitorStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = MonitorControllerTest.TestConfig.class,
        properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
class MonitorControllerTest {

    @Configuration
    @EnableWebMvc
    @ComponentScan(
            basePackages = "org.jeecg.modules.bpm.monitor.controller",
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MonitorController.class)
    )
    static class TestConfig {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @Autowired MockMvc mvc;
    @MockBean MonitorQueryService queryService;
    @MockBean MonitorStatsService statsService;

    @Test
    void instancesReturnsPageResult() throws Exception {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("records", Collections.emptyList());
        page.put("total", 0L);
        page.put("pageNo", 1);
        page.put("pageSize", 20);
        when(queryService.listInstances(any())).thenReturn(page);

        mvc.perform(get("/bpm/v1/monitor/instances?defKey=foo&pageNo=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.records").isArray());
    }

    @Test
    void instancesWithNoParamsUsesDefaults() throws Exception {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("records", Collections.emptyList());
        page.put("total", 0L);
        page.put("pageNo", 1);
        page.put("pageSize", 20);
        when(queryService.listInstances(any())).thenReturn(page);

        mvc.perform(get("/bpm/v1/monitor/instances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(20));
    }
}
