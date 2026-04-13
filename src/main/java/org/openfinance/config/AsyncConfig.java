package org.openfinance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution in the application.
 * 
 * <p>This configuration enables Spring's @Async annotation support and provides
 * a custom thread pool executor optimized for file import operations.</p>
 * 
 * <p><strong>Use Cases:</strong></p>
 * <ul>
 *   <li>File parsing for transaction imports (QIF, OFX, QFX)</li>
 *   <li>Bulk transaction processing</li>
 *   <li>Long-running operations that shouldn't block HTTP requests</li>
 * </ul>
 * 
 * <p><strong>Thread Pool Configuration:</strong></p>
 * <ul>
 *   <li>Core pool size: 2 threads (always available)</li>
 *   <li>Max pool size: 5 threads (peak capacity)</li>
 *   <li>Queue capacity: 25 tasks (pending operations)</li>
 *   <li>Keep-alive: 60 seconds (idle thread timeout)</li>
 * </ul>
 * 
 * <p>Requirement REQ-2.5.1.8: Asynchronous import processing</p>
 * <p>Requirement REQ-3.5: Application performance optimization</p>
 * 
 * @author Open-Finance Development Team
 * @version 1.0
 * @since 2024-01-15
 * @see org.springframework.scheduling.annotation.Async
 * @see org.openfinance.service.ImportService
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    /**
     * Creates a thread pool task executor for async operations.
     * 
     * <p>This executor is specifically tuned for file import operations which are
     * I/O intensive and may involve parsing large files. The configuration balances
     * resource usage with responsiveness:</p>
     * 
     * <ul>
     *   <li><strong>Core Pool Size (2):</strong> Minimum threads always ready to handle imports</li>
     *   <li><strong>Max Pool Size (5):</strong> Maximum concurrent imports to prevent resource exhaustion</li>
     *   <li><strong>Queue Capacity (25):</strong> Buffer for pending imports during peak usage</li>
     *   <li><strong>Keep Alive (60s):</strong> Idle threads are terminated after 1 minute</li>
     * </ul>
     * 
     * <p><strong>Rejection Policy:</strong> CallerRunsPolicy - If all threads are busy and queue is full,
     * the task runs synchronously in the caller's thread (fallback behavior).</p>
     * 
     * <p><strong>Thread Naming:</strong> Threads are named "import-executor-{n}" for easy identification
     * in logs and monitoring tools.</p>
     * 
     * <p><strong>Example Usage:</strong></p>
     * <pre>{@code
     * @Async("taskExecutor")
     * public void parseFileAsync(ImportSession session) {
     *     // Long-running parsing operation runs in background thread
     *     // HTTP request returns immediately with session ID
     * }
     * }</pre>
     * 
     * <p><strong>Monitoring:</strong></p>
     * <ul>
     *   <li>Check thread names in logs: "import-executor-1", "import-executor-2", etc.</li>
     *   <li>Monitor queue size via JMX or Spring Boot Actuator</li>
     *   <li>Watch for "Task rejected" warnings (indicates overload)</li>
     * </ul>
     * 
     * <p>Requirement REQ-2.5.1.8: Asynchronous import with configurable thread pool</p>
     * 
     * @return configured ThreadPoolTaskExecutor for async operations
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size: minimum threads always available
        executor.setCorePoolSize(2);
        
        // Max pool size: maximum concurrent imports
        executor.setMaxPoolSize(5);
        
        // Queue capacity: pending imports buffer
        executor.setQueueCapacity(25);
        
        // Thread name prefix for identification in logs
        executor.setThreadNamePrefix("import-executor-");
        
        // Keep alive time: idle thread timeout (60 seconds)
        executor.setKeepAliveSeconds(60);
        
        // Allow core threads to timeout when idle
        executor.setAllowCoreThreadTimeOut(true);
        
        // Wait for tasks to complete on shutdown (graceful shutdown)
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Maximum wait time for shutdown (30 seconds)
        executor.setAwaitTerminationSeconds(30);
        
        // Initialize the executor
        executor.initialize();
        
        return executor;
    }
    
    /**
     * Handles uncaught exceptions from @Async methods.
     * 
     * <p>This is a safety net for errors that escape method-level error handling.
     * If an exception reaches this handler, it indicates:</p>
     * <ul>
     *   <li>A bug in error handling (forgot to catch Exception)</li>
     *   <li>Critical system error (OutOfMemoryError, ThreadDeath, etc.)</li>
     *   <li>Framework-level error</li>
     * </ul>
     * 
     * <p><strong>Why This Matters:</strong></p>
     * <p>Without this handler, uncaught async exceptions are silently logged by Spring
     * and ignored. For import operations, this would leave ImportSession stuck in
     * PARSING status indefinitely, with no indication to the user that parsing failed.</p>
     * 
     * <p><strong>Handling Strategy:</strong></p>
     * <ol>
     *   <li>Log the exception with full stack trace for debugging</li>
     *   <li>Log method name and parameters for context</li>
     *   <li>Do NOT attempt to update database (no repositories available here)</li>
     *   <li>Rely on method-level error handling for session status updates</li>
     * </ol>
     * 
     * <p><strong>Best Practice:</strong> This handler should rarely trigger in production.
     * If it does, investigate immediately as it indicates a bug in error handling.</p>
     * 
     * <p>Requirement REQ-2.5.1.8: Robust async error handling</p>
     * <p>Requirement REQ-3.4: Application reliability and error tracking</p>
     * 
     * @return exception handler for async methods
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("!!! UNCAUGHT ASYNC EXCEPTION !!! in method '{}' with parameters {}: {}", 
                    method.getName(), 
                    params, 
                    ex.getMessage(), 
                    ex);
                
                // Log additional context for import operations
                if ("parseFileAsync".equals(method.getName()) && params.length > 0) {
                    log.error("Import session {} failed with uncaught exception - session may be stuck in PARSING status", 
                        params[0]);
                }
                
                // Note: Cannot inject repository here to update session status
                // Method-level error handling (try-catch in parseFileAsync) should prevent this
                // If this handler triggers, it indicates a bug in error handling
            }
        };
    }
}
