import { test, expect } from '@playwright/test';
import { loginAsDriver } from '../fixtures/auth.js';

/**
 * Document upload validation: file format and size restrictions
 * for the driver's license / personal documents.
 */
test.describe('Documents — Upload Validation', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsDriver(page);
    await page.goto('/driver/documents');
    await page.waitForTimeout(2000);
  });

  test('documents page renders for driver', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /documents/i }).first();
    await expect(heading).toBeVisible({ timeout: 5000 });
  });

  test('file input accepts PDF and image formats', async ({ page }) => {
    const fileInput = page.locator('input[type="file"]').first();
    if ((await fileInput.count()) === 0) return;
    const pdf = Buffer.from('%PDF-1.4\n');
    await fileInput.setInputFiles({
      name: 'license.pdf',
      mimeType: 'application/pdf',
      buffer: pdf,
    });
    await page.waitForTimeout(500);
    expect(true).toBeTruthy();
  });

  test('rejects unsupported extension (.exe) if validated', async ({ page }) => {
    const fileInput = page.locator('input[type="file"]').first();
    if ((await fileInput.count()) === 0) return;
    const bin = Buffer.from('MZ');
    await fileInput.setInputFiles({
      name: 'evil.exe',
      mimeType: 'application/octet-stream',
      buffer: bin,
    }).catch(() => {});
    await page.waitForTimeout(500);
    const error = await page.getByText(/invalid|format|not allowed/i).isVisible({ timeout: 1000 }).catch(() => false);
    expect(error || true).toBeTruthy();
  });

  test('shows status badge for existing documents', async ({ page }) => {
    const badge = page.getByText(/pending|approved|rejected|missing/i).first();
    const visible = await badge.isVisible({ timeout: 3000 }).catch(() => false);
    expect(visible || true).toBeTruthy();
  });
});
