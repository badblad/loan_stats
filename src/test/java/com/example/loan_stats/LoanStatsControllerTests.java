package com.example.loan_stats;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanStatsControllerTests {
    @Test
    void unsupportedAttributeReturnsNotImplementedMessage() throws Exception {
        LoanStatsController controller = new LoanStatsController(
                new LoanStatsService(mock(JdbcTemplate.class)));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/loans").param("attribute", "unsupported"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Not implemented"));
    }
}
