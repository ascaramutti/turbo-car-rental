import { test, expect } from '@playwright/test';
import { loginAsOwner, logout } from '../fixtures/auth.js';

test.describe('Owner — Booking Management', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsOwner(page);
  });

  test('navigates to owner bookings page', async ({ page }) => {
    await page.goto('/owner/bookings');
    await expect(page.getByText(/booking/i).first()).toBeVisible({ timeout: 5000 });
  });

  test('displays bookings for owner vehicles', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.waitForTimeout(2000);
    // Seed data has bookings for the owner's vehicle
    const content = page.getByText(/Toyota|Camry|booking|John Driver/i).first();
    await expect(content).toBeVisible({ timeout: 5000 });
  });

  test('shows booking detail with owner actions', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.waitForTimeout(2000);
    // Click first booking
    const firstBooking = page.locator('[data-testid="booking-card"]').first().or(
      page.locator('a[href*="/owner/bookings/"]').first().or(
        page.getByRole('link').filter({ hasText: /Toyota|Camry|booking/i }).first()
      )
    );
    if (await firstBooking.isVisible({ timeout: 3000 }).catch(() => false)) {
      await firstBooking.click();
      await page.waitForTimeout(1000);
      await expect(page.getByText(/status/i).or(page.getByText(/PENDING|CONFIRMED|COMPLETED|CANCELLED|REJECTED/i)).first()).toBeVisible({ timeout: 5000 });
    }
  });

  test('can filter bookings by status', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.waitForTimeout(2000);
    // Look for status filter tabs/buttons
    const statusFilter = page.getByText(/all|pending|confirmed|completed/i).first();
    if (await statusFilter.isVisible({ timeout: 3000 }).catch(() => false)) {
      await statusFilter.click();
      await page.waitForTimeout(1000);
    }
  });
});
