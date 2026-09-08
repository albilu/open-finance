package org.openfinance.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.openfinance.dto.TransactionRequest;
import org.openfinance.entity.RecurringTransaction;
import org.openfinance.entity.TransactionType;
import org.openfinance.repository.RecurringTransactionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecurringOccurrenceServiceImpl implements RecurringOccurrenceService {
    private final RecurringTransactionRepository recurringRepository;
    private final TransactionService transactionService;
    private final JdbcTemplate jdbc;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean post(Long recurringId, Long userId, LocalDate expectedDate) {
        // Acquire the writer/row lock before reading, including on SQLite WAL databases.
        if (jdbc.update(
                        "UPDATE recurring_transactions SET updated_at = updated_at WHERE id = ? AND user_id = ?",
                        recurringId,
                        userId)
                != 1) return false;
        RecurringTransaction template =
                recurringRepository.findByIdAndUserId(recurringId, userId).orElseThrow();
        if (!Boolean.TRUE.equals(template.getIsActive())
                || !expectedDate.equals(template.getNextOccurrence())) return false;
        if (expectedDate.isAfter(LocalDate.now())) return false;
        TransactionRequest request =
                TransactionRequest.builder()
                        .accountId(template.getAccountId())
                        .toAccountId(template.getToAccountId())
                        .type(template.getType())
                        .amount(template.getAmount())
                        .currency(template.getCurrency())
                        .categoryId(template.getCategoryId())
                        .date(expectedDate)
                        .description(template.getDescription())
                        .notes(template.getNotes())
                        .isReconciled(false)
                        .build();
        if (template.getType() == TransactionType.TRANSFER) {
            transactionService.createTransfer(userId, request);
        } else {
            transactionService.createTransaction(userId, request);
        }
        LocalDate next = template.calculateNextOccurrence();
        template.setNextOccurrence(next);
        if (template.getEndDate() != null && next.isAfter(template.getEndDate()))
            template.setIsActive(false);
        recurringRepository.save(template);
        return true;
    }
}
