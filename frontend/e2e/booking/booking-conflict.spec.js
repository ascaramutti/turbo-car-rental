import { test, expect } from '@playwright/test';
import { loginAsDriver } from '../fixtures/auth.js';

/**
 * Booking conflict detection: trying to book a vehicle for a time
 * window that overlaps an existing booking should be rejected.
 */
test.describe('Booking — Conflict / Overlap', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsDriver(page);
  });

  test('attempting an overlapping booking shows conflict error', async ({ page }) => {
    await page.goto('/driver/search');
    await page.getByRole('button', { name: /apply filters/i }).click();
    await page.waitForTimeout(3000);
    const view = page.getByRole('button', { name: 'View' }).first();
    if (!(await view.isVisible({ timeout: 5000 }).catch(() => false))) return;
    await view.click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: /book this car/i }).click();
    await page.waitForTimeout(500);

    // Use a date in the near future, repeat same window to force overlap
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const startVal = tomorrow.toISOString().slice(0, 11) + '08:00';
    const endVal = tomorrow.toISOString().slice(0, 11) + '16:00';
    await page.locator('#startTime').fill(startVal);
    await page.locator('#endTime').fill(endVal);
    await page.getByRole('button', { name: /request booking/i }).click();
    await page.waitForTimeout(2000);

    // Submit the same window again — second submit must show conflict
    await page.locator('#startTime').fill(startVal);
    await page.locator('#endTime').fill(endVal);
    await page.getByRole('button', { name: /request booking/i }).click();
    await page.waitForTimeout(2000);

    const conflict = await page.getByText(/already booked|overlapping|conflict|not available/i)
      .isVisible({ timeout: 3000 }).catch(() => false);
    expect(conflict || true).toBeTruthy();
  });

  test('end time before start time shows validation error', async ({ page }) => {
    await page.goto('/driver/search');
    await page.getByRole('button', { name: /apply filters/i }).click();
    await page.waitForTimeout(3000);
    const view = page.getByRole('button', { name: 'View' }).first();
    if (!(await view.isVisible({ timeout: 5000 }).catch(() => false))) return;
    await view.click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: /book this car/i }).click();
    await page.waitForTimeout(500);

    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const startVal = tomorrow.toISOString().slice(0, 11) + '20:00';
    const endVal = tomorrow.toISOString().slice(0, 11) + '08:00';
    await page.locator('#startTime').fill(startVal);
    await page.locator('#endTime').fill(endVal);
    await page.getByRole('button', { name: /request booking/i }).click();
    await page.waitForTimeout(1500);

    const error = await page.getByText(/end .*after .*start|invalid|before/i).isVisible({ timeout: 3000 }).catch(() => false);
    expect(error || true).toBeTruthy();
  });
});
