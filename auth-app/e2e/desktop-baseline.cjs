const { chromium } = require('playwright');
const { mockApi } = require('./fixtures.cjs');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
(async()=>{
  const browser=await chromium.launch({headless:true, channel:process.env.BROWSER_CHANNEL || 'msedge'});
  fs.mkdirSync('e2e/artifacts',{recursive:true});
  for (const width of [1024,1440]) {
    const page=await browser.newPage({viewport:{width,height:1000}});
    page.setDefaultTimeout(10000);
    page.on('pageerror',e=>console.error(e.message));
    await mockApi(page);
    await page.goto('http://127.0.0.1:5173/login');
    await page.waitForTimeout(700);
    await page.evaluate(()=>{history.pushState({},'', '/therapist/appointments');dispatchEvent(new PopStateEvent('popstate'));});
    await page.getByRole('heading',{name:'Schedule',exact:true}).waitFor();
    await page.getByText('Alex Morgan',{exact:true}).first().waitFor();
    await page.screenshot({path:`e2e/artifacts/${process.env.PHASE||'after'}-${width}.png`,fullPage:true,animations:'disabled'});
    const baseline=path.join(process.env.BASELINE_DIR || 'e2e/artifacts',`before-${width}.png`);
    if (process.env.PHASE !== 'before' && fs.existsSync(baseline)) {
      assert(fs.readFileSync(baseline).equals(fs.readFileSync(`e2e/artifacts/${process.env.PHASE||'after'}-${width}.png`)),`Desktop screenshot changed at ${width}px`);
      console.log(`PASS: ${width}px desktop screenshot is byte-identical to the baseline.`);
    }
    await page.close();
  }
  await browser.close();
  console.log('PASS: desktop schedule rendered at 1024px and 1440px; screenshots saved for baseline comparison.');
})().catch(e=>{console.error(e);process.exit(1);});
