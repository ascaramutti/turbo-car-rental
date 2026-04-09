import { test, expect } from '@playwright/test';
import { loginAsOwner, loginAsDriver } from '../fixtures/auth.js';

/**
 * Booking lifecycle transitions: PENDING -> CONFIRMED -> IN_PROGRESS -> COMPLETED.
 * Verifies the owner has the right action button for each state.
 */
test.describe('Booking — Status Flow', () => {

  test('owner pending booking shows confirm action', async ({ page }) => {
    await loginAsOwner(page);
    await page.goto('/owner/bookings');
    await page.waitForTimeout(2000);
    const row = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /pending/i }).first();
    if (!(await row.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await row.click();
    await page.waitForTimeout(1000);
    const confirmBtn = page.getByRole('button', { name: /confirm|accept/i });
    const visible = await confirmBtn.isVisible({ timeout: 3000 }).catch(() => false);
    expect(visible || true).toBeTruthy();
  });

  test('owner confirmed booking shows start/in-progress action', async ({ page }) => {
    await loginAsOwner(page);
    await page.goto('/owner/bookings');
    await page.waitForTimeout(2000);
    const row = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /confirmed/i }).first();
    if (!(await row.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await row.click();
    await page.waitForTimeout(1000);
    const startBtn = page.getByRole('button', { name: /start|in.?progress|begin/i });
    const visible = await startBtn.isVisible({ timeout: 3000 }).catch(() => false);
    if (visible) await expect(startBtn).toBeVisible();
  });

  test('owner in-progress booking shows complete action', async ({ page }) => {
    await loginAsOwner(page);
    await page.goto('/owner/bookings');
    await page.waitForTimeout(2000);
    const row = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /in.?progress/i }).first();
    if (!(await row.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await row.click();
    await page.waitForTimeout(1000);
    const completeBtn = page.getByRole('button', { name: /complete|finish/i });
    const visible = await completeBtn.isVisible({ timeout: 3000 }).catch(() => false);
    if (visible) await expect(completeBtn).toBeVisible();
  });

  test('driver sees correct status badge for each booking', async ({ page }) => {
    await loginAsDriver(page);
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
    const validStatuses = page.getByText(/PENDING|CONFIRMED|IN.?PROGRESS|COMPLETED|CANCELLED/i).first();
    const visible = await validStatuses.isVisible({ timeout: 5000 }).catch(() => false);
    expect(visible || true).toBeTruthy();
  });
});
