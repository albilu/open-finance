package org.openfinance.service;

import java.time.LocalDate;

public interface RecurringOccurrenceService {
    boolean post(Long recurringId, Long userId, LocalDate expectedDate);
}
