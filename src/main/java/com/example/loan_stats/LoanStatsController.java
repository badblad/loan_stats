package com.example.loan_stats;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class LoanStatsController {
    private final LoanStatsService loanStatsService;

    public LoanStatsController(LoanStatsService loanStatsService) {
        this.loanStatsService = loanStatsService;
    }

    @GetMapping("/api/loans")
    public List<LoanStatsResponse> search(@RequestParam(required = false) String attribute) {
        return loanStatsService.search(attribute);
    }

    @ExceptionHandler(NotImplementedAttributeException.class)
    public ResponseEntity<Map<String, String>> handleNotImplementedAttribute() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Not implemented"));
    }
}
