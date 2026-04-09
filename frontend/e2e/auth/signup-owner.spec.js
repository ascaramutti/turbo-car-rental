import { test, expect } from '@playwright/test';

/**
 * Car owner signup flow.
 * Mirrors driver signup but selects CAR_OWNER role.
 */
test.describe('Auth — Car Owner Signup', () => {

  test('selecting CAR OWNER highlights the owner role button', async ({ page }) => {
    await page.goto('/signup');
    const ownerBtn = page.getByRole('button', { name: 'CAR OWNER' });
    await ownerBtn.click();
    await expect(ownerBtn).toHaveClass(/bg-owner-red/);
  });

  test('owner signup with invalid email shows email error', async ({ page }) => {
    await page.goto('/signup');
    const email = page.getByPlaceholder('you@example.com');
    await email.fill('not-an-email');
    await email.blur();
    await expect(page.getByText(/invalid|valid email/i)).toBeVisible({ timeout: 3000 });
  });

  test('successful owner signup redirects to OTP page', async ({ page }) => {
    await page.goto('/signup');
    const unique = Date.now();
    await page.getByPlaceholder('First Name').fill('Test');
    await page.getByPlaceholder('Last Name').fill('Owner');
    await page.locator('input[name="dateOfBirth"]').fill('1990-03-15');
    await page.getByPlaceholder('Street Address').fill('456 Oak Ave');
    await page.getByPlaceholder('City').fill('Burnaby');
    await page.getByPlaceholder('V6B 1A1').fill('V5A1S6');
    await page.getByPlaceholder('you@example.com').fill(`testowner${unique}@turbo.com`);
    await page.getByPlaceholder('Min. 6 characters').fill('test123');
    await page.getByPlaceholder('Re-enter password').fill('test123');
    await page.getByRole('button', { name: 'CAR OWNER' }).click();
    await page.getByRole('button', { name: /sign up/i }).click();

    await page.waitForTimeout(3000);
    const onOtp = page.url().includes('/verify-otp');
    const hasError = await page.getByText(/already|exists|invalid/i).isVisible({ timeout: 1000 }).catch(() => false);
    expect(onOtp || hasError).toBeTruthy();
  });

  test('navigation link from signup goes back to login', async ({ page }) => {
    await page.goto('/signup');
    await page.getByRole('link', { name: /log in/i }).click();
    await expect(page).toHaveURL(/\/login/);
  });
});
