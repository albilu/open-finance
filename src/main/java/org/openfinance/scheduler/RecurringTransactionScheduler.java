package org.openfinance.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openfinance.config.SchedulerProperties;
import org.openfinance.service.RecurringTransactionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduled job for automatically processing due recurring transactions.
 *
 * <p>This scheduler runs daily at midnight to generate actual transactions from
 * recurring transaction templates. It queries all due recurring transactions across
 * all users and creates corresponding Transaction entries.</p>
 *
 * <p><strong>Default Schedule:</strong></p>
 * <ul>
 *   <li>Frequency: Daily at midnight (00:00:00)</li>
 *   <li>Cron Expression: {@code 0 0 0 * * ?}</li>
 *   <li>Time Zone: System default (UTC recommended for production)</li>
 * </ul>
 *
 * <p><strong>Configurable Frequency</strong> ({@code application.scheduled.recurring-transactions.mode}):</p>
 * <ul>
 *   <li>{@code DEFAULT} — daily at midnight (built-in default above)</li>
 *   <li>{@code STARTUP_ONLY} — once on application startup, no periodic schedule</li>
 *   <li>{@code STARTUP_AND_EVERY_X_HOURS} — on startup, then every {@code interval-hours} hours</li>
 *   <li>{@code EVERY_HOUR} — once per hour</li>
 *   <li>{@code DAILY} — once per day at midnight</li>
 * </ul>
 *
 * <p><strong>Behavior:</strong></p>
 * <ul>
 *   <li>Fetches all active recurring transactions where nextOccurrence <= today</li>
 *   <li>Creates actual Transaction for each due recurring transaction</li>
 *   <li>Updates nextOccurrence date based on frequency (DAILY, WEEKLY, MONTHLY, etc.)</li>
 *   <li>Sets isActive=false if endDate has been reached</li>
 *   <li>Logs summary statistics (processed count, failed count, errors)</li>
 *   <li>Continues processing even if individual transactions fail</li>
 * </ul>
 *
 * <p><strong>Performance:</strong> With 100 users and 500 total recurring transactions,
 * expect ~5-10 seconds execution time. Uses batch processing to handle errors gracefully.</p>
 *
 * <p><strong>Error Handling:</strong> Failures for individual recurring transactions are
 * caught, logged, and reported in the ProcessingResult. The scheduler continues processing
 * remaining transactions to ensure maximum reliability.</p>
 *
 * <p><strong>Requirements:</strong></p>
 * <ul>
 *   <li>REQ-2.3.6: Recurring transaction management</li>
 *   <li>REQ-2.3.6.1: Automatic processing based on frequency</li>
 *   <li>REQ-2.3.6.2: End date handling</li>
 * </ul>
 *
 * @author Open-Finance Development Team
 * @version 1.0
 * @since 2026-02-03
 * @see SchedulerProperties
 * @see RecurringTransactionService#processRecurringTransactions()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransactionScheduler implements ApplicationRunner {

    /** Default cron: daily at midnight. */
    static final String DEFAULT_CRON = "0 0 0 * * ?";

    private final RecurringTransactionService recurringTransactionService;
    private final SchedulerProperties schedulerProperties;

    // -----------------------------------------------------------------
    // Startup execution
    // -----------------------------------------------------------------

    /**
     * Runs recurring transaction processing once on application startup when the
     * configured mode requests it ({@code STARTUP_ONLY} or
     * {@code STARTUP_AND_EVERY_X_HOURS}).
     */
    @Override
    public void run(ApplicationArguments args) {
        if (schedulerProperties.getRecurringTransactions().isRunOnStartup()) {
            log.info("Executing startup recurring transaction processing (mode={})",
                    schedulerProperties.getRecurringTransactions().getMode());
            processRecurringTransactions();
        }
    }

    // -----------------------------------------------------------------
    // Periodic execution
    // -----------------------------------------------------------------

    /**
     * Scheduled job to process due recurring transactions daily at midnight.
     *
     * <p>Cron expression: {@code 0 0 0 * * ?} translates to:</p>
     * <ul>
     *   <li>0 seconds</li>
     *   <li>0 minutes</li>
     *   <li>0 hours (midnight)</li>
     *   <li>Every day of month</li>
     *   <li>Every month</li>
     *   <li>Every day of week (? means no specific value)</li>
     * </ul>
     *
     * <p><strong>Processing Steps:</strong></p>
     * <ol>
     *   <li>Query all active recurring transactions with nextOccurrence <= today</li>
     *   <li>For each recurring transaction:
     *     <ul>
     *       <li>Create a new Transaction with the same details</li>
     *       <li>Calculate next occurrence date based on frequency</li>
     *       <li>Update nextOccurrence in recurring transaction</li>
     *       <li>Set isActive=false if endDate has been reached</li>
     *     </ul>
     *   </li>
     *   <li>Log summary with processed count, failed count, and error details</li>
     * </ol>
     *
     * <p>The effective cron is resolved once at startup via Spring SpEL from
     * {@link SchedulerProperties.SchedulerConfig#effectiveCron(String)}.
     * When mode is {@code STARTUP_ONLY} the cron resolves to {@code "-"}, which
     * instructs Spring not to schedule any periodic execution.</p>
     *
     * <p><strong>Example Log Output:</strong></p>
     * <pre>{@code
     * INFO  Starting scheduled recurring transaction processing
     * INFO  Found 25 due recurring transactions to process
     * INFO  Recurring transaction processing complete: processed=24, failed=1, duration=3.2s
     * }</pre>
     *
     * <p>Requirement REQ-2.3.6: Automatically process recurring transactions daily</p>
     */
    @Scheduled(cron = "#{schedulerProperties.recurringTransactions.effectiveCron('"
            + DEFAULT_CRON + "')}")
    public void processRecurringTransactions() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("Starting scheduled recurring transaction processing at {} (mode={})",
                startTime, schedulerProperties.getRecurringTransactions().getMode());

        try {
            RecurringTransactionService.ProcessingResult result =
                    recurringTransactionService.processRecurringTransactions();

            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

            log.info("Recurring transaction processing complete: processed={}, failed={}, duration={}s",
                    result.getProcessedCount(), result.getFailedCount(), durationSeconds);

            if (!result.getErrors().isEmpty()) {
                log.warn("Recurring transaction processing encountered {} errors:", result.getErrors().size());
                result.getErrors().forEach(error -> log.warn("  - {}", error));
            }

        } catch (Exception e) {
            log.error("Fatal error during scheduled recurring transaction processing", e);
        }
    }

    // -----------------------------------------------------------------
    // Manual trigger
    // -----------------------------------------------------------------

    /**
     * Manual trigger for processing recurring transactions outside of the scheduled time.
     *
     * <p>This method can be called via an admin endpoint for manual processing.
     * Useful for testing or immediate processing when needed.</p>
     *
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Testing recurring transaction processing in development</li>
     *   <li>Recovering from a failed scheduled job</li>
     *   <li>Processing on-demand after system maintenance</li>
     * </ul>
     *
     * <p><strong>Warning:</strong> This method should only be called by administrators
     * to avoid processing recurring transactions multiple times in a single day.</p>
     *
     * @return summary message with processing statistics
     */
    public String triggerManualProcessing() {
        log.info("Manual recurring transaction processing triggered");

        try {
            RecurringTransactionService.ProcessingResult result =
                    recurringTransactionService.processRecurringTransactions();

            String message = String.format(
                    "Manual processing completed. Processed: %d, Failed: %d, Errors: %d",
                    result.getProcessedCount(), result.getFailedCount(), result.getErrors().size());

            log.info(message);

            if (!result.getErrors().isEmpty()) {
                log.warn("Errors during manual processing:");
                result.getErrors().forEach(error -> log.warn("  - {}", error));
            }

            return message;

        } catch (Exception e) {
            String message = "Manual processing failed: " + e.getMessage();
            log.error(message, e);
            return message;
        }
    }
}
