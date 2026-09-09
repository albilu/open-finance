package org.openfinance.dto;

import java.math.BigDecimal;

/** Null tranche denotes repayment of an opening balance, not an undrawn commitment. */
public record PrincipalAllocationResponse(Long trancheId, BigDecimal amount) {}
