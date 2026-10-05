const { chromium } = require('playwright');
const { mockApi } = require('./fixtures.cjs');
const assert = require('node:assert/strict');
const fs = require('node:fs');

(async () => {
  fs.mkdirSync('e2e/artifacts', { recursive: true });
  const browser = await chromium.launch({ headless: true, channel: process.env.BROWSER_CHANNEL || 'msedge' });
  try {
    for (const width of [320, 1280]) {
      const page = await browser.newPage({ viewport: { width, height: 900 } });
      const errors = [];
      page.on('pageerror', e => errors.push(e.message));
      const fixture = await mockApi(page);
      Object.assign(fixture.appointments[0], { meetingStatus: 'READY', meetingUrl: 'https://meet.google.com/abc-defg-hij' });
      await page.goto('http://127.0.0.1:5173/therapist/appointments');
      if (width <= 640) await page.getByRole('button', { name: /Alex Morgan/ }).first().click();
      else await page.getByText('Alex Morgan', { exact: true }).first().click();
      const join = page.getByRole('link', { name: /Join Google Meet/ });
      await join.waitFor();
      assert.equal(await join.getAttribute('href'), 'https://meet.google.com/abc-defg-hij');
      assert.equal(await join.getAttribute('target'), '_blank');
      assert.equal(await join.getAttribute('rel'), 'noopener noreferrer');
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
      await page.screenshot({ path: `e2e/artifacts/meeting-ready-${width}.png`, fullPage: true, animations: 'disabled' });

      // Simulate a completed reschedule's pending projection, including a stale URL.
      Object.assign(fixture.appointments[0], { status: 'RESCHEDULED', meetingStatus: 'PENDING' });
      await page.reload();
      if (width <= 640) await page.getByRole('button', { name: /Alex Morgan/ }).first().click();
      else await page.getByText('Alex Morgan', { exact: true }).first().click();
      await page.getByText('Meeting update pending', { exact: true }).waitFor();
      assert.equal(await join.count(), 0);
      Object.assign(fixture.appointments[0], { meetingStatus: 'READY', meetingUrl: 'https://meet.google.com/klm-nopq-rst' });
      await page.getByRole('button', { name: 'Refresh meeting status', exact: true }).click();
      await join.waitFor();
      assert.equal(await join.getAttribute('href'), 'https://meet.google.com/klm-nopq-rst');
      const warnings = [];
      page.on('console', message => { if (message.type() === 'warning') warnings.push(message.text()); });
      fixture.appointments[0].meetingUrl = 'https://example.test/not-a-meet';
      await page.reload();
      if (width <= 640) await page.getByRole('button', { name: /Alex Morgan/ }).first().click();
      else await page.getByText('Alex Morgan', { exact: true }).first().click();
      await page.getByText('Meeting link is unavailable', { exact: true }).waitFor();
      assert(warnings.some(message => message.includes('Meeting marked READY has an invalid Google Meet URL')));
      assert(!warnings.some(message => message.includes('https://example.test/not-a-meet')));
      assert.equal(await join.count(), 0);
      fixture.appointments[0].status = 'CANCELLED';
      await page.reload();
      await join.waitFor({ state: 'hidden' });
      assert.deepEqual(errors, []);
      assert(!fixture.calls.some(c => c.path.includes('meeting-link') || c.path.startsWith('/notification')));
      await page.close();
    }
    const page = await browser.newPage({ viewport: { width: 1280, height: 1000 } });
    const fixture = await mockApi(page);
    Object.assign(fixture.appointments[0], { meetingStatus: 'READY', meetingUrl: 'https://meet.google.com/abc-defg-hij' });
    await page.goto('http://127.0.0.1:5173/therapist-home');
    await page.getByRole('link', { name: /Join Google Meet/ }).waitFor();
    await page.screenshot({ path: 'e2e/artifacts/meeting-home.png', fullPage: true, animations: 'disabled' });
    Object.assign(fixture.appointments[0], { meetingStatus: 'FAILED', meetingUrl: null });
    await page.reload();
    await page.getByText('Meeting link is unavailable', { exact: true }).waitFor();
    await page.route('**/appointment/editor-view?**', route => route.fulfill({ status: 503, json: { message: 'Unavailable' } }));
    await page.getByRole('button', { name: 'Refresh meeting status', exact: true }).click();
    await page.getByRole('alert').filter({ hasText: "Couldn't refresh meeting status" }).waitFor();
    await page.close();
    console.log('PASS: mobile/desktop details and home, ready/pending/refresh/cancel/failure; existing appointment APIs only.');
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exit(1); });
