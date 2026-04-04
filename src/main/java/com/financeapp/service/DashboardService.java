package com.financeapp.service;

import com.financeapp.dto.response.CategorySummaryResponse;
import com.financeapp.dto.response.DashboardSummaryResponse;
import com.financeapp.dto.response.MonthlyTrendResponse;
import com.financeapp.dto.response.TransactionResponse;
import com.financeapp.enums.TransactionType;
import com.financeapp.mapper.TransactionMapper;
import com.financeapp.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        BigDecimal totalIncome = transactionRepository.sumByType(TransactionType.INCOME);
        BigDecimal totalExpenses = transactionRepository.sumByType(TransactionType.EXPENSE);
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);
        long totalTransactions = transactionRepository.countByDeletedFalse();

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .totalTransactions(totalTransactions)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> getCategoryWiseTotals(TransactionType type) {
        return transactionRepository.getCategoryWiseTotals(type);
    }

    @Transactional(readOnly = true)
    public List<MonthlyTrendResponse> getMonthlyTrends(int months) {
        LocalDate startDate = LocalDate.now().minusMonths(months);
        return transactionRepository.getMonthlyTrends(startDate);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentTransactions(int limit) {
        return transactionRepository
                .findRecentTransactions(PageRequest.of(0, limit))
                .stream()
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }
}
