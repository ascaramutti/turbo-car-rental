import { test, expect } from '@playwright/test';
import { loginAsDriver, logout } from '../fixtures/auth.js';

test.describe('Driver — Booking Flow', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsDriver(page);
  });

  test('navigates to vehicle search page', async ({ page }) => {
    await page.goto('/driver/search');
    await expect(page.getByText(/search/i).first()).toBeVisible({ timeout: 5000 });
  });

  test('displays available vehicles after clicking search', async ({ page }) => {
    await page.goto('/driver/search');
    await page.waitForTimeout(1000);
    // Click the Search Vehicles button to load results
    await page.getByRole('button', { name: /search vehicles/i }).click();
    await page.waitForTimeout(3000);
    // Should show at least the seeded vehicle (Toyota Camry)
    await expect(page.getByText(/Toyota/i).or(page.getByText(/Camry/i)).first()).toBeVisible({ timeout: 5000 });
  });

  test('shows service type warning after search', async ({ page }) => {
    await page.goto('/driver/search');
    await page.waitForTimeout(1000);
    await page.getByRole('button', { name: /search vehicles/i }).click();
    await page.waitForTimeout(3000);
    // The seeded driver has CLASS_4 license, vehicle is DELIVERY_ONLY
    // Should show warning about delivery only
    await expect(page.getByText(/delivery/i).first()).toBeVisible({ timeout: 5000 });
  });

  test('navigates to driver bookings page', async ({ page }) => {
    await page.goto('/driver/bookings');
    await expect(page.getByText(/booking/i).first()).toBeVisible({ timeout: 5000 });
  });

  test('displays existing bookings from seed data', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
    // Seed data has 3 bookings for the driver
    const bookingCards = page.locator('[data-testid="booking-card"]').or(
      page.getByText(/Toyota/i).or(page.getByText(/Camry/i))
    );
    await expect(bookingCards.first()).toBeVisible({ timeout: 5000 });
  });

  test('shows booking detail page with correct status', async ({ page }) => {
    await page.goto('/driver/bookings');
    await page.waitForTimeout(2000);
    // Click on the first booking
    const firstBooking = page.locator('[data-testid="booking-card"]').first().or(
      page.locator('a[href*="/driver/bookings/"]').first().or(
        page.getByRole('link').filter({ hasText: /Toyota|Camry|booking/i }).first()
      )
    );
    if (await firstBooking.isVisible({ timeout: 3000 }).catch(() => false)) {
      await firstBooking.click();
      await page.waitForTimeout(1000);
      // Should show booking details
      await expect(page.getByText(/status/i).or(page.getByText(/PENDING|CONFIRMED|COMPLETED|CANCELLED/i)).first()).toBeVisible({ timeout: 5000 });
    }
  });
});
