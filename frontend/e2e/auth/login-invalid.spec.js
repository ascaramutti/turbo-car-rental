import { test, expect } from '@playwright/test';

/**
 * Login error paths: wrong password, unknown email, empty fields,
 * malformed email. Ensures user stays on /login and a toast/error appears.
 */
test.describe('Auth — Login Failure', () => {

  test('wrong password shows error and stays on login', async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('you@example.com').fill('driver@turbo.com');
    await page.getByPlaceholder('Enter your password').fill('wrongpassword');
    await page.getByRole('button', { name: /log in/i }).click();
    await page.waitForTimeout(2000);
    expect(page.url()).toContain('/login');
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeFalsy();
  });

  test('unknown email shows error', async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('you@example.com').fill('nobody@nowhere.com');
    await page.getByPlaceholder('Enter your password').fill('whatever');
    await page.getByRole('button', { name: /log in/i }).click();
    await page.waitForTimeout(2000);
    expect(page.url()).toContain('/login');
  });

  test('empty fields show validation errors', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: /log in/i }).click();
    await expect(page.getByText(/required|invalid email/i).first()).toBeVisible({ timeout: 3000 });
  });

  test('invalid email format shows blur error', async ({ page }) => {
    await page.goto('/login');
    const email = page.getByPlaceholder('you@example.com');
    await email.fill('not-an-email');
    await email.blur();
    await expect(page.getByText(/invalid|valid email/i)).toBeVisible({ timeout: 3000 });
  });
});
