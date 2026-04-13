/**
 * Playwright E2E Test Configuration
 *
 * Task 13.2.5: Add end-to-end tests with Playwright
 *
 * Covers critical user flows:
 * - Authentication (login, register, logout)
 * - Account management (create, edit, delete)
 * - Transaction management (create, filter, delete)
 * - Dashboard display
 * - Navigation
 */
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  /** Directory containing Playwright E2E tests */
  testDir: './e2e',

  /** Run test files in parallel */
  fullyParallel: false,

  /** Fail the build on CI if `test.only` is accidentally left in code */
  forbidOnly: !!process.env.CI,

  /** Retry on CI only */
  retries: process.env.CI ? 2 : 0,

  /** Number of workers — keep sequential for stability with shared backend */
  workers: 1,

  /** Reporter to use */
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['line'],
  ],

  /** Shared settings for all projects below */
  use: {
    /** Base URL of the running dev server */
    baseURL: 'http://localhost:3000',

    /** Collect trace on first retry */
    trace: 'on-first-retry',

    /** Screenshot on failure */
    screenshot: 'only-on-failure',

    /** Timeout for each action */
    actionTimeout: 10_000,

    /** Viewport for desktop tests */
    viewport: { width: 1280, height: 720 },
  },

  /** Global test timeout */
  timeout: 30_000,

  /** Expect assertion timeout */
  expect: {
    timeout: 10_000,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
      testMatch: '**/responsive.spec.ts',
    },
  ],

  /**
   * Do NOT start the dev server automatically — the dev server must already
   * be running before executing Playwright tests. This avoids dependency on
   * a specific start command and allows testing against a real backend.
   */
  // webServer: { ... }
});
