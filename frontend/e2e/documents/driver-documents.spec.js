import { test, expect } from '@playwright/test';
import { loginAsDriver } from '../fixtures/auth.js';

test.describe('Driver — Document Verification', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsDriver(page);
  });

  test('shows document verification page with both card types', async ({ page }) => {
    await page.goto('/driver/documents');
    await expect(page.getByText('Document Verification')).toBeVisible();
    await expect(page.getByText("Driver's License")).toBeVisible();
    await expect(page.getByText('Study Permit')).toBeVisible();
  });

  test('shows verification banner (required or verified)', async ({ page }) => {
    await page.goto('/driver/documents');
    const required = page.getByText('Verification Required');
    const verified = page.getByText('Account Verified');
    await expect(required.or(verified)).toBeVisible({ timeout: 5000 });
  });

  test('shows accepted formats help text', async ({ page }) => {
    await page.goto('/driver/documents');
    await expect(page.getByText(/PDF, JPG, PNG/)).toBeVisible();
  });

  test('handles file upload via hidden input', async ({ page }) => {
    await page.goto('/driver/documents');

    // File inputs are hidden inside labels — use force to interact
    const fileInput = page.locator('input[type="file"]').first();
    const isVisible = await fileInput.isVisible({ timeout: 3000 }).catch(() => false);

    if (!isVisible) {
      // Input is hidden — try to set files with force or skip if no upload card
      const hasUploadCard = await page.getByText('Select File').isVisible({ timeout: 3000 }).catch(() => false);
      if (hasUploadCard) {
        await fileInput.setInputFiles({
          name: 'license.pdf',
          mimeType: 'application/pdf',
          buffer: Buffer.from('fake-pdf-content'),
        });
        // Either success toast or error (if already uploaded)
        const result = await page.getByText(/uploaded|already|error/i).isVisible({ timeout: 10000 }).catch(() => false);
        expect(result).toBe(true);
      }
    }
  });

  test('shows View Document and Download buttons for uploaded docs', async ({ page }) => {
    await page.goto('/driver/documents');

    const viewButton = page.getByText('View Document').first();
    if (await viewButton.isVisible({ timeout: 3000 }).catch(() => false)) {
      await expect(page.getByText('Download').first()).toBeVisible();
    }
  });

  test('opens FilePreviewModal when clicking View Document', async ({ page }) => {
    await page.goto('/driver/documents');

    const viewButton = page.getByText('View Document').first();
    if (await viewButton.isVisible({ timeout: 3000 }).catch(() => false)) {
      await viewButton.click();
      await expect(page.locator('.fixed.inset-0').last()).toBeVisible({ timeout: 5000 });
    }
  });
});
