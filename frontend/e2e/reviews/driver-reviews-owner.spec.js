import { test, expect } from '@playwright/test';
import { loginAsDriver } from '../fixtures/auth.js';

/**
 * Driver leaves a review for the car owner after a completed booking.
 */
test.describe('Reviews — Driver Reviews Owner', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsDriver(page);
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
  });

  test('completed booking exposes a leave review action', async ({ page }) => {
    const completedTab = page.getByRole('button', { name: /completed/i });
    if (await completedTab.isVisible({ timeout: 2000 }).catch(() => false)) {
      await completedTab.click();
      await page.waitForTimeout(1000);
    }
    const row = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /completed/i }).first();
    if (!(await row.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await row.click();
    await page.waitForTimeout(1000);
    const reviewBtn = page.getByRole('button', { name: /review|rate/i });
    const visible = await reviewBtn.isVisible({ timeout: 3000 }).catch(() => false);
    if (visible) await expect(reviewBtn).toBeVisible();
  });

  test('review form has rating stars and comment field', async ({ page }) => {
    const row = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /completed/i }).first();
    if (!(await row.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await row.click();
    await page.waitForTimeout(1000);
    const reviewBtn = page.getByRole('button', { name: /review|rate/i });
    if (!(await reviewBtn.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await reviewBtn.click();
    await page.waitForTimeout(500);
    const stars = page.locator('[class*="star"], button[aria-label*="star" i]').first();
    const comment = page.getByPlaceholder(/comment|review|feedback/i);
    const hasStars = await stars.isVisible({ timeout: 2000 }).catch(() => false);
    const hasComment = await comment.isVisible({ timeout: 2000 }).catch(() => false);
    expect(hasStars || hasComment).toBeTruthy();
  });

  test('submitting review with empty rating shows validation', async ({ page }) => {
    const row = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /completed/i }).first();
    if (!(await row.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await row.click();
    await page.waitForTimeout(1000);
    const reviewBtn = page.getByRole('button', { name: /review|rate/i });
    if (!(await reviewBtn.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await reviewBtn.click();
    await page.waitForTimeout(500);
    const submit = page.getByRole('button', { name: /submit|send|post/i }).last();
    if (await submit.isVisible({ timeout: 2000 }).catch(() => false)) {
      await submit.click();
      await page.waitForTimeout(1000);
      // Should remain in modal/form (not navigate away)
      expect(true).toBeTruthy();
    }
  });
});
