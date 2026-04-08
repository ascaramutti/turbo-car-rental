/**
 * Authentication helpers for E2E tests.
 * Credentials from backend DataInitializer seed data.
 */

/** Logs in as a user and waits for redirect. */
export async function login(page, email, password) {
  await page.goto('/login');
  await page.getByPlaceholder('you@example.com').fill(email);
  await page.getByPlaceholder('Enter your password').fill(password);
  await page.getByRole('button', { name: /log in/i }).click();
}

/** Logs in as the seed driver (driver@turbo.com / driver123). */
export async function loginAsDriver(page) {
  await login(page, 'driver@turbo.com', 'driver123');
  await page.waitForURL('**/driver/**');
}

/** Logs in as the seed admin (admin@turbo.com / admin123). */
export async function loginAsAdmin(page) {
  await login(page, 'admin@turbo.com', 'admin123');
  await page.waitForURL('**/admin/**');
}

/** Logs in as the seed car owner (owner@turbo.com / owner123). */
export async function loginAsOwner(page) {
  await login(page, 'owner@turbo.com', 'owner123');
  await page.waitForURL('**/owner/**');
}

/** Logs out the current user by clearing storage. */
export async function logout(page) {
  await page.evaluate(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  });
  await page.goto('/login');
}
