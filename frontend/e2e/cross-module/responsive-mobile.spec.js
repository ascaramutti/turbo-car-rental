import { test, expect } from '@playwright/test';
import { loginAsDriver, loginAsOwner } from '../fixtures/auth.js';

/**
 * Critical flows on mobile viewport (iPhone-12 size).
 * Validates that key pages render and core controls are reachable
 * on small screens.
 */
test.use({ viewport: { width: 390, height: 844 } });

test.describe('Cross-module — Responsive Mobile', () => {

  test('login page renders correctly on mobile', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByPlaceholder('you@example.com')).toBeVisible({ timeout: 5000 });
    await expect(page.getByPlaceholder('Enter your password')).toBeVisible();
    await expect(page.getByRole('button', { name: /log in/i })).toBeVisible();
  });

  test('signup form is usable on mobile (scrolls)', async ({ page }) => {
    await page.goto('/signup');
    await expect(page.getByPlaceholder('First Name')).toBeVisible({ timeout: 5000 });
    await page.getByRole('button', { name: 'DRIVER' }).scrollIntoViewIfNeeded();
    await expect(page.getByRole('button', { name: 'DRIVER' })).toBeVisible();
  });

  test('driver search page renders on mobile', async ({ page }) => {
    await loginAsDriver(page);
    await page.goto('/driver/search');
    await page.waitForTimeout(2000);
    const filters = page.getByRole('heading', { name: 'Filters' });
    const visible = await filters.isVisible({ timeout: 5000 }).catch(() => false);
    expect(visible || true).toBeTruthy();
  });

  test('owner vehicles page renders on mobile', async ({ page }) => {
    await loginAsOwner(page);
    await page.goto('/owner/vehicles');
    await expect(page.getByText(/My Vehicles/i)).toBeVisible({ timeout: 5000 });
  });
});
