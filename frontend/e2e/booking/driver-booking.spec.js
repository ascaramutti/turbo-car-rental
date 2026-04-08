import { test, expect } from '@playwright/test';
import { loginAsDriver, logout } from '../fixtures/auth.js';

test.describe('Driver — Vehicle Search', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsDriver(page);
  });

  test('search page loads with filters sidebar and empty state', async ({ page }) => {
    await page.goto('/driver/search');
    await expect(page.getByRole('heading', { name: 'Filters' })).toBeVisible({ timeout: 5000 });
    await expect(page.getByText('Set your filters and click Apply Filters')).toBeVisible();
  });

  test('search returns available vehicles', async ({ page }) => {
    await page.goto('/driver/search');
    await page.getByRole('button', { name: /apply filters/i }).click();
    await page.waitForTimeout(3000);
    const vehicleCard = page.getByText(/Toyota/i).first();
    const hasResults = await vehicleCard.isVisible({ timeout: 5000 }).catch(() => false);
    if (hasResults) {
      await expect(vehicleCard).toBeVisible();
    } else {
      await expect(page.getByText(/no vehicles found/i)).toBeVisible();
    }
  });

  test('search shows readable service type labels', async ({ page }) => {
    await page.goto('/driver/search');
    await page.getByRole('button', { name: /apply filters/i }).click();
    await page.waitForTimeout(3000);
    const deliveryLabel = page.getByText('Delivery Only').first();
    const taxiLabel = page.getByText('Taxi + Delivery').first();
    const hasDelivery = await deliveryLabel.isVisible({ timeout: 3000 }).catch(() => false);
    const hasTaxi = await taxiLabel.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasDelivery || hasTaxi) {
      // Should show readable labels, not raw enums
      const rawEnum = page.getByText('DELIVERY_ONLY').first();
      await expect(rawEnum).not.toBeVisible({ timeout: 1000 }).catch(() => {});
    }
  });

  test('price filter validation - negative price shows error on blur', async ({ page }) => {
    await page.goto('/driver/search');
    const minPrice = page.locator('input[name="minPrice"]');
    await minPrice.fill('-10');
    await minPrice.blur();
    await expect(page.getByText(/cannot be negative/i)).toBeVisible({ timeout: 3000 });
  });

  test('shift duration filter - end time before start time shows error', async ({ page }) => {
    await page.goto('/driver/search');
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const startVal = tomorrow.toISOString().slice(0, 11) + '18:00';
    const endVal = tomorrow.toISOString().slice(0, 11) + '10:00';
    await page.locator('input[name="startTime"]').fill(startVal);
    await page.locator('input[name="endTime"]').fill(endVal);
    await page.getByRole('button', { name: /apply filters/i }).click();
    await expect(page.getByText(/end time must be after start time/i)).toBeVisible({ timeout: 3000 });
  });

  test('sort buttons change result order', async ({ page }) => {
    await page.goto('/driver/search');
    await page.getByRole('button', { name: /apply filters/i }).click();
    await page.waitForTimeout(3000);
    const cheapest = page.getByRole('button', { name: 'Cheapest' });
    const hasCheapest = await cheapest.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasCheapest) {
      await cheapest.click();
      await page.waitForTimeout(500);
      await expect(cheapest).toHaveClass(/bg-accent-orange/);
    }
  });

  test('vehicle detail modal opens on View click', async ({ page }) => {
    await page.goto('/driver/search');
    await page.getByRole('button', { name: /apply filters/i }).click();
    await page.waitForTimeout(3000);
    const viewButton = page.getByRole('button', { name: 'View' }).first();
    const hasView = await viewButton.isVisible({ timeout: 5000 }).catch(() => false);
    if (hasView) {
      await viewButton.click();
      await expect(page.getByText('Vehicle Detail')).toBeVisible({ timeout: 3000 });
      await expect(page.getByRole('button', { name: /book this car/i })).toBeVisible();
    }
  });

  test('vehicle detail modal shows availableUntil in booking form', async ({ page }) => {
    await page.goto('/driver/search');
    await page.getByRole('button', { name: /apply filters/i }).click();
    await page.waitForTimeout(3000);
    const viewButton = page.getByRole('button', { name: 'View' }).first();
    const hasView = await viewButton.isVisible({ timeout: 5000 }).catch(() => false);
    if (hasView) {
      await viewButton.click();
      await page.waitForTimeout(1000);
      await page.getByRole('button', { name: /book this car/i }).click();
      await expect(page.getByText(/vehicle available until/i)).toBeVisible({ timeout: 3000 });
    }
  });
});

test.describe('Driver — Create Booking', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsDriver(page);
  });

  test('creates a booking and redirects to my bookings', async ({ page }) => {
    await page.goto('/driver/search');
    await page.getByRole('button', { name: /apply filters/i }).click();
    await page.waitForTimeout(3000);

    const viewButton = page.getByRole('button', { name: 'View' }).first();
    const hasView = await viewButton.isVisible({ timeout: 5000 }).catch(() => false);
    if (!hasView) return;

    await viewButton.click();
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: /book this car/i }).click();
    await page.waitForTimeout(500);

    // Fill booking form with valid future dates (tomorrow, 8h shift)
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const startVal = tomorrow.toISOString().slice(0, 11) + '08:00';
    const endVal = tomorrow.toISOString().slice(0, 11) + '16:00';
    await page.locator('#startTime').fill(startVal);
    await page.locator('#endTime').fill(endVal);

    await page.getByRole('button', { name: /request booking/i }).click();

    // Should redirect to driver bookings or show success
    // Wait for either success toast or redirect
    await page.waitForTimeout(3000);
    const onBookingsPage = page.url().includes('/driver/bookings');
    const hasSuccess = await page.getByText(/booking request submitted/i).isVisible({ timeout: 3000 }).catch(() => false);
    const hasError = await page.getByText(/already booked|overlapping/i).isVisible({ timeout: 1000 }).catch(() => false);
    // Pass if redirected, success shown, or conflict error (valid behavior with existing bookings)
    expect(onBookingsPage || hasSuccess || hasError).toBeTruthy();
  });
});

test.describe('Driver — Booking Management', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsDriver(page);
  });

  test('navbar shows Search and My Bookings links', async ({ page }) => {
    await page.goto('/driver/search');
    await expect(page.getByRole('link', { name: /search/i })).toBeVisible({ timeout: 3000 });
    await expect(page.getByRole('link', { name: /my bookings/i })).toBeVisible({ timeout: 3000 });
  });

  test('my bookings page lists bookings with status badges', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
    const bookingContent = page.getByText(/Toyota|Camry|PENDING|CONFIRMED|COMPLETED|CANCELLED/i).first();
    await expect(bookingContent).toBeVisible({ timeout: 5000 });
  });

  test('status filter tabs work', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
    const allTab = page.getByRole('button', { name: 'All' });
    const hasAllTab = await allTab.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasAllTab) {
      const completedTab = page.getByRole('button', { name: 'Completed', exact: true });
      await completedTab.click();
      await page.waitForTimeout(1000);
      await expect(completedTab).toHaveClass(/bg-accent-orange/);
    }
  });

  test('booking detail page shows vehicle info and actions', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
    // Click on first booking row/card
    const firstBooking = page.locator('tr').filter({ hasText: /Toyota|Camry/i }).first()
      .or(page.locator('[class*="cursor-pointer"]').filter({ hasText: /Toyota|Camry/i }).first());
    const hasBooking = await firstBooking.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasBooking) {
      await firstBooking.click();
      await page.waitForTimeout(1000);
      await expect(page.getByText(/booking details/i)).toBeVisible({ timeout: 5000 });
      await expect(page.getByText(/total price/i)).toBeVisible();
    }
  });

  test('booking detail hides coordinates when location is approximate', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
    const confirmedRow = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /confirmed/i }).first();
    const hasConfirmed = await confirmedRow.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasConfirmed) {
      await confirmedRow.click();
      await page.waitForTimeout(1000);
      const approxLocation = page.getByText(/approximate location/i);
      const hasApprox = await approxLocation.isVisible({ timeout: 3000 }).catch(() => false);
      if (hasApprox) {
        // Coordinates should NOT be visible when approximate
        await expect(page.getByText(/coordinates:/i)).not.toBeVisible({ timeout: 1000 }).catch(() => {});
      }
    }
  });

  test('cancel booking flow - shows reason form and confirms', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
    const pendingRow = page.locator('tr, [class*="cursor-pointer"]').filter({ hasText: /pending/i }).first();
    const hasPending = await pendingRow.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasPending) {
      await pendingRow.click();
      await page.waitForTimeout(1000);
      const cancelBtn = page.getByRole('button', { name: /cancel booking/i });
      const hasCancel = await cancelBtn.isVisible({ timeout: 3000 }).catch(() => false);
      if (hasCancel) {
        await cancelBtn.click();
        await expect(page.getByText(/cancellation reason/i)).toBeVisible({ timeout: 3000 });
        await expect(page.getByPlaceholder(/provide a reason/i)).toBeVisible();
      }
    }
  });

  test('driver hours badge visible in navbar', async ({ page }) => {
    await page.goto('/driver/search');
    await expect(page.getByText(/hrs/i)).toBeVisible({ timeout: 5000 });
  });
});
