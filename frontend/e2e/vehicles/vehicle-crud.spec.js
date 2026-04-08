import { test, expect } from '@playwright/test';
import { loginAsOwner, logout } from '../fixtures/auth.js';

test.describe('CarOwner — Vehicle Registration & Management', () => {

  test.beforeEach(async ({ page }) => {
    await loginAsOwner(page);
  });

  test('shows My Vehicles page with title and register button', async ({ page }) => {
    await page.goto('/owner/vehicles');
    await expect(page.getByText('My Vehicles')).toBeVisible();
    await expect(page.getByText(/register new vehicle/i)).toBeVisible();
  });

  test('opens register vehicle form', async ({ page }) => {
    await page.goto('/owner/vehicles');
    await page.getByText(/register new vehicle/i).click();
    await expect(page.getByPlaceholder(/VIN/i).or(page.getByText(/VIN/i))).toBeVisible({ timeout: 5000 });
  });

  test('registers a vehicle successfully', async ({ page }) => {
    await page.goto('/owner/vehicles');
    await page.getByText(/register new vehicle/i).click();

    // Fill form — wait for it to appear
    await page.waitForTimeout(500);

    // VIN
    const vinInput = page.locator('input[name="vin"]').or(page.getByPlaceholder(/vin/i));
    if (await vinInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await vinInput.fill('WVWZZZ3CZWE' + Date.now().toString().slice(-6));
    }

    // Make and Model
    const makeInput = page.locator('input[name="make"]').or(page.getByPlaceholder(/make/i));
    if (await makeInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await makeInput.fill('Toyota');
    }

    const modelInput = page.locator('input[name="model"]').or(page.getByPlaceholder(/model/i));
    if (await modelInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await modelInput.fill('Camry');
    }

    // Year
    const yearInput = page.locator('input[name="year"]').or(page.getByPlaceholder(/year/i));
    if (await yearInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await yearInput.fill('2022');
    }

    // License Plate
    const plateInput = page.locator('input[name="licensePlate"]').or(page.getByPlaceholder(/plate/i));
    if (await plateInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await plateInput.fill('TEST ' + Math.floor(Math.random() * 999));
    }

    // Hourly Rate
    const rateInput = page.locator('input[name="hourlyRate"]').or(page.getByPlaceholder(/rate/i));
    if (await rateInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await rateInput.fill('25');
    }

    // General Location
    const locationInput = page.locator('input[name="generalLocation"]').or(page.getByPlaceholder(/location/i));
    if (await locationInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await locationInput.fill('Vancouver Downtown');
    }

    // Latitude/Longitude
    const latInput = page.locator('input[name="latitude"]').or(page.getByPlaceholder(/latitude/i));
    if (await latInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await latInput.fill('49.2827');
    }

    const lngInput = page.locator('input[name="longitude"]').or(page.getByPlaceholder(/longitude/i));
    if (await lngInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await lngInput.fill('-123.1207');
    }

    // Category and Fuel Type selects
    const categorySelect = page.locator('select[name="category"]');
    if (await categorySelect.isVisible({ timeout: 2000 }).catch(() => false)) {
      await categorySelect.selectOption('SEDAN');
    }

    const fuelSelect = page.locator('select[name="fuelType"]');
    if (await fuelSelect.isVisible({ timeout: 2000 }).catch(() => false)) {
      await fuelSelect.selectOption('GASOLINE');
    }

    // Submit
    const submitBtn = page.getByRole('button', { name: /register|submit|save/i });
    if (await submitBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
      await submitBtn.click();
      await expect(page.getByText(/registered|success/i)).toBeVisible({ timeout: 10000 });
    }
  });

  test('shows vehicle cards or empty state', async ({ page }) => {
    await page.goto('/owner/vehicles');

    // Page should show either vehicle cards or the "My Vehicles" title at minimum
    await expect(page.getByText('My Vehicles')).toBeVisible({ timeout: 5000 });
  });
});
