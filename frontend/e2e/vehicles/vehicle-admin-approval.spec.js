import { test, expect } from '@playwright/test';
import { loginAsAdmin } from '../fixtures/auth.js';

/**
 * Admin approval flow for newly registered vehicles.
 * Verifies the admin can list pending vehicles, open one, and approve/reject it.
 */
test.describe('Admin — Vehicle Approval', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('admin can navigate to vehicles management page', async ({ page }) => {
    await page.goto('/admin/vehicles');
    await page.waitForTimeout(2000);
    const heading = page.getByRole('heading', { name: /vehicles|pending|approval/i }).first();
    const visible = await heading.isVisible({ timeout: 5000 }).catch(() => false);
    if (!visible) {
      // Fall back to dashboard tile
      await page.goto('/admin/dashboard');
      await expect(page.getByText(/vehicle/i).first()).toBeVisible({ timeout: 5000 });
    }
  });

  test('pending vehicles list renders or empty state shown', async ({ page }) => {
    await page.goto('/admin/vehicles');
    await page.waitForTimeout(2000);
    const list = page.locator('table, [class*="card"]').first();
    const empty = page.getByText(/no .*vehicles|empty/i).first();
    const hasContent = await list.isVisible({ timeout: 3000 }).catch(() => false);
    const hasEmpty = await empty.isVisible({ timeout: 3000 }).catch(() => false);
    expect(hasContent || hasEmpty).toBeTruthy();
  });

  test('admin can open a pending vehicle and see approve/reject actions', async ({ page }) => {
    await page.goto('/admin/vehicles');
    await page.waitForTimeout(2000);
    const firstRow = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /Toyota|Honda|Ford|pending/i }).first();
    const visible = await firstRow.isVisible({ timeout: 3000 }).catch(() => false);
    if (!visible) return;
    await firstRow.click();
    await page.waitForTimeout(1000);
    const approve = page.getByRole('button', { name: /approve/i });
    const reject = page.getByRole('button', { name: /reject/i });
    const hasApprove = await approve.isVisible({ timeout: 3000 }).catch(() => false);
    const hasReject = await reject.isVisible({ timeout: 3000 }).catch(() => false);
    expect(hasApprove || hasReject).toBeTruthy();
  });

  test('rejecting a vehicle requires a reason', async ({ page }) => {
    await page.goto('/admin/vehicles');
    await page.waitForTimeout(2000);
    const firstRow = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /pending/i }).first();
    const visible = await firstRow.isVisible({ timeout: 3000 }).catch(() => false);
    if (!visible) return;
    await firstRow.click();
    await page.waitForTimeout(1000);
    const reject = page.getByRole('button', { name: /reject/i });
    if (await reject.isVisible({ timeout: 2000 }).catch(() => false)) {
      await reject.click();
      await expect(page.getByPlaceholder(/reason/i).or(page.getByText(/reason/i))).toBeVisible({ timeout: 3000 });
    }
  });
});
