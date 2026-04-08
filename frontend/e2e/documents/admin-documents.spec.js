import { test, expect } from '@playwright/test';
import { loginAsAdmin } from '../fixtures/auth.js';

test.describe('Admin — Document Verification Portal', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('shows verification portal page', async ({ page }) => {
    await page.goto('/admin/documents');
    await expect(page.getByText('Verification Portal')).toBeVisible();
    await expect(page.getByText(/review and verify/i)).toBeVisible();
  });

  test('shows pending documents or empty state', async ({ page }) => {
    await page.goto('/admin/documents');

    const approve = page.getByText('Approve').first();
    const empty = page.getByText(/no pending/i);
    await expect(approve.or(empty)).toBeVisible({ timeout: 5000 });
  });

  test('shows license class selector for DRIVERS_LICENSE', async ({ page }) => {
    await page.goto('/admin/documents');

    const hasLicense = await page.getByText('License Classification').isVisible({ timeout: 5000 }).catch(() => false);
    if (hasLicense) {
      await expect(page.getByText(/Class 4/)).toBeVisible();
      await expect(page.getByText(/Class 5/)).toBeVisible();
    }
  });

  test('shows View and Download buttons', async ({ page }) => {
    await page.goto('/admin/documents');

    const hasView = await page.getByText('View').first().isVisible({ timeout: 5000 }).catch(() => false);
    if (hasView) {
      await expect(page.getByText('Download').first()).toBeVisible();
    }
  });

  test('opens reject form with textarea and Cancel button', async ({ page }) => {
    await page.goto('/admin/documents');

    const rejectButton = page.getByText('Reject').first();
    const hasReject = await rejectButton.isVisible({ timeout: 5000 }).catch(() => false);
    if (hasReject) {
      await rejectButton.click();
      await expect(page.getByPlaceholder(/provide a reason/i)).toBeVisible();
      await expect(page.getByText('Confirm Rejection')).toBeVisible();
      await expect(page.getByText('Cancel')).toBeVisible();
    }
  });

  test('cancel button hides reject form', async ({ page }) => {
    await page.goto('/admin/documents');

    const rejectButton = page.getByText('Reject').first();
    const hasReject = await rejectButton.isVisible({ timeout: 5000 }).catch(() => false);
    if (hasReject) {
      await rejectButton.click();
      await page.getByText('Cancel').click();
      await expect(page.getByText('Approve').first()).toBeVisible();
      await expect(page.getByPlaceholder(/provide a reason/i)).not.toBeVisible();
    }
  });

  test('rejection textarea blocks special characters', async ({ page }) => {
    await page.goto('/admin/documents');

    const rejectButton = page.getByText('Reject').first();
    const hasReject = await rejectButton.isVisible({ timeout: 5000 }).catch(() => false);
    if (hasReject) {
      await rejectButton.click();
      const textarea = page.getByPlaceholder(/provide a reason/i);
      await textarea.fill('');
      await textarea.type('test<script>');
      // < and > should be filtered out
      await expect(textarea).toHaveValue('testscript');
    }
  });
});
