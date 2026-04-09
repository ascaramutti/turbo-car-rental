import { test, expect } from '@playwright/test';
import { loginAsDriver, logout } from '../fixtures/auth.js';

/**
 * Logout flow: token cleared from localStorage and protected pages
 * become inaccessible.
 */
test.describe('Auth — Logout', () => {

  test('logout clears token from localStorage', async ({ page }) => {
    await loginAsDriver(page);
    await logout(page);
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeFalsy();
  });

  test('after logout, protected route redirects to login', async ({ page }) => {
    await loginAsDriver(page);
    await logout(page);
    await page.goto('/driver/dashboard');
    await page.waitForTimeout(1500);
    expect(page.url()).toMatch(/\/login|\/$/);
  });

  test('logout via UI button (if present in navbar)', async ({ page }) => {
    await loginAsDriver(page);
    const logoutBtn = page.getByRole('button', { name: /log ?out|sign ?out/i });
    const visible = await logoutBtn.isVisible({ timeout: 2000 }).catch(() => false);
    if (visible) {
      await logoutBtn.click();
      await page.waitForTimeout(1500);
      const token = await page.evaluate(() => localStorage.getItem('token'));
      expect(token).toBeFalsy();
    }
  });
});
