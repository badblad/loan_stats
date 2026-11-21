package com.example.loan_stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanStatsServiceTests {
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private LoanStatsService loanStatsService;

    @ParameterizedTest
    @CsvSource({
            "addr_state",
            "application_type",
            "grade",
            "home_ownership",
            "loan_status",
            "purpose",
            "sub_grade",
            "term",
            "verification_status"
    })
    void searchesSupportedAttributesUsingGroupedLoanAmountStatistics(String attribute) {
        when(jdbcTemplate.query(anyString(), anyRowMapper())).thenReturn(List.of());

        assertTrue(loanStatsService.search(attribute).isEmpty());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), anyRowMapper());
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("\"" + attribute + "\" AS attribute_name"));
        assertTrue(sql.contains("MIN(loan_amnt) AS min_loan_amount"));
        assertTrue(sql.contains("MAX(loan_amnt) AS max_loan_amount"));
        assertTrue(sql.contains("AVG(loan_amnt) AS average_loan_amount"));
        assertTrue(sql.contains("PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY loan_amnt)"));
        assertTrue(sql.contains("GROUP BY \"" + attribute + "\""));
        assertTrue(sql.contains("ORDER BY \"" + attribute + "\""));
    }

    @Test
    void trimsAndLowercasesAttributeAndMapsQueryResults() throws Exception {
        when(jdbcTemplate.query(anyString(), anyRowMapper())).thenAnswer(invocation -> {
            RowMapper<LoanStatsResponse> rowMapper = invocation.getArgument(1);
            ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
            when(resultSet.getString("attribute_name")).thenReturn("A");
            when(resultSet.getBigDecimal("min_loan_amount")).thenReturn(new BigDecimal("1000.00"));
            when(resultSet.getBigDecimal("max_loan_amount")).thenReturn(new BigDecimal("5000.00"));
            when(resultSet.getBigDecimal("average_loan_amount")).thenReturn(new BigDecimal("3000.00"));
            when(resultSet.getBigDecimal("median_loan_amount")).thenReturn(new BigDecimal("2500.00"));
            return List.of(rowMapper.mapRow(resultSet, 0));
        });

        List<LoanStatsResponse> results = loanStatsService.search("  GRADE ");

        assertEquals(1, results.size());
        assertEquals(new LoanStatsResponse(
                "A",
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("2500.00")), results.get(0));
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), anyRowMapper());
        assertTrue(sqlCaptor.getValue().contains("\"grade\" AS attribute_name"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", "id", "grade\"; DROP TABLE loan_stats; --"})
    void rejectsUnsupportedAttributesWithoutQueryingDatabase(String attribute) {
        NotImplementedAttributeException exception = assertThrows(
                NotImplementedAttributeException.class,
                () -> loanStatsService.search(attribute));

        assertEquals("Not implemented", exception.getMessage());
        verifyNoInteractions(jdbcTemplate);
    }

    @SuppressWarnings("unchecked")
    private static RowMapper<LoanStatsResponse> anyRowMapper() {
        return any(RowMapper.class);
    }
}
