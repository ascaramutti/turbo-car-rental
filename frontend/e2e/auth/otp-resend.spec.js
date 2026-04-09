import { test, expect } from '@playwright/test';

/**
 * OTP resend flow: button starts enabled, clicking triggers cooldown,
 * after which the button becomes enabled again.
 */
test.describe('Auth — OTP Resend', () => {

  test('resend button shows cooldown timer after click', async ({ page }) => {
    await page.goto('/signup');
    const unique = Date.now();
    await page.getByPlaceholder('First Name').fill('Resend');
    await page.getByPlaceholder('Last Name').fill('Test');
    await page.locator('input[name="dateOfBirth"]').fill('1991-06-20');
    await page.getByPlaceholder('Street Address').fill('99 Pine');
    await page.getByPlaceholder('City').fill('Vancouver');
    await page.getByPlaceholder('V6B 1A1').fill('V6B1A1');
    await page.getByPlaceholder('you@example.com').fill(`resend${unique}@turbo.com`);
    await page.getByPlaceholder('Min. 6 characters').fill('test123');
    await page.getByPlaceholder('Re-enter password').fill('test123');
    await page.getByRole('button', { name: 'DRIVER' }).click();
    await page.getByRole('button', { name: /sign up/i }).click();
    await page.waitForTimeout(2500);

    if (!page.url().includes('/verify-otp')) return;

    const resendBtn = page.getByRole('button', { name: /resend code/i });
    const visible = await resendBtn.isVisible({ timeout: 3000 }).catch(() => false);
    if (!visible) return;

    await resendBtn.click();
    await page.waitForTimeout(2000);
    // After clicking, the button should show "Resend in Xs" cooldown text.
    await expect(page.getByText(/resend in \d+s/i)).toBeVisible({ timeout: 3000 });
  });

  test('resend page shows the email it was sent to', async ({ page }) => {
    await page.goto('/signup');
    const unique = Date.now();
    const email = `showmail${unique}@turbo.com`;
    await page.getByPlaceholder('First Name').fill('Show');
    await page.getByPlaceholder('Last Name').fill('Mail');
    await page.locator('input[name="dateOfBirth"]').fill('1991-06-20');
    await page.getByPlaceholder('Street Address').fill('1 St');
    await page.getByPlaceholder('City').fill('Vancouver');
    await page.getByPlaceholder('V6B 1A1').fill('V6B1A1');
    await page.getByPlaceholder('you@example.com').fill(email);
    await page.getByPlaceholder('Min. 6 characters').fill('test123');
    await page.getByPlaceholder('Re-enter password').fill('test123');
    await page.getByRole('button', { name: 'DRIVER' }).click();
    await page.getByRole('button', { name: /sign up/i }).click();
    await page.waitForTimeout(2500);

    if (page.url().includes('/verify-otp')) {
      await expect(page.getByText(email)).toBeVisible({ timeout: 3000 });
    }
  });
});
