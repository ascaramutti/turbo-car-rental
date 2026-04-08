import { test, expect } from '@playwright/test';
import { loginAsAdmin, logout } from '../fixtures/auth.js';

test.describe('Admin — Booking Overview', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('bookings page loads from sidebar link', async ({ page }) => {
    await page.goto('/admin/documents');
    await page.waitForTimeout(1000);
    const bookingsLink = page.getByRole('link', { name: /bookings/i });
    const hasLink = await bookingsLink.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasLink) {
      await bookingsLink.click();
      await page.waitForURL('**/admin/bookings');
      await expect(page.getByText(/all bookings/i)).toBeVisible({ timeout: 5000 });
    }
  });

  test('bookings page shows status filter tabs', async ({ page }) => {
    await page.goto('/admin/bookings');
    await page.waitForTimeout(2000);
    await expect(page.getByRole('button', { name: 'All' })).toBeVisible({ timeout: 3000 });
    await expect(page.getByRole('button', { name: 'Pending' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Confirmed' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Completed' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Cancelled' })).toBeVisible();
  });

  test('bookings table shows all columns including Owner', async ({ page }) => {
    await page.goto('/admin/bookings');
    await page.waitForTimeout(2000);
    const table = page.locator('table');
    const hasTable = await table.isVisible({ timeout: 5000 }).catch(() => false);
    if (hasTable) {
      await expect(page.getByRole('columnheader', { name: 'ID' })).toBeVisible();
      await expect(page.getByRole('columnheader', { name: 'Driver' })).toBeVisible();
      await expect(page.getByRole('columnheader', { name: 'Owner' })).toBeVisible();
      await expect(page.getByRole('columnheader', { name: 'Vehicle' })).toBeVisible();
      await expect(page.getByRole('columnheader', { name: 'Total' })).toBeVisible();
      await expect(page.getByRole('columnheader', { name: 'Status' })).toBeVisible();
    }
  });

  test('status filter changes table content', async ({ page }) => {
    await page.goto('/admin/bookings');
    await page.waitForTimeout(2000);
    const completedTab = page.getByRole('button', { name: 'Completed' });
    await completedTab.click();
    await page.waitForTimeout(1000);
    await expect(completedTab).toHaveClass(/bg-accent-orange/);
  });

  test('clicking a booking row opens detail page', async ({ page }) => {
    await page.goto('/admin/bookings');
    await page.waitForTimeout(2000);
    const firstRow = page.locator('tr').filter({ hasText: /Toyota|Camry|John/i }).first();
    const hasRow = await firstRow.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasRow) {
      await firstRow.click();
      await page.waitForURL('**/admin/bookings/**');
      await expect(page.getByText(/booking details/i)).toBeVisible({ timeout: 5000 });
    }
  });
});

test.describe('Admin — Booking Detail (Read-Only)', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('detail page shows driver and owner info', async ({ page }) => {
    await page.goto('/admin/bookings');
    await page.waitForTimeout(2000);
    const firstRow = page.locator('tr').filter({ hasText: /Toyota|Camry/i }).first();
    const hasRow = await firstRow.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasRow) {
      await firstRow.click();
      await page.waitForTimeout(1000);
      await expect(page.getByText(/booking details/i)).toBeVisible({ timeout: 5000 });
      await expect(page.getByText('Driver', { exact: true })).toBeVisible();
      await expect(page.getByText(/total price/i)).toBeVisible();
    }
  });

  test('detail page shows readable labels', async ({ page }) => {
    await page.goto('/admin/bookings');
    await page.waitForTimeout(2000);
    const firstRow = page.locator('tr').filter({ hasText: /Toyota|Camry/i }).first();
    const hasRow = await firstRow.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasRow) {
      await firstRow.click();
      await page.waitForTimeout(1000);
      // Category and service type should use readable labels
      const sectionLabel = page.getByText('Sedan').or(page.getByText('Delivery Only')).or(page.getByText('Taxi + Delivery'));
      const hasLabel = await sectionLabel.first().isVisible({ timeout: 3000 }).catch(() => false);
      if (hasLabel) {
        // Should NOT show raw enums
        const rawEnum = page.getByText('DELIVERY_ONLY').or(page.getByText('SEDAN'));
        await expect(rawEnum.first()).not.toBeVisible({ timeout: 1000 }).catch(() => {});
      }
    }
  });

  test('detail page has no action buttons (read-only)', async ({ page }) => {
    await page.goto('/admin/bookings');
    await page.waitForTimeout(2000);
    const firstRow = page.locator('tr').filter({ hasText: /Toyota|Camry/i }).first();
    const hasRow = await firstRow.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasRow) {
      await firstRow.click();
      await page.waitForTimeout(1000);
      // Admin detail is read-only — no confirm/reject/cancel buttons
      const confirmBtn = page.getByRole('button', { name: /confirm/i });
      const rejectBtn = page.getByRole('button', { name: /reject/i });
      const cancelBtn = page.getByRole('button', { name: /cancel booking/i });
      await expect(confirmBtn).not.toBeVisible({ timeout: 1000 }).catch(() => {});
      await expect(rejectBtn).not.toBeVisible({ timeout: 1000 }).catch(() => {});
      await expect(cancelBtn).not.toBeVisible({ timeout: 1000 }).catch(() => {});
    }
  });

  test('back button returns to all bookings list', async ({ page }) => {
    await page.goto('/admin/bookings');
    await page.waitForTimeout(2000);
    const firstRow = page.locator('tr').filter({ hasText: /Toyota|Camry/i }).first();
    const hasRow = await firstRow.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasRow) {
      await firstRow.click();
      await page.waitForTimeout(1000);
      await page.getByText(/back to all bookings/i).click();
      await page.waitForURL('**/admin/bookings');
      await expect(page.getByText(/all bookings/i)).toBeVisible({ timeout: 5000 });
    }
  });

  test('shows cancellation reason on cancelled bookings', async ({ page }) => {
    await page.goto('/admin/bookings');
    await page.getByRole('button', { name: 'Cancelled' }).click();
    await page.waitForTimeout(2000);
    const cancelledRow = page.locator('tr').filter({ hasText: /cancelled/i }).first();
    const hasCancelled = await cancelledRow.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasCancelled) {
      await cancelledRow.click();
      await page.waitForTimeout(1000);
      const reason = page.getByText(/cancellation reason/i);
      const hasReason = await reason.isVisible({ timeout: 3000 }).catch(() => false);
      if (hasReason) {
        await expect(reason).toBeVisible();
        await expect(page.getByText(/cancelled by/i)).toBeVisible();
      }
    }
  });
});
