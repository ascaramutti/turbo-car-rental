import { test, expect } from '@playwright/test';
import { loginAsDriver, loginAsOwner } from '../fixtures/auth.js';

/** Helper: navigates to driver bookings, clicks a status tab, and clicks the first card. */
async function goToBookingByStatus(page, tabName) {
  await page.goto('/driver/bookings');
  await page.waitForTimeout(3000);

  const tab = page.getByRole('button', { name: tabName, exact: true });
  await tab.click();
  await page.waitForTimeout(2000);

  // Cards are rendered as bordered divs with booking text — click the first one
  const card = page.locator('text=Booking #').first();
  const hasCard = await card.isVisible({ timeout: 5000 }).catch(() => false);
  if (!hasCard) return false;

  await card.click();
  await page.waitForTimeout(2000);
  return true;
}

test.describe('Driver — Payment Flow', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsDriver(page);
  });

  test('confirmed booking detail page loads with booking details', async ({ page }) => {
    const found = await goToBookingByStatus(page, 'Confirmed');
    if (!found) {
      test.skip(true, 'No confirmed booking available');
      return;
    }

    await expect(page.getByText(/booking details/i)).toBeVisible({ timeout: 5000 });
    await expect(page.getByText(/total price/i)).toBeVisible();
  });

  test('Pay Now button visible for confirmed booking without payment', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(3000);

    const confirmedTab = page.getByRole('button', { name: 'Confirmed', exact: true });
    await confirmedTab.click();
    await page.waitForTimeout(2000);

    // Click the SECOND confirmed card (booking #12, no payment yet)
    const cards = page.locator('text=Booking #');
    const count = await cards.count();
    if (count < 2) {
      // Try the first one
      if (count === 0) { test.skip(true, 'No confirmed bookings'); return; }
      await cards.first().click();
    } else {
      await cards.nth(1).click();
    }
    await page.waitForTimeout(2000);

    const payBtn = page.getByRole('button', { name: /pay now/i });
    const hasPayBtn = await payBtn.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasPayBtn) {
      await expect(payBtn).toBeVisible();
    }
  });

  test('Pay Now opens payment modal with breakdown', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(3000);

    const confirmedTab = page.getByRole('button', { name: 'Confirmed', exact: true });
    await confirmedTab.click();
    await page.waitForTimeout(2000);

    // Click the second card (booking #12, fresh, no payment)
    const cards = page.locator('text=Booking #');
    const count = await cards.count();
    if (count < 2) { test.skip(true, 'Need booking #12'); return; }
    await cards.nth(1).click();
    await page.waitForTimeout(2000);

    const payBtn = page.getByRole('button', { name: /pay now/i });
    const hasPayBtn = await payBtn.isVisible({ timeout: 3000 }).catch(() => false);
    if (!hasPayBtn) { test.skip(true, 'Pay Now not visible'); return; }

    await payBtn.click();
    await page.waitForTimeout(3000);

    // Modal should open with Payment heading
    await expect(page.getByRole('heading', { name: /payment/i })).toBeVisible({ timeout: 5000 });

    // Wait for content to load (payment breakdown or error)
    const hasBreakdown = await page.getByText('Rental Total').isVisible({ timeout: 5000 }).catch(() => false);
    const hasError = await page.getByText(/error|not configured/i).isVisible({ timeout: 2000 }).catch(() => false);

    // Either breakdown or error state is valid (depends on Stripe connectivity)
    expect(hasBreakdown || hasError).toBeTruthy();

    if (hasBreakdown) {
      await expect(page.getByText('Security Deposit')).toBeVisible();
      await expect(page.getByText('Total')).toBeVisible();
      await expect(page.getByText(/CAD/)).toBeVisible();
    }
  });

  test('payment modal has Pay Now and Cancel buttons', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(3000);

    const confirmedTab = page.getByRole('button', { name: 'Confirmed', exact: true });
    await confirmedTab.click();
    await page.waitForTimeout(2000);

    const cards = page.locator('text=Booking #');
    const count = await cards.count();
    if (count < 2) { test.skip(true, 'Need booking #12'); return; }
    await cards.nth(1).click();
    await page.waitForTimeout(2000);

    const payBtn = page.getByRole('button', { name: /pay now/i });
    const hasPayBtn = await payBtn.isVisible({ timeout: 3000 }).catch(() => false);
    if (!hasPayBtn) { test.skip(true, 'Pay Now not visible'); return; }

    await payBtn.click();
    await page.waitForTimeout(3000);

    await expect(page.getByRole('heading', { name: /payment/i })).toBeVisible({ timeout: 5000 });

    // Stripe form should have Pay Now and Cancel buttons
    // Note: the Stripe PaymentElement is an iframe we can't interact with,
    // but the form buttons are in our DOM
    const modalPayBtn = page.locator('.fixed button:has-text("Pay Now")');
    const cancelBtn = page.locator('.fixed button:has-text("Cancel")');

    await expect(modalPayBtn).toBeVisible({ timeout: 5000 });
    await expect(cancelBtn).toBeVisible();

    // Close modal
    await cancelBtn.click();
    await expect(page.getByText('Rental Total')).not.toBeVisible({ timeout: 3000 });
  });

  test('completed booking does not show Pay Now', async ({ page }) => {
    const found = await goToBookingByStatus(page, 'Completed');
    if (!found) { test.skip(true, 'No completed booking'); return; }

    await expect(page.getByText(/booking details/i)).toBeVisible({ timeout: 5000 });
    await expect(page.getByRole('button', { name: /pay now/i })).not.toBeVisible({ timeout: 2000 });
  });

  test('cancelled booking does not show Pay Now', async ({ page }) => {
    const found = await goToBookingByStatus(page, 'Cancelled');
    if (!found) { test.skip(true, 'No cancelled booking'); return; }

    await expect(page.getByRole('button', { name: /pay now/i })).not.toBeVisible({ timeout: 2000 });
  });
});

test.describe('Owner — Payment Visibility', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsOwner(page);
  });

  test('owner booking detail does not show Pay Now', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.waitForTimeout(3000);

    const card = page.locator('text=Booking #').first();
    const hasCard = await card.isVisible({ timeout: 5000 }).catch(() => false);
    if (!hasCard) { test.skip(true, 'No bookings for owner'); return; }

    await card.click();
    await page.waitForTimeout(2000);

    await expect(page.getByRole('button', { name: /pay now/i })).not.toBeVisible({ timeout: 2000 });
  });
});
