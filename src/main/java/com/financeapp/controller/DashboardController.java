package com.financeapp.controller;

import com.financeapp.dto.response.*;
import com.financeapp.enums.TransactionType;
import com.financeapp.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Validated
@Tag(name = "Dashboard", description = "Analytics and summary endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Operation(
        summary = "Get overall financial summary",
        description = "Returns total income, total expenses, net balance, and transaction count. Accessible by all roles."
    )
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSummary()));
    }

    @GetMapping("/category-wise")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(
        summary = "Get category-wise totals",
        description = "Returns total amounts grouped by category. Filter by INCOME or EXPENSE. ANALYST and ADMIN only."
    )
    public ResponseEntity<ApiResponse<List<CategorySummaryResponse>>> getCategoryWise(
            @RequestParam(defaultValue = "EXPENSE") TransactionType type
    ) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getCategoryWiseTotals(type)));
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Operation(
        summary = "Get monthly trends",
        description = "Returns monthly income and expense breakdown. Default is last 6 months. ANALYST and ADMIN only."
    )
    public ResponseEntity<ApiResponse<List<MonthlyTrendResponse>>> getMonthlyTrends(
            @RequestParam(defaultValue = "6") @Min(1) @Max(24) int months
    ) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getMonthlyTrends(months)));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Operation(
        summary = "Get recent transactions",
        description = "Returns the latest N transactions. Default limit is 10. Accessible by all roles."
    )
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getRecent(
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getRecentTransactions(limit)));
    }
}
