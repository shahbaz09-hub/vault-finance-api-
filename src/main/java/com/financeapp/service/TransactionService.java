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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * Service class responsible for managing financial transactions.
 * 
 * This service implements the core business logic for CRUD operations on transactions.
 * It ensures that transactions are associated with the currently authenticated user
 * and supports features like soft-deletion, pagination, and dynamic filtering.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "date", "amount", "category", "type", "createdAt"
    );

    /**
     * Creates a new financial transaction.
     * 
     * Associates the transaction with the currently authenticated user.
     * By default, the transaction is marked as not deleted (soft-delete flag).
     *
     * @param request The data transfer object containing transaction details (amount, type, etc.).
     * @return TransactionResponse containing the persisted transaction entity data.
     */
    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        User currentUser = getCurrentUser();

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory().trim())
                .date(request.getDate())
                .notes(request.getNotes())
                .createdBy(currentUser)
                .deleted(false)
                .build();

        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    /**
     * Retrieves a paginated and filtered list of transactions.
     * 
     * Supports dynamic filtering by transaction type, category, and date range.
     * Defends against SQL injection/sorting errors by restricting sorting to allowed fields.
     *
     * @param type       The type of transaction to filter by (optional).
     * @param category   The category of transaction to filter by (optional).
     * @param startDate  The start date of the reporting period (optional).
     * @param endDate    The end date of the reporting period (optional).
     * @param page       The zero-based page index to retrieve.
     * @param size       The number of records per page.
     * @param sortBy     The field name to sort the results by.
     * @param sortDir    The sorting direction ('asc' or 'desc').
     * @return A paginated list of TransactionResponse objects.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAll(
            TransactionType type,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "date";

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(safeSortBy).ascending()
                : Sort.by(safeSortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return transactionRepository
                .findAllWithFilters(type, category, startDate, endDate, pageable)
                .map(transactionMapper::toResponse);
    }

    /**
     * Retrieves a specific transaction by its unique identifier.
     * 
     * Evaluates only active (non-deleted) transactions.
     *
     * @param id The primary key of the transaction to retrieve.
     * @return TransactionResponse representing the requested transaction.
     * @throws ResourceNotFoundException if the transaction does not exist or has been soft-deleted.
     */
    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        Transaction transaction = findActiveTransactionById(id);
        return transactionMapper.toResponse(transaction);
    }

    /**
     * Updates an existing financial transaction.
     * 
     * Validates that the transaction exists and is active before applying updates.
     * Replaces the modifiable fields with the values provided in the request payload.
     *
     * @param id      The primary key of the transaction to update.
     * @param request The data transfer object containing the updated transaction details.
     * @return TransactionResponse reflecting the applied updates.
     * @throws ResourceNotFoundException if the transaction does not exist or has been soft-deleted.
     */
    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = findActiveTransactionById(id);

        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory().trim());
        transaction.setDate(request.getDate());
        transaction.setNotes(request.getNotes());

        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    /**
     * Soft-deletes a specific transaction.
     * 
     * Instead of permanently removing the record from the database, this method sets
     * a 'deleted' flag to true. This preserves historical data integrity while removing
     * the transaction from active views and aggregations.
     *
     * @param id The primary key of the transaction to be masked as deleted.
     * @throws ResourceNotFoundException if the transaction does not exist or is already deleted.
     */
    @Transactional
    public void softDelete(Long id) {
        Transaction transaction = findActiveTransactionById(id);
        transaction.setDeleted(true);
        transactionRepository.save(transaction);
    }

    private Transaction findActiveTransactionById(Long id) {
        return transactionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
