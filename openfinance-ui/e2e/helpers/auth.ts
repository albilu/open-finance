/**
 * E2E Authentication Helpers
 *
 * Reusable helpers for Playwright E2E tests covering login, logout,
 * and registration flows.
 *
 * Task 13.2.5: Add end-to-end tests with Playwright
 */
import type { Page } from '@playwright/test';

/** Credentials for the pre-existing E2E test user */
export const E2E_USER = {
  username: 'real_test_user',
  password: 'Password123!',
  masterPassword: 'RealMaster123!',
} as const;

/** Credentials for a fresh registration test user */
export const E2E_REGISTER_USER = {
  username: `e2e_reg_${Date.now()}`,
  email: `e2e_reg_${Date.now()}@test.local`,
  password: 'E2eTest123!',
  masterPassword: 'E2eMaster123!',
} as const;

/**
 * Navigates to /login and fills + submits the login form.
 * Waits until the dashboard URL is reached.
 */
export async function loginAs(
  page: Page,
  credentials: { username: string; password: string; masterPassword: string } = E2E_USER,
): Promise<void> {
  await page.goto('/login');
  await page.waitForLoadState('networkidle');

  await page.getByLabel(/username/i).fill(credentials.username);
  await page.getByLabel(/^password$/i).fill(credentials.password);
  await page.getByLabel(/master password/i).fill(credentials.masterPassword);

  const submitBtn = page.getByRole('button', { name: /sign in|log in/i });
  await submitBtn.click();

  // If we are still on /login, it might be due to a validation error or slow response
  // We check if the button is still there and if there are error messages
  try {
    await page.waitForURL('**/dashboard', { timeout: 15_000 });
  } catch (e) {
    const errorVisible = await page.getByTestId('login-error-message').isVisible();
    if (errorVisible) {
      const errorText = await page.getByTestId('login-error-message').innerText();
      throw new Error(`Login failed with error: ${errorText}`);
    }
    // If no error banner, maybe validation failed?
    const validationErrors = page.locator('[role="alert"], .text-error');
    if (await validationErrors.count() > 0) {
      const firstError = await validationErrors.first().innerText();
      throw new Error(`Login failed with validation error: ${firstError}`);
    }
    throw e;
  }
}

/**
 * Logs out the currently authenticated user via the user-menu dropdown.
 */
export async function logout(page: Page): Promise<void> {
  // Open user menu — look for avatar / initials button in the sidebar/topbar
  const userMenuButton = page.getByRole('button', { name: /user menu/i })
    .or(page.locator('[aria-label*="user" i], [aria-label*="menu" i], button:has-text("TU"), button:has-text("RU")').first());
  await userMenuButton.click();

  // Click the "Logout" or "Sign out" menu item
  await page.getByRole('menuitem', { name: /log ?out|sign ?out/i }).click();
  await page.waitForURL('**/login', { timeout: 10_000 });
}

/**
 * Registers a new user and waits for redirect to /login.
 */
export async function registerUser(
  page: Page,
  user: { username: string; email: string; password: string; masterPassword: string },
): Promise<void> {
  await page.goto('/register');
  await page.waitForLoadState('networkidle');

  await page.getByLabel(/username/i).fill(user.username);
  await page.getByLabel(/email/i).fill(user.email);

  // Password fields — target by autocomplete or label
  const passwordFields = await page.getByLabel(/^password$/i).all();
  if (passwordFields.length >= 1) await passwordFields[0].fill(user.password);

  const confirmFields = await page.getByLabel(/confirm password/i).all();
  if (confirmFields.length >= 1) await confirmFields[0].fill(user.password);

  const masterFields = await page.getByLabel(/master password/i).all();
  if (masterFields.length >= 1) await masterFields[0].fill(user.masterPassword);

  const confirmMasterFields = await page.getByLabel(/confirm master/i).all();
  if (confirmMasterFields.length >= 1) await confirmMasterFields[0].fill(user.masterPassword);

  await page.getByRole('button', { name: /create account|register|sign up/i }).click();
  // After successful registration navigate to /login
  await page.waitForURL('**/login', { timeout: 15_000 });
}
