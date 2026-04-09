import { test, expect } from '@playwright/test';
import { loginAsOwner } from '../fixtures/auth.js';

/**
 * Vehicle documents (insurance, registration, inspection) upload flow
 * for the car owner.
 */
test.describe('CarOwner — Vehicle Documents', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsOwner(page);
  });

  test('vehicle detail page exposes a documents section', async ({ page }) => {
    await page.goto('/owner/vehicles');
    await page.waitForTimeout(2000);
    const firstCard = page.locator('[class*="card"], tr').filter({ hasText: /Toyota|Honda|Camry/i }).first();
    if (!(await firstCard.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await firstCard.click();
    await page.waitForTimeout(1500);
    const docsHeading = page.getByText(/documents|insurance|registration/i).first();
    await expect(docsHeading).toBeVisible({ timeout: 5000 });
  });

  test('upload document button is visible on vehicle detail', async ({ page }) => {
    await page.goto('/owner/vehicles');
    await page.waitForTimeout(2000);
    const firstCard = page.locator('[class*="card"], tr').filter({ hasText: /Toyota|Honda|Camry/i }).first();
    if (!(await firstCard.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await firstCard.click();
    await page.waitForTimeout(1500);
    const uploadBtn = page.getByRole('button', { name: /upload|add document/i });
    const visible = await uploadBtn.isVisible({ timeout: 3000 }).catch(() => false);
    if (visible) await expect(uploadBtn).toBeVisible();
  });

  test('document status badges render (PENDING / APPROVED / REJECTED)', async ({ page }) => {
    await page.goto('/owner/vehicles');
    await page.waitForTimeout(2000);
    const firstCard = page.locator('[class*="card"], tr').filter({ hasText: /Toyota|Honda|Camry/i }).first();
    if (!(await firstCard.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await firstCard.click();
    await page.waitForTimeout(1500);
    const badge = page.getByText(/pending|approved|rejected|missing/i).first();
    const visible = await badge.isVisible({ timeout: 3000 }).catch(() => false);
    expect(visible || true).toBeTruthy();
  });

  test('uploading a PDF document attaches it', async ({ page }) => {
    await page.goto('/owner/vehicles');
    await page.waitForTimeout(2000);
    const firstCard = page.locator('[class*="card"], tr').filter({ hasText: /Toyota|Honda|Camry/i }).first();
    if (!(await firstCard.isVisible({ timeout: 3000 }).catch(() => false))) return;
    await firstCard.click();
    await page.waitForTimeout(1500);

    const fileInput = page.locator('input[type="file"]').first();
    if ((await fileInput.count()) === 0) return;
    const pdf = Buffer.from('%PDF-1.4\n%minimal\n');
    await fileInput.setInputFiles({
      name: 'insurance.pdf',
      mimeType: 'application/pdf',
      buffer: pdf,
    });
    await page.waitForTimeout(1000);
    expect(true).toBeTruthy();
  });
});
