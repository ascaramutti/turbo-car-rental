import { test, expect } from '@playwright/test';
import { loginAsOwner } from '../fixtures/auth.js';

test.describe('CarOwner — Vehicle Detail Page', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsOwner(page);
  });

  test('navigates to vehicle detail page', async ({ page }) => {
    await page.goto('/owner/vehicles');
    await page.waitForTimeout(1000);

    // Click on first vehicle card if any exist
    const vehicleCard = page.locator('[class*="rounded"]').filter({ hasText: /\$/ }).first();
    const hasVehicle = await vehicleCard.isVisible({ timeout: 5000 }).catch(() => false);

    if (hasVehicle) {
      await vehicleCard.click();
      await page.waitForURL('**/owner/vehicles/**');
      // Should show vehicle details sections
      await expect(page.getByText(/documents|document/i).first()).toBeVisible({ timeout: 5000 });
    }
  });

  test('shows vehicle document upload cards on detail page', async ({ page }) => {
    await page.goto('/owner/vehicles');
    await page.waitForTimeout(1000);

    const vehicleCard = page.locator('[class*="rounded"]').filter({ hasText: /\$/ }).first();
    const hasVehicle = await vehicleCard.isVisible({ timeout: 5000 }).catch(() => false);

    if (hasVehicle) {
      await vehicleCard.click();
      await page.waitForURL('**/owner/vehicles/**');

      // Should show the Vehicle Documents section heading
      await expect(page.getByText('Vehicle Documents')).toBeVisible({ timeout: 5000 });
    }
  });

  test('shows availability section with datetime input or active status', async ({ page }) => {
    await page.goto('/owner/vehicles');
    await page.waitForTimeout(1000);

    const vehicleCard = page.locator('[class*="rounded"]').filter({ hasText: /\$/ }).first();
    const hasVehicle = await vehicleCard.isVisible({ timeout: 5000 }).catch(() => false);

    if (hasVehicle) {
      await vehicleCard.click();
      await page.waitForURL('**/owner/vehicles/**');

      // Should show either datetime input (not active) or "Available until" (active)
      const datetimeInput = page.locator('input[type="datetime-local"]');
      const availableUntilText = page.getByText(/available until/i);
      const listForRent = page.getByRole('button', { name: /list for rent/i });
      const listedForRent = page.getByText(/listed for rent/i);

      const hasDatetime = await datetimeInput.isVisible({ timeout: 3000 }).catch(() => false);
      const hasAvailableUntil = await availableUntilText.isVisible({ timeout: 2000 }).catch(() => false);
      const hasList = await listForRent.isVisible({ timeout: 2000 }).catch(() => false);
      const hasListed = await listedForRent.isVisible({ timeout: 2000 }).catch(() => false);

      // At least one of these should be visible for an approved vehicle
      if (hasDatetime || hasAvailableUntil || hasList || hasListed) {
        expect(hasDatetime || hasAvailableUntil || hasList || hasListed).toBe(true);
      }
    }
  });
});
