package org.openfinance.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openfinance.dto.calculator.*;
import org.openfinance.service.FinancialFreedomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for financial freedom calculations.
 * 
 * <p>Provides endpoints for:</p>
 * <ul>
 *   <li>Calculating time to financial freedom</li>
 *   <li>Calculating savings longevity</li>
 *   <li>Retrieving default calculation parameters</li>
 * </ul>
 * 
 * <p><strong>API Endpoints:</strong></p>
 * <ul>
 *   <li>POST /api/v1/calculator/financial-freedom/timeline - Calculate time to freedom</li>
 *   <li>POST /api/v1/calculator/financial-freedom/longevity - Calculate savings longevity</li>
 *   <li>GET /api/v1/calculator/financial-freedom/defaults - Get default parameters</li>
 * </ul>
 * 
 * @author Open-Finance Development Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/calculator/financial-freedom")
@RequiredArgsConstructor
@Slf4j
public class FinancialFreedomController {
    
    private final FinancialFreedomService financialFreedomService;
    
    /**
     * Calculate time to financial freedom.
     * 
     * <p>Determines how long it will take for savings to grow to the target amount
     * needed to sustain the user's lifestyle indefinitely.</p>
     * 
     * <p><strong>Request Body:</strong> {@link FreedomCalculatorRequest}</p>
     * <p><strong>Response:</strong> {@link FreedomCalculatorResponse}</p>
     * 
     * @param request Calculation input parameters
     * @return Timeline projections and sensitivity analysis
     */
    @PostMapping("/timeline")
    public ResponseEntity<FreedomCalculatorResponse> calculateTimeline(
            @Valid @RequestBody FreedomCalculatorRequest request) {
        
        log.info("Received financial freedom calculation request: savings={}, expenses={}, return={}%",
                request.getCurrentSavings(), request.getMonthlyExpenses(), 
                request.getExpectedAnnualReturn());
        
        FreedomCalculatorResponse response = financialFreedomService.calculateTimeToFreedom(request);
        
        log.info("Calculated financial freedom timeline: {} years, {} months to goal",
                response.getYearsToFreedom(), response.getMonthsToFreedom());
        
        return ResponseEntity.ok(response);
    }
    
      /**
       * Calculate savings longevity.
       *
       * <p>Determines how long current savings will last given ongoing expenses
       * and expected investment returns.</p>
       *
       * <p><strong>Request Body:</strong> {@link FreedomCalculatorRequest}</p>
       * <p><strong>Response:</strong> {@link SavingsLongevityResult}</p>
       *
       * @param request Calculation input parameters
       * @return Longevity analysis and projections
       */
      @PostMapping("/longevity")
      public ResponseEntity<SavingsLongevityResult> calculateLongevity(
              @Valid @RequestBody FreedomCalculatorRequest request) {

          log.info("Received savings longevity calculation request: savings={}, expenses={}, return={}%",
                  request.getCurrentSavings(), request.getMonthlyExpenses(), request.getExpectedAnnualReturn());

          SavingsLongevityResult result = financialFreedomService.calculateSavingsLongevity(
                  request.getCurrentSavings(), request.getMonthlyExpenses(), request.getExpectedAnnualReturn());

          log.info("Calculated savings longevity: willDeplete={}, yearsUntilDepletion={}",
                  result.isWillDeplete(), result.getYearsUntilDepletion());

          return ResponseEntity.ok(result);
      }
    
    /**
     * Get default calculation parameters.
     * 
     * <p>Returns default values and valid ranges for calculator inputs.
     * Useful for initializing UI components.</p>
     * 
     * <p><strong>Response:</strong> {@link CalculationDefaults}</p>
     * 
     * @return Default parameters and valid ranges
     */
    @GetMapping("/defaults")
    public ResponseEntity<CalculationDefaults> getDefaults() {
        log.debug("Returning financial freedom calculator defaults");
        
        CalculationDefaults defaults = financialFreedomService.getDefaults();
        
        return ResponseEntity.ok(defaults);
    }
}
