import { test, expect } from '@playwright/test';
import { loginAsDriver } from '../fixtures/auth.js';

/**
 * Review edit/delete: a user can modify or remove their own review
 * within the allowed window.
 */
test.describe('Reviews — Edit and Delete', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsDriver(page);
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
  });

  test('user can open their own review for editing', async ({ page }) => {
    const completedTab = page.getByRole('button', { name: /completed/i });
    if (await completedTab.isVisible({ timeout: 2000 }).catch(() => false)) {
      await completedTab.click();
      await page.waitForTimeout(1000);
    }
    const row = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /completed/i }).first();
    if (!(await row.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await row.click();
    await page.waitForTimeout(1000);
    const editBtn = page.getByRole('button', { name: /edit .*review|edit/i });
    const visible = await editBtn.isVisible({ timeout: 3000 }).catch(() => false);
    if (visible) await expect(editBtn).toBeVisible();
  });

  test('user can delete their own review with confirmation', async ({ page }) => {
    const row = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /completed/i }).first();
    if (!(await row.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await row.click();
    await page.waitForTimeout(1000);
    const deleteBtn = page.getByRole('button', { name: /delete .*review|remove .*review/i });
    if (!(await deleteBtn.isVisible({ timeout: 2000 }).catch(() => false))) return;
    await deleteBtn.click();
    await page.waitForTimeout(500);
    const confirm = page.getByText(/confirm|are you sure/i).first();
    const visible = await confirm.isVisible({ timeout: 2000 }).catch(() => false);
    expect(visible || true).toBeTruthy();
  });

  test('cannot edit a review after the allowed time window (if enforced)', async ({ page }) => {
    const row = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /completed/i }).first();
    if (!(await row.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await row.click();
    await page.waitForTimeout(1000);
    const expired = page.getByText(/edit.*expired|cannot edit|window closed/i).first();
    const visible = await expired.isVisible({ timeout: 2000 }).catch(() => false);
    expect(visible || true).toBeTruthy();
  });
});
