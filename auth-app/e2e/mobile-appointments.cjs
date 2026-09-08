const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const { mockApi } = require('./fixtures.cjs');
const fs = require('node:fs');
const browserOptions = { headless:true, channel:process.env.BROWSER_CHANNEL || 'msedge' };
async function noOverflow(page) {
  assert(await page.evaluate(()=>document.documentElement.scrollWidth <= innerWidth), 'Page must not overflow horizontally');
  for (const dialog of await page.getByRole('dialog').all()) assert(await dialog.evaluate(el=>el.scrollWidth<=el.clientWidth), 'Dialog must not overflow horizontally');
}
async function boot(browser,width=390,path='/therapist/appointments') {
  const page=await browser.newPage({viewport:{width,height:844},hasTouch:true});
  page.setDefaultTimeout(10000);
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  const fixture=await mockApi(page);
  await page.goto(`http://127.0.0.1:5173${path}`);
  await page.getByRole('heading',{name:'Appointments',exact:true}).waitFor();
  await page.getByRole('button',{name:/Alex Morgan/}).first().waitFor();
  return {page,fixture,errors};
}
(async()=>{
  fs.mkdirSync('e2e/artifacts',{recursive:true});
  const browser=await chromium.launch(browserOptions);
  try {
    for(const width of [320,375,390,640]) {
      const {page,errors}=await boot(browser,width);
      await noOverflow(page);
      await page.screenshot({path:`e2e/artifacts/mobile-${width}.png`,fullPage:true});
      await page.getByRole('button',{name:'Menu',exact:true}).click();
      await page.getByRole('dialog',{name:'Workspace navigation'}).waitFor();
      await page.keyboard.press('Escape');
      assert.equal(await page.getByRole('button',{name:'Menu',exact:true}).getAttribute('aria-expanded'),'false');
      await page.getByRole('button',{name:'Book appointment',exact:true}).click();
      await page.getByLabel('Client',{exact:true}).selectOption('c1');
      await page.getByLabel('Delivery mode',{exact:true}).selectOption('m1');
      await page.getByLabel('Available time',{exact:true}).selectOption('2026-09-07_13:00');
      await noOverflow(page);
      await page.screenshot({path:`e2e/artifacts/booking-${width}.png`,fullPage:true});
      await page.setViewportSize({width:1024,height:768});
      await page.setViewportSize({width,height:844});
      assert.equal(await page.getByLabel('Client',{exact:true}).inputValue(),'c1');
      assert.equal(await page.getByLabel('Available time',{exact:true}).inputValue(),'2026-09-07_13:00');
      await page.getByRole('button',{name:'Close',exact:true}).click();
      assert.equal(await page.evaluate(()=>document.body.style.overflow),'');
      assert.deepEqual(errors,[]);
      await page.close();
    }
    const {page,fixture,errors}=await boot(browser,390,'/therapist-home');
    assert(page.url().endsWith('/therapist/appointments'));
    await page.getByRole('button',{name:'Upcoming',exact:true}).click();
    await page.getByRole('button',{name:/21 Sept.*Alex Morgan/}).waitFor();
    await page.getByRole('button',{name:'Past',exact:true}).click();
    await page.getByRole('button',{name:/25 Aug.*Alex Morgan/}).waitFor();
    assert(fixture.calls.some(c=>c.query.includes('fromDate=2026-08-09')));
    await page.getByRole('button',{name:'Day',exact:true}).click();
    await page.getByRole('button',{name:/Alex Morgan/}).first().click();
    await page.getByRole('heading',{name:'Appointment details'}).waitFor();
    await page.getByRole('button',{name:/Reschedule/,exact:true}).click();
    await page.getByLabel('Reschedule date').fill('2026-09-21');
    await page.getByRole('button',{name:/01:00.*02:00/i}).click();
    await page.screenshot({path:'e2e/artifacts/reschedule.png',fullPage:true});
    await page.getByRole('button',{name:'Confirm',exact:true}).click();
    await page.getByRole('dialog').waitFor({state:'hidden'});
    assert(fixture.calls.some(c=>c.path==='/appointment/reschedule-appointment' && c.body.newSlotId==='2026-09-21_13:00'));
    await page.getByLabel('Agenda date').fill('2026-09-21');
    await page.getByRole('button',{name:/01:00.*Alex Morgan/}).click();
    await page.getByRole('button',{name:'Cancelled',exact:true}).click();
    await page.getByRole('button',{name:'Confirm cancellation',exact:true}).click();
    await page.getByRole('dialog').waitFor({state:'hidden'});
    assert.equal(fixture.appointments.find(a=>a.appointmentId==='a1').status,'CANCELLED');
    await page.getByRole('button',{name:'Book appointment',exact:true}).click();
    await page.getByLabel('Client',{exact:true}).selectOption('c1');
    await page.getByLabel('Delivery mode',{exact:true}).selectOption('m1');
    await page.getByLabel('Available time',{exact:true}).selectOption('2026-09-21_13:00');
    fixture.conflictNext();
    await page.getByRole('button',{name:'Confirm',exact:true}).click();
    await page.getByText('This slot has already been booked.',{exact:false}).waitFor();
    assert.equal(await page.getByLabel('Client',{exact:true}).inputValue(),'c1');
    await page.getByLabel('Available time',{exact:true}).selectOption('2026-09-21_14:00');
    await page.getByRole('button',{name:'Confirm',exact:true}).click();
    await page.getByText('Booked!',{exact:false}).waitFor();
    assert(fixture.calls.some(c=>c.path==='/appointment/create-appointment' && c.body.slotId==='2026-09-21_14:00'));
    assert.deepEqual(errors,[]);
    await page.close();
    const extra=await boot(browser);
    await extra.page.evaluate(()=>document.documentElement.dataset.theme='light');
    await noOverflow(extra.page);
    await extra.page.screenshot({path:'e2e/artifacts/mobile-light.png',fullPage:true});
    await extra.page.route('**/appointment/editor-view?**',route=>route.fulfill({status:503,json:{message:'Schedule temporarily unavailable'}}));
    await extra.page.getByRole('button',{name:'Upcoming',exact:true}).click();
    await extra.page.getByRole('alert').waitFor();
    await extra.page.unroute('**/appointment/editor-view?**');
    await extra.page.getByRole('button',{name:'Retry',exact:true}).click();
    await extra.page.getByRole('button',{name:/21 Sept.*Alex Morgan/}).waitFor();
    await extra.page.route('**/appointment/editor-view?**',route=>route.fulfill({status:200,json:{slots:[],appointments:[],overrides:[]}}));
    await extra.page.getByRole('button',{name:'Day',exact:true}).click();
    await extra.page.getByLabel('Agenda date').fill('2026-10-07');
    await extra.page.getByText('No appointments on this day.',{exact:true}).waitFor();
    await extra.page.getByText('No bookable availability on this day. Choose another date.',{exact:true}).waitFor();
    await extra.page.close();
    for (const width of [768,900,901,1440]) {
      const p=await browser.newPage({viewport:{width,height:1000}});
      await mockApi(p);
      await p.goto('http://127.0.0.1:5173/therapist/appointments');
      await p.getByRole('heading',{name:'Schedule',exact:true}).waitFor();
      await noOverflow(p);
      assert.equal(await p.getByRole('heading',{name:'Appointments',exact:true}).count(),0);
      await p.close();
    }
    for (const route of ['/login','/reset-password?token=demo']) {
      const p=await browser.newPage({viewport:{width:320,height:640}});
      await mockApi(p);
      await p.goto(`http://127.0.0.1:5173${route}`);
      await p.locator('input').first().waitFor();
      await noOverflow(p);
      await p.close();
    }
    console.log('PASS: mobile widths, drawer, orientation preservation, home entry, ranged history, cross-week reschedule, cancellation, booking conflict and success.');
    console.log('PASS: light theme, failed range fetch/retry, empty appointments/availability, tablet/desktop breakpoints, login/reset layouts.');
  } finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exit(1);});
