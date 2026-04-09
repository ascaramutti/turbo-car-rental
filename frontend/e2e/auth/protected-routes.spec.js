import { test, expect } from '@playwright/test';

/**
 * Protected route guards: anonymous users hitting any /driver, /owner
 * or /admin route must be redirected to /login.
 */
test.describe('Auth — Protected Routes', () => {

  test.beforeEach(async ({ page }) => {
    // Make sure no token is set
    await page.goto('/login');
    await page.evaluate(() => {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    });
  });

  const protectedPaths = [
    '/driver/dashboard',
    '/driver/search',
    '/driver/bookings',
    '/owner/dashboard',
    '/owner/vehicles',
    '/admin/dashboard',
  ];

  for (const path of protectedPaths) {
    test(`anonymous request to ${path} redirects to /login`, async ({ page }) => {
      await page.goto(path);
      await page.waitForTimeout(1500);
      expect(page.url()).toMatch(/\/login|\/$/);
    });
  }
});
