import { test, expect } from '@playwright/test';
import { loginAsDriver, loginAsOwner, loginAsAdmin } from '../fixtures/auth.js';

/**
 * Role-based access control: a logged-in user with a given role
 * must not be able to access pages that belong to other roles.
 */
test.describe('Auth — Role-based Access Control', () => {

  test('driver cannot access /owner area', async ({ page }) => {
    await loginAsDriver(page);
    await page.goto('/owner/dashboard');
    await page.waitForTimeout(1500);
    // Should be redirected away from /owner (back to driver area, login or 403)
    expect(page.url()).not.toMatch(/\/owner\/dashboard$/);
  });

  test('driver cannot access /admin area', async ({ page }) => {
    await loginAsDriver(page);
    await page.goto('/admin/dashboard');
    await page.waitForTimeout(1500);
    expect(page.url()).not.toMatch(/\/admin\/dashboard$/);
  });

  test('owner cannot access /driver area', async ({ page }) => {
    await loginAsOwner(page);
    await page.goto('/driver/search');
    await page.waitForTimeout(1500);
    expect(page.url()).not.toMatch(/\/driver\/search$/);
  });

  test('owner cannot access /admin area', async ({ page }) => {
    await loginAsOwner(page);
    await page.goto('/admin/dashboard');
    await page.waitForTimeout(1500);
    expect(page.url()).not.toMatch(/\/admin\/dashboard$/);
  });

  test('admin cannot access /driver area', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/driver/search');
    await page.waitForTimeout(1500);
    expect(page.url()).not.toMatch(/\/driver\/search$/);
  });
});
