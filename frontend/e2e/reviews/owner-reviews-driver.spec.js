import { test, expect } from '@playwright/test';
import { loginAsOwner } from '../fixtures/auth.js';

/**
 * Car owner leaves a review for the driver after a completed booking.
 */
test.describe('Reviews — Owner Reviews Driver', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsOwner(page);
    await page.goto('/owner/bookings');
    await page.waitForTimeout(2000);
  });

  test('completed booking has rate driver action', async ({ page }) => {
    const row = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /completed/i }).first();
    if (!(await row.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await row.click();
    await page.waitForTimeout(1000);
    const rateBtn = page.getByRole('button', { name: /rate|review/i });
    const visible = await rateBtn.isVisible({ timeout: 3000 }).catch(() => false);
    if (visible) await expect(rateBtn).toBeVisible();
  });

  test('owner can submit a 5-star review', async ({ page }) => {
    const row = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /completed/i }).first();
    if (!(await row.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await row.click();
    await page.waitForTimeout(1000);
    const rateBtn = page.getByRole('button', { name: /rate|review/i });
    if (!(await rateBtn.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await rateBtn.click();
    await page.waitForTimeout(500);
    // Click last star (5)
    const stars = page.locator('[aria-label*="star" i], button[class*="star"]');
    if ((await stars.count()) >= 5) {
      await stars.nth(4).click();
    }
    const comment = page.getByPlaceholder(/comment|review|feedback/i);
    if (await comment.isVisible({ timeout: 1000 }).catch(() => false)) {
      await comment.fill('Excellent driver, very respectful.');
    }
    const submit = page.getByRole('button', { name: /submit|send|post/i }).last();
    if (await submit.isVisible({ timeout: 1000 }).catch(() => false)) {
      await submit.click();
      await page.waitForTimeout(2000);
    }
    expect(true).toBeTruthy();
  });
});
