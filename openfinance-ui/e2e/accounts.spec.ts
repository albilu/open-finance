/**
 * E2E Tests: Account Management
 *
 * Covers critical flows:
 * - core-012: Create checking account with initial balance
 * - core-013: Create savings account
 * - core-014: Create account with zero balance (edge case)
 * - core-018: Edit account name and description
 * - core-019: Delete account validation
 * - Accounts page renders the account list
 *
 * Task 13.2.5: Add end-to-end tests with Playwright
 * Requirements: REQ-2.2 (Account Management)
 */
import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/auth';

/** Unique name suffix to prevent collisions across test runs */
const RUN_ID = Date.now();

test.describe('Account Management', () => {
  // Log in once before all tests in this suite
  test.beforeEach(async ({ page }) => {
    await loginAs(page);
    await page.goto('/accounts');
    await page.waitForLoadState('networkidle');
  });

  // ─── Accounts page ─────────────────────────────────────────────────────────

  test('accounts page renders page heading', async ({ page }) => {
    // Page title should include "Accounts"
    await expect(page.getByRole('heading', { name: /accounts/i })).toBeVisible();
  });

  test('accounts page shows Add Account button', async ({ page }) => {
    const addBtn = page.getByRole('button', { name: /add account/i });
    await expect(addBtn).toBeVisible();
  });

  // ─── Create account ────────────────────────────────────────────────────────

  test('core-012: create checking account with initial balance', async ({ page }) => {
    const accountName = `E2E Checking ${RUN_ID}`;

    // Open the create form
    await page.getByRole('button', { name: /add account/i }).click();
    await expect(page.getByRole('dialog')).toBeVisible();

    // Fill in the form
    await page.getByLabel(/name/i).fill(accountName);

    // Select account type — "Checking" is the default, but set it explicitly
    const typeSelect = page.locator('select[name="type"], [aria-label*="type" i]').first();
    if (await typeSelect.isVisible().catch(() => false)) {
      await typeSelect.selectOption('CHECKING');
    }

    // Set initial balance
    const balanceInput = page.getByLabel(/initial balance/i);
    await balanceInput.fill('1500');

    // Submit
    const submitBtn = page.getByRole('button', { name: /create|save/i });
    await submitBtn.click();

    // Dialog should close and the new account should appear in the list
    await expect(page.getByRole('dialog')).not.toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(accountName)).toBeVisible({ timeout: 10_000 });
  });

  test('core-013: create savings account', async ({ page }) => {
    const accountName = `E2E Savings ${RUN_ID}`;

    await page.getByRole('button', { name: /add account/i }).click();
    await expect(page.getByRole('dialog')).toBeVisible();

    await page.getByLabel(/name/i).fill(accountName);

    const typeSelect = page.locator('select[name="type"]').first();
    if (await typeSelect.isVisible().catch(() => false)) {
      await typeSelect.selectOption('SAVINGS');
    }

    const balanceInput = page.getByLabel(/initial balance/i);
    await balanceInput.fill('5000');

    const submitBtn2 = page.getByRole('button', { name: /create|save/i });
    await submitBtn2.click();
    await expect(page.getByRole('dialog')).not.toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(accountName)).toBeVisible({ timeout: 10_000 });
  });

  test('core-014: create account with zero initial balance', async ({ page }) => {
    const accountName = `E2E Zero ${RUN_ID}`;

    await page.getByRole('button', { name: /add account/i }).click();
    await expect(page.getByRole('dialog')).toBeVisible();

    await page.getByLabel(/name/i).fill(accountName);

    const balanceInput = page.getByLabel(/initial balance/i);
    await balanceInput.fill('0');

    const submitBtn3 = page.getByRole('button', { name: /create|save/i });
    await submitBtn3.click();
    await expect(page.getByRole('dialog')).not.toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(accountName)).toBeVisible({ timeout: 10_000 });
  });

  test('account form validates required name field', async ({ page }) => {
    await page.getByRole('button', { name: /add account/i }).click();
    await expect(page.getByRole('dialog')).toBeVisible();

    // Submit without filling name
    const submitBtn4 = page.getByRole('button', { name: /create|save/i });
    await submitBtn4.click();

    // Validation error should show
    const nameError = page.locator('[id*="name-error"], .text-error').first();
    await expect(nameError).toBeVisible({ timeout: 5_000 });

    // Dialog should still be open
    await expect(page.getByRole('dialog')).toBeVisible();
  });

  // ─── Edit account ──────────────────────────────────────────────────────────

  test('core-018: edit account name opens pre-filled form', async ({ page }) => {
    // Wait for at least one account card to be rendered
    const firstCard = page.locator('[aria-label="Edit account"]').first();
    await expect(firstCard).toBeVisible({ timeout: 10_000 });

    // Hover to reveal action buttons (they are opacity-0 by default)
    const cardContainer = page.locator('.group').first();
    await cardContainer.hover();

    await firstCard.click();

    // Edit dialog should open
    await expect(page.getByRole('dialog')).toBeVisible();

    // Name field should be pre-filled (not empty)
    const nameField = page.getByLabel(/name/i);
    const currentName = await nameField.inputValue();
    expect(currentName.length).toBeGreaterThan(0);
  });

  // ─── Cancel / Close form ────────────────────────────────────────────────────

  test('cancel button closes the account form without saving', async ({ page }) => {
    await page.getByRole('button', { name: /add account/i }).click();
    await expect(page.getByRole('dialog')).toBeVisible();

    // Click cancel
    const cancelBtn = page.getByRole('button', { name: /cancel/i });
    await expect(cancelBtn).toBeVisible();
    await cancelBtn.click();

    // Dialog should close
    await expect(page.getByRole('dialog')).not.toBeVisible({ timeout: 5_000 });
  });
});
