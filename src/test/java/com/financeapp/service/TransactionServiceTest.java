package com.financeapp.service;

import com.financeapp.dto.request.TransactionRequest;
import com.financeapp.dto.response.TransactionResponse;
import com.financeapp.enums.TransactionType;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.mapper.TransactionMapper;
import com.financeapp.model.Transaction;
import com.financeapp.model.User;
import com.financeapp.repository.TransactionRepository;
import com.financeapp.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Transaction testTransaction;
    private TransactionResponse testResponse;
    private TransactionRequest testRequest;

    @BeforeEach
    void setUp() {
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("admin@test.com");
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        testUser = User.builder()
                .id(1L)
                .name("Admin")
                .email("admin@test.com")
                .build();

        testTransaction = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("5000.00"))
                .type(TransactionType.INCOME)
                .category("Salary")
                .date(LocalDate.of(2024, 4, 1))
                .notes("Monthly salary")
                .createdBy(testUser)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testResponse = TransactionResponse.builder()
                .id(1L)
                .amount(new BigDecimal("5000.00"))
                .type(TransactionType.INCOME)
                .category("Salary")
                .date(LocalDate.of(2024, 4, 1))
                .notes("Monthly salary")
                .createdByName("Admin")
                .createdAt(testTransaction.getCreatedAt())
                .updatedAt(testTransaction.getUpdatedAt())
                .build();

        testRequest = new TransactionRequest();
        testRequest.setAmount(new BigDecimal("5000.00"));
        testRequest.setType(TransactionType.INCOME);
        testRequest.setCategory("Salary");
        testRequest.setDate(LocalDate.of(2024, 4, 1));
        testRequest.setNotes("Monthly salary");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create - should save and return transaction")
    void create_Success() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testUser));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(transactionMapper.toResponse(testTransaction)).thenReturn(testResponse);

        TransactionResponse result = transactionService.create(testRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(result.getCategory()).isEqualTo("Salary");

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("GetById - should return transaction when found")
    void getById_Success() {
        when(transactionRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testTransaction));
        when(transactionMapper.toResponse(testTransaction)).thenReturn(testResponse);

        TransactionResponse result = transactionService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(transactionRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    @DisplayName("GetById - should throw ResourceNotFoundException when not found")
    void getById_NotFound() {
        when(transactionRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    @DisplayName("Update - should modify and return updated transaction")
    void update_Success() {
        TransactionRequest updateRequest = new TransactionRequest();
        updateRequest.setAmount(new BigDecimal("6000.00"));
        updateRequest.setType(TransactionType.INCOME);
        updateRequest.setCategory("Salary Updated");
        updateRequest.setDate(LocalDate.of(2024, 4, 15));
        updateRequest.setNotes("Updated salary");

        Transaction updatedTransaction = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("6000.00"))
                .type(TransactionType.INCOME)
                .category("Salary Updated")
                .date(LocalDate.of(2024, 4, 15))
                .notes("Updated salary")
                .createdBy(testUser)
                .deleted(false)
                .build();

        TransactionResponse updatedResponse = TransactionResponse.builder()
                .id(1L)
                .amount(new BigDecimal("6000.00"))
                .type(TransactionType.INCOME)
                .category("Salary Updated")
                .date(LocalDate.of(2024, 4, 15))
                .notes("Updated salary")
                .createdByName("Admin")
                .build();

        when(transactionRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(updatedTransaction);
        when(transactionMapper.toResponse(updatedTransaction)).thenReturn(updatedResponse);

        TransactionResponse result = transactionService.update(1L, updateRequest);

        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(result.getCategory()).isEqualTo("Salary Updated");

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("SoftDelete - should mark transaction as deleted")
    void softDelete_Success() {
        when(transactionRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        transactionService.softDelete(1L);

        assertThat(testTransaction.getDeleted()).isTrue();
        verify(transactionRepository).save(testTransaction);
    }

    @Test
    @DisplayName("SoftDelete - should throw ResourceNotFoundException when not found")
    void softDelete_NotFound() {
        when(transactionRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.softDelete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("GetAll - should return paginated results")
    void getAll_Success() {
        Page<Transaction> page = new PageImpl<>(List.of(testTransaction));
        when(transactionRepository.findAllWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(page);
        when(transactionMapper.toResponse(testTransaction)).thenReturn(testResponse);

        Page<TransactionResponse> result = transactionService.getAll(
                null, null, null, null, 0, 10, "date", "desc"
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategory()).isEqualTo("Salary");
    }
}
