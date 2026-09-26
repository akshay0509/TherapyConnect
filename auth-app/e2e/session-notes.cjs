const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { mockApi } = require('./fixtures.cjs');

(async () => {
  const browser = await chromium.launch({ headless: true, channel: process.env.BROWSER_CHANNEL || 'msedge' });
  fs.mkdirSync('e2e/artifacts', { recursive: true });
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
    page.setDefaultTimeout(12000);
    const errors = []; page.on('pageerror', e => errors.push(e.message));
    await mockApi(page);
    const sessions = [
      { appointmentId: 'n1', status: 'COMPLETED', startTime: '2026-09-06T11:00:00', endTime: '2026-09-06T12:00:00', modeId: 'm1', sessionNotes: '' },
      { appointmentId: 'n0', status: 'COMPLETED', startTime: '2026-09-01T11:00:00', endTime: '2026-09-01T12:00:00', modeId: 'm1', sessionNotes: 'Legacy <b>literal</b>\nSecond line' },
    ];
    let failSave = false;
    await page.route('**/therapist/c1/session-details', route => route.fulfill({ json: sessions }));
    await page.route('**/client/get/c1', route => route.fulfill({ json: { clientId: 'c1', firstName: 'Alex', lastName: 'Morgan', clientName: 'Alex Morgan', status: 'ACTIVE' } }));
    await page.route('**/client/c1/fee-history', route => route.fulfill({ json: [] }));
    await page.route('**/therapist/c1/*-notes', route => {
      if (failSave) { failSave = false; return route.fulfill({ status: 500, json: { message: 'Test save failure' } }); }
      const body = route.request().postDataJSON();
      const session = sessions.find(s => s.appointmentId === body.appointmentId);
      if (session) session.sessionNotes = body.sessionNotes;
      return route.fulfill({ json: {} });
    });
    await page.goto('http://127.0.0.1:5173/therapist/clients/c1');
    await page.getByRole('button', { name: /^Sessions/ }).click();
    await page.getByRole('button', { name: 'Add Notes', exact: true }).click();
    const edit = page.getByRole('textbox', { name: 'Session notes', exact: true });
    const toolbar = page.getByRole('group', { name: 'Note formatting', exact: true });
    await toolbar.getByRole('button', { name: /^Bold/ }).click();
    await edit.fill('Session progress');
    await edit.press('Control+a');
    await toolbar.getByRole('button', { name: /^Underline/ }).click();
    await page.getByText('More formatting', { exact: true }).click();
    await page.getByLabel('Font', { exact: true }).selectOption('Georgia');
    await page.getByLabel('Font size', { exact: true }).selectOption('20');
    await page.getByLabel('Alignment', { exact: true }).selectOption('center');
    await page.getByLabel('Line spacing', { exact: true }).selectOption('2');
    await page.getByRole('button', { name: 'Link', exact: true }).click();
    await page.getByLabel('Link address').fill('javascript:alert(1)');
    await page.getByRole('button', { name: 'Apply link' }).click();
    await page.getByRole('alert').filter({ hasText: 'Use an https://' }).waitFor();
    await page.getByLabel('Link address').fill('https://example.com');
    await page.getByRole('button', { name: 'Apply link' }).click();
    await edit.press('ArrowRight'); await edit.press('End'); await edit.press('Enter');
    await page.getByRole('button', { name: 'Insert table', exact: true }).click();
    await edit.locator('th').first().click(); await page.keyboard.type('Goal');
    await page.getByLabel('Table actions').selectOption('addRowAfter');
    assert.equal(await edit.locator('tr').count(), 4);
    failSave = true;
    await page.getByRole('button', { name: 'Save', exact: true }).click();
    await page.getByText('Test save failure', { exact: true }).waitFor();
    assert((await edit.innerText()).includes('Session progress'));
    await page.getByRole('button', { name: 'Save', exact: true }).click();
    await edit.waitFor({ state: 'hidden' });
    assert.equal(JSON.parse(sessions[0].sessionNotes).format, 'therapyconnect.session-note');
    const saved = page.locator('.session-note-view').first();
    assert.equal(await saved.locator('u').count() > 0, true);
    assert.equal(await saved.locator('table').count(), 1);
    assert.equal(await saved.locator('a').getAttribute('href'), 'https://example.com');
    assert.equal(await saved.locator('p').first().evaluate(el => el.style.lineHeight), '2');
    await page.getByRole('button', { name: 'Modify', exact: true }).first().click();
    assert.equal(await edit.locator('table').count(), 1);
    // Opening a saved note without editing must not trigger discard confirmation.
    await page.getByRole('button', { name: 'Cancel', exact: true }).click();
    await edit.waitFor({ state: 'hidden' });
    await page.getByRole('button', { name: 'Modify', exact: true }).first().click();
    await edit.press('Control+a');
    await toolbar.getByRole('button', { name: /^Italic/ }).click();
    await page.getByRole('button', { name: 'Cancel', exact: true }).click();
    await page.getByText('Discard this note?', { exact: true }).waitFor();
    await page.getByRole('button', { name: 'Keep writing', exact: true }).click();
    await edit.press('Control+Enter');
    await edit.waitFor({ state: 'hidden' });
    // Legacy text and its literal angle brackets remain intact.
    await page.getByRole('button', { name: 'Modify', exact: true }).last().click();
    assert((await edit.innerText()).includes('<b>literal</b>'));
    await edit.press('Control+a');
    await toolbar.getByRole('button', { name: /^Bold/ }).click();
    await toolbar.getByRole('button', { name: 'Undo', exact: true }).click();
    await page.getByRole('button', { name: 'Cancel', exact: true }).click();
    await edit.waitFor({ state: 'hidden' });
    for (const width of [320, 390, 1280]) {
      await page.setViewportSize({ width, height: 900 });
      await page.getByRole('button', { name: 'Modify', exact: true }).first().click();
      await page.getByText('More formatting', { exact: true }).click();
      await page.getByRole('button', { name: /Previous notes/ }).click();
      assert((await page.locator('.session-note-view').last().innerText()).includes('<b>literal</b>'));
      await page.getByRole('button', { name: 'Hide previous notes' }).click();
      await page.screenshot({ path: `e2e/artifacts/session-notes-${width}.png`, fullPage: true });
      assert(await page.locator('.session-note-editor').evaluate(el => el.scrollWidth <= el.clientWidth + 1), `editor overflow at ${width}`);
      await page.getByRole('button', { name: 'Cancel', exact: true }).click();
    }
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto('http://127.0.0.1:5173/therapist/appointments');
    await page.getByRole('button', { name: /Alex Morgan/ }).first().click();
    await page.getByRole('button', { name: 'Completed', exact: true }).click();
    await edit.fill('Appointment note');
    await edit.press('Control+a');
    await edit.evaluate(el => {
      const data = new DataTransfer();
      data.setData('text/html', '<p><strong>Pasted note</strong><img src="https://invalid.example/tracker"><a href="javascript:alert(1)">link</a></p>');
      el.dispatchEvent(new ClipboardEvent('paste', { clipboardData: data, bubbles: true, cancelable: true }));
    });
    assert.equal(await edit.locator('strong').count(), 1);
    assert.equal(await edit.locator('img, a[href^="javascript:"]').count(), 0);
    await edit.press('Control+a');
    await toolbar.getByRole('button', { name: 'Bullet list', exact: true }).click();
    assert.equal(await edit.locator('ul').count(), 1);
    await toolbar.getByRole('button', { name: 'Numbered list', exact: true }).click();
    assert.equal(await edit.locator('ol').count(), 1);
    await toolbar.getByRole('button', { name: 'Checklist', exact: true }).click();
    assert.equal(await edit.locator('ul[data-type="taskList"]').count(), 1);
    await toolbar.getByRole('button', { name: 'Clear formatting', exact: true }).click();
    assert.equal(await edit.locator('strong, ul, ol').count(), 0);
    await toolbar.getByRole('button', { name: 'Undo', exact: true }).click();
    assert.equal(await edit.locator('ul[data-type="taskList"]').count(), 1);
    await toolbar.getByRole('button', { name: 'Redo', exact: true }).click();
    assert.equal(await edit.locator('ul, ol').count(), 0);
    await page.screenshot({ path: 'e2e/artifacts/appointment-note.png', fullPage: true });
    await page.getByRole('button', { name: 'Save', exact: true }).click();
    await edit.waitFor({ state: 'hidden' });
    assert.deepEqual(errors, []);
    console.log('PASS: rich formatting, tables, safe links, save/reopen, retry, discard, legacy notes, desktop/mobile and appointment editor');
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
