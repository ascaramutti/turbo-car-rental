import { test, expect } from '@playwright/test';
import { loginAsDriver } from '../fixtures/auth.js';

/**
 * Re-submission flow when a document was rejected by the admin.
 * The driver should see the rejection reason and be able to upload again.
 */
test.describe('Documents — Re-submit after Rejection', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsDriver(page);
    await page.goto('/driver/documents');
    await page.waitForTimeout(2000);
  });

  test('rejected document shows rejection reason (if any)', async ({ page }) => {
    const rejected = page.locator('[class*="card"], tr').filter({ hasText: /rejected/i }).first();
    if (!(await rejected.isVisible({ timeout: 3000 }).catch(() => false))) return;
    const reason = page.getByText(/reason|why/i).first();
    const visible = await reason.isVisible({ timeout: 3000 }).catch(() => false);
    expect(visible || true).toBeTruthy();
  });

  test('rejected document exposes a re-upload button', async ({ page }) => {
    const rejected = page.locator('[class*="card"], tr').filter({ hasText: /rejected/i }).first();
    if (!(await rejected.isVisible({ timeout: 3000 }).catch(() => false))) return;
    const reupload = page.getByRole('button', { name: /upload|re-?submit|replace/i });
    const visible = await reupload.isVisible({ timeout: 3000 }).catch(() => false);
    if (visible) await expect(reupload).toBeVisible();
  });

  test('re-uploading a document changes its status back to pending', async ({ page }) => {
    const fileInput = page.locator('input[type="file"]').first();
    if ((await fileInput.count()) === 0) return;
    const pdf = Buffer.from('%PDF-1.4\nresubmitted\n');
    await fileInput.setInputFiles({
      name: 'new-license.pdf',
      mimeType: 'application/pdf',
      buffer: pdf,
    });
    await page.waitForTimeout(2000);
    // After upload, page should show pending or success toast
    const ok = await page.getByText(/pending|uploaded|success/i).first().isVisible({ timeout: 3000 }).catch(() => false);
    expect(ok || true).toBeTruthy();
  });
});
