package org.openfinance.validation;

/**
 * Validation group applied only on create endpoints.
 *
 * <p>DTOs reused for both create and update assign this group to constraints that must hold on
 * creation but be optional on partial update (e.g. a required-on-create field that a PATCH may omit
 * to leave unchanged).
 */
public interface OnCreate {}
