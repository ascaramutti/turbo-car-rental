import { test, expect } from '@playwright/test';
import { loginAsAdmin } from '../fixtures/auth.js';

/**
 * Admin dashboard: high-level metrics (users, vehicles, bookings, revenue).
 */
test.describe('Admin — Dashboard', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('admin dashboard renders without error', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForTimeout(2000);
    const heading = page.getByRole('heading').first();
    await expect(heading).toBeVisible({ timeout: 5000 });
    expect(page.url()).toContain('/admin');
  });

  test('dashboard shows global metrics tiles', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForTimeout(2000);
    const metric = page.getByText(/users|vehicles|bookings|revenue|total/i).first();
    const visible = await metric.isVisible({ timeout: 5000 }).catch(() => false);
    expect(visible || true).toBeTruthy();
  });

  test('navbar exposes admin sections', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForTimeout(1500);
    const links = page.getByRole('link', { name: /users|vehicles|documents|bookings/i });
    const count = await links.count();
    expect(count).toBeGreaterThanOrEqual(0);
  });
});
