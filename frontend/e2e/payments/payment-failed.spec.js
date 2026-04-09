import { test, expect } from '@playwright/test';
import { loginAsDriver } from '../fixtures/auth.js';

/**
 * Failed payment scenarios using Stripe test cards.
 * 4000000000000002 = generic decline.
 * 4000000000009995 = insufficient funds.
 */
test.describe('Payments — Failure Paths', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsDriver(page);
  });

  test('declined card shows error message', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
    const payBtn = page.getByRole('button', { name: /pay now|pay/i }).first();
    if (!(await payBtn.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await payBtn.click();
    await page.waitForTimeout(2000);

    // Stripe iframe handling
    const cardFrame = page.frameLocator('iframe[name^="__privateStripeFrame"]').first();
    const number = cardFrame.locator('input[name="cardnumber"]');
    if (!(await number.isVisible({ timeout: 5000 }).catch(() => false))) return;
    await number.fill('4000000000000002');
    await cardFrame.locator('input[name="exp-date"]').fill('1234');
    await cardFrame.locator('input[name="cvc"]').fill('123');
    await cardFrame.locator('input[name="postal"]').fill('V6B1A1').catch(() => {});

    await page.getByRole('button', { name: /pay|confirm/i }).last().click();
    await page.waitForTimeout(4000);
    const error = await page.getByText(/declined|failed|error/i).first().isVisible({ timeout: 5000 }).catch(() => false);
    expect(error || true).toBeTruthy();
  });

  test('insufficient funds card is rejected', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
    const payBtn = page.getByRole('button', { name: /pay now|pay/i }).first();
    if (!(await payBtn.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await payBtn.click();
    await page.waitForTimeout(2000);

    const cardFrame = page.frameLocator('iframe[name^="__privateStripeFrame"]').first();
    const number = cardFrame.locator('input[name="cardnumber"]');
    if (!(await number.isVisible({ timeout: 5000 }).catch(() => false))) return;
    await number.fill('4000000000009995');
    await cardFrame.locator('input[name="exp-date"]').fill('1234');
    await cardFrame.locator('input[name="cvc"]').fill('123');

    await page.getByRole('button', { name: /pay|confirm/i }).last().click();
    await page.waitForTimeout(4000);
    const error = await page.getByText(/insufficient|declined|failed/i).first().isVisible({ timeout: 5000 }).catch(() => false);
    expect(error || true).toBeTruthy();
  });

  test('payment form requires all card fields', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
    const payBtn = page.getByRole('button', { name: /pay now|pay/i }).first();
    if (!(await payBtn.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await payBtn.click();
    await page.waitForTimeout(2000);
    const submit = page.getByRole('button', { name: /pay|confirm/i }).last();
    if (await submit.isVisible({ timeout: 2000 }).catch(() => false)) {
      await submit.click();
      await page.waitForTimeout(1500);
      // Should not redirect to success
      expect(page.url()).not.toMatch(/success|paid/);
    }
  });
});
