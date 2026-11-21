package com.example.loan_stats;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class LoanStatsService {
    private static final Set<String> IMPLEMENTED_ATTRIBUTES = Set.of(
            "addr_state",
            "application_type",
            "grade",
            "home_ownership",
            "loan_status",
            "purpose",
            "sub_grade",
            "term",
            "verification_status");

    private final JdbcTemplate jdbcTemplate;

    public LoanStatsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LoanStatsResponse> search(String attribute) {
        String column = attribute == null ? "" : attribute.trim().toLowerCase();
        if (!IMPLEMENTED_ATTRIBUTES.contains(column)) {
            throw new NotImplementedAttributeException();
        }

        String sql = """
                SELECT "%s" AS attribute_name,
                       MIN(loan_amnt) AS min_loan_amount,
                       MAX(loan_amnt) AS max_loan_amount,
                       AVG(loan_amnt) AS average_loan_amount,
                       CAST(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY loan_amnt) AS NUMERIC(14, 2))
                           AS median_loan_amount
                FROM loan_stats
                WHERE "%s" IS NOT NULL AND BTRIM("%s") <> '' AND loan_amnt IS NOT NULL
                GROUP BY "%s"
                ORDER BY "%s"
                """.formatted(column, column, column, column, column);

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new LoanStatsResponse(
                resultSet.getString("attribute_name"),
                resultSet.getBigDecimal("min_loan_amount"),
                resultSet.getBigDecimal("max_loan_amount"),
                resultSet.getBigDecimal("average_loan_amount"),
                resultSet.getBigDecimal("median_loan_amount")));
    }
}
