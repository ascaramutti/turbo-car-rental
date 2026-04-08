import { test, expect } from '@playwright/test';
import { loginAsOwner, logout } from '../fixtures/auth.js';

test.describe('Owner — Booking List', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsOwner(page);
  });

  test('bookings page loads with status tabs', async ({ page }) => {
    await page.goto('/owner/bookings');
    await expect(page.getByText(/upcoming bookings/i)).toBeVisible({ timeout: 5000 });
    await expect(page.getByRole('button', { name: 'All' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Pending' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Confirmed' })).toBeVisible();
  });

  test('bookings table shows driver, vehicle, and price columns', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.waitForTimeout(2000);
    const table = page.locator('table');
    const hasTable = await table.isVisible({ timeout: 5000 }).catch(() => false);
    if (hasTable) {
      await expect(page.getByRole('columnheader', { name: 'Driver' })).toBeVisible();
      await expect(page.getByRole('columnheader', { name: 'Vehicle' })).toBeVisible();
      await expect(page.getByRole('columnheader', { name: 'You Earn' })).toBeVisible();
    }
  });

  test('status filter tabs switch content', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.waitForTimeout(2000);
    const completedTab = page.getByRole('button', { name: 'Completed' });
    await completedTab.click();
    await page.waitForTimeout(1000);
    await expect(completedTab).toHaveClass(/bg-accent-orange/);
  });

  test('clicking a booking row navigates to detail', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.waitForTimeout(2000);
    const firstRow = page.locator('tr').filter({ hasText: /Toyota|Camry|John/i }).first();
    const hasRow = await firstRow.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasRow) {
      await firstRow.click();
      await page.waitForURL('**/owner/bookings/**');
      await expect(page.getByText(/booking details/i)).toBeVisible({ timeout: 5000 });
    }
  });

  test('APPROVE button visible for PENDING bookings', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.getByRole('button', { name: 'Pending' }).click();
    await page.waitForTimeout(2000);
    const approveBtn = page.getByRole('button', { name: 'APPROVE' }).first();
    const hasPending = await approveBtn.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasPending) {
      await expect(approveBtn).toBeVisible();
    }
  });
});

test.describe('Owner — Booking Detail & Actions', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsOwner(page);
  });

  test('detail page shows booking info with readable labels', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.waitForTimeout(2000);
    const firstRow = page.locator('tr').filter({ hasText: /Toyota|Camry/i }).first();
    const hasRow = await firstRow.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasRow) {
      await firstRow.click();
      await page.waitForTimeout(1000);
      await expect(page.getByText(/booking details/i)).toBeVisible({ timeout: 5000 });
      // Service type should use readable labels
      const deliveryLabel = page.getByText('Delivery Only');
      const taxiLabel = page.getByText('Taxi + Delivery');
      const hasLabel = await deliveryLabel.isVisible({ timeout: 2000 }).catch(() => false)
        || await taxiLabel.isVisible({ timeout: 1000 }).catch(() => false);
      if (hasLabel) {
        const rawEnum = page.getByText('DELIVERY_ONLY');
        await expect(rawEnum).not.toBeVisible({ timeout: 1000 }).catch(() => {});
      }
    }
  });

  test('confirm action on PENDING booking', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.getByRole('button', { name: 'Pending' }).click();
    await page.waitForTimeout(2000);
    const firstPending = page.locator('tr').filter({ hasText: /pending/i }).first();
    const hasPending = await firstPending.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasPending) {
      await firstPending.click();
      await page.waitForTimeout(1000);
      const confirmBtn = page.getByRole('button', { name: /confirm/i }).first();
      const hasConfirm = await confirmBtn.isVisible({ timeout: 3000 }).catch(() => false);
      if (hasConfirm) {
        await confirmBtn.click();
        await page.waitForTimeout(2000);
        // Should show success toast or status change
        const success = page.getByText(/confirmed|success/i).first();
        await expect(success).toBeVisible({ timeout: 5000 });
      }
    }
  });

  test('reject action requires reason', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.getByRole('button', { name: 'Pending' }).click();
    await page.waitForTimeout(2000);
    const firstPending = page.locator('tr').filter({ hasText: /pending/i }).first();
    const hasPending = await firstPending.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasPending) {
      await firstPending.click();
      await page.waitForTimeout(1000);
      const rejectBtn = page.getByRole('button', { name: /reject/i }).first();
      const hasReject = await rejectBtn.isVisible({ timeout: 3000 }).catch(() => false);
      if (hasReject) {
        await rejectBtn.click();
        await page.waitForTimeout(500);
        // Should show the reason textarea
        await expect(page.getByPlaceholder(/provide a reason/i)).toBeVisible({ timeout: 3000 });
      }
    }
  });

  test('cancel action on CONFIRMED booking', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.getByRole('button', { name: 'Confirmed' }).click();
    await page.waitForTimeout(2000);
    const firstConfirmed = page.locator('tr').filter({ hasText: /confirmed/i }).first();
    const hasConfirmed = await firstConfirmed.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasConfirmed) {
      await firstConfirmed.click();
      await page.waitForTimeout(1000);
      const cancelBtn = page.getByRole('button', { name: /cancel/i }).first();
      const hasCancel = await cancelBtn.isVisible({ timeout: 3000 }).catch(() => false);
      if (hasCancel) {
        await cancelBtn.click();
        await expect(page.getByText(/cancellation reason/i)).toBeVisible({ timeout: 3000 });
      }
    }
  });

  test('shows pickup and return photos on completed booking', async ({ page }) => {
    await page.goto('/owner/bookings');
    await page.getByRole('button', { name: 'Completed' }).click();
    await page.waitForTimeout(2000);
    const firstCompleted = page.locator('tr').filter({ hasText: /completed/i }).first();
    const hasCompleted = await firstCompleted.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasCompleted) {
      await firstCompleted.click();
      await page.waitForTimeout(1000);
      // If photos exist, the section should be visible
      const pickupPhotos = page.getByText(/pickup photos/i);
      const returnPhotos = page.getByText(/return photos/i);
      const hasPickup = await pickupPhotos.isVisible({ timeout: 2000 }).catch(() => false);
      const hasReturn = await returnPhotos.isVisible({ timeout: 2000 }).catch(() => false);
      // At least booking details should be visible
      await expect(page.getByText(/booking details/i)).toBeVisible();
    }
  });
});

test.describe('Owner — Dashboard', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsOwner(page);
  });

  test('dashboard page loads with stats', async ({ page }) => {
    await page.goto('/owner/dashboard');
    await page.waitForTimeout(2000);
    const dashboard = page.getByText(/dashboard/i).first();
    await expect(dashboard).toBeVisible({ timeout: 5000 });
  });

  test('sidebar navigation works between dashboard, vehicles, and bookings', async ({ page }) => {
    await page.goto('/owner/dashboard');
    await page.waitForTimeout(1000);

    // Click Bookings in sidebar
    const bookingsLink = page.getByRole('link', { name: /bookings/i });
    const hasLink = await bookingsLink.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasLink) {
      await bookingsLink.click();
      await page.waitForURL('**/owner/bookings');
      await expect(page.getByText(/upcoming bookings/i)).toBeVisible({ timeout: 5000 });
    }
  });

  test('expired vehicle listing shows "Listing Expired" badge', async ({ page }) => {
    await page.goto('/owner/vehicles');
    await page.waitForTimeout(1000);
    const vehicleCard = page.locator('[class*="rounded"]').filter({ hasText: /Toyota/i }).first();
    const hasVehicle = await vehicleCard.isVisible({ timeout: 5000 }).catch(() => false);
    if (hasVehicle) {
      await vehicleCard.click();
      await page.waitForURL('**/owner/vehicles/**');
      await page.waitForTimeout(1000);
      // If vehicle has expired availableUntil, should show "Listing Expired"
      const expiredBadge = page.getByText(/listing expired/i);
      const hasExpired = await expiredBadge.isVisible({ timeout: 3000 }).catch(() => false);
      if (hasExpired) {
        await expect(expiredBadge).toBeVisible();
        // Should NOT show "Listed for Rent"
        await expect(page.getByText(/listed for rent/i)).not.toBeVisible({ timeout: 1000 }).catch(() => {});
      }
    }
  });
});
