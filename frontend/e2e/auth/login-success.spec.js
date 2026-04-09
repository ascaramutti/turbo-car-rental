import { test, expect } from '@playwright/test';
import { loginAsDriver, loginAsOwner, loginAsAdmin } from '../fixtures/auth.js';

/**
 * Successful login flows for the three seed roles.
 * Verifies JWT is stored in localStorage and the user is redirected
 * to the role-appropriate dashboard.
 */
test.describe('Auth — Login Success', () => {

  test('driver login stores token and redirects to driver area', async ({ page }) => {
    await loginAsDriver(page);
    expect(page.url()).toMatch(/\/driver\//);
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeTruthy();
  });

  test('owner login stores token and redirects to owner area', async ({ page }) => {
    await loginAsOwner(page);
    expect(page.url()).toMatch(/\/owner\//);
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeTruthy();
  });

  test('admin login stores token and redirects to admin area', async ({ page }) => {
    await loginAsAdmin(page);
    expect(page.url()).toMatch(/\/admin\//);
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeTruthy();
  });

  test('login page renders form correctly', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: /log in to turbo/i })).toBeVisible();
    await expect(page.getByPlaceholder('you@example.com')).toBeVisible();
    await expect(page.getByPlaceholder('Enter your password')).toBeVisible();
    await expect(page.getByRole('button', { name: /log in/i })).toBeVisible();
  });
});
