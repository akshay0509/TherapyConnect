# Appointment responsive regression checks

Run `npm ci`, then start `npm run dev -- --host 127.0.0.1` in one terminal.
In another terminal, run `npm run test:mobile` and `npm run test:desktop`.
Tests use Playwright with installed Microsoft Edge by default. Set
`BROWSER_CHANNEL=chrome` to use installed Chrome instead.

Run `node --test e2e/rescheduleAvailability.test.mjs` for the mobile rescheduling
duration/contiguous-slot checks. These run without a browser or backend.

All API requests are intercepted with synthetic fixtures: tests do not book,
cancel, or modify real appointments. External font requests are blocked for
stable screenshots. The simulated date is 7 September 2026.

`test:mobile` verifies booking, a booking conflict, rescheduling across weeks,
cancellation, date-range history, empty/error/retry states, navigation, phone
widths, rotation with a draft, light theme, login/reset and tablet/desktop widths.

`test:desktop` captures 1024px and 1440px desktop schedule screenshots in the
ignored `e2e/artifacts` directory. Before modifying the desktop presentation,
set `PHASE=before` to capture a baseline. The default writes `after` screenshots.
The delivery package includes the original before/after captures for this change.
When a `before-<width>.png` exists in `e2e/artifacts`, the desktop check asserts
that the new screenshot is byte-identical. `BASELINE_DIR` can point to the
delivery package's `verification/screenshots` folder instead. Baseline comparison
requires the same browser/font environment; capture a fresh baseline when those change.

These tests exercise Chromium/Edge with mocked APIs. They do not replace staging
integration tests or physical iOS Safari/Android keyboard and safe-area checks.
