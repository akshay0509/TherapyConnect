import test from "node:test";
import assert from "node:assert/strict";
import { fittingRescheduleStarts } from "../src/utils/rescheduleAvailability.mjs";

const at = time => `2026-09-09T${time}:00`;
const slot = (start, end) => ({ startTime: at(start), endTime: at(end) });
const hours = [slot("16:00","16:30"),slot("16:30","17:00"),slot("17:00","17:30"),slot("17:30","18:00")];

test("50-minute sessions require two blocks and cannot start at 17:30", () => {
  assert.deepEqual(fittingRescheduleStarts(hours,at("09:00"),at("09:50")),hours.slice(0,3));
});
test("a missing block prevents a session spanning the gap", () => {
  assert.deepEqual(fittingRescheduleStarts([hours[0],hours[2],hours[3]],at("09:00"),at("10:00")),[hours[2]]);
});
test("a 30-minute session can use the final block", () => {
  assert.deepEqual(fittingRescheduleStarts(hours,at("09:00"),at("09:30")),hours);
});
test("75-minute sessions reserve three complete blocks", () => {
  assert.deepEqual(fittingRescheduleStarts(hours,at("09:00"),at("10:15")),hours.slice(0,2));
});
test("empty or invalid appointments have no valid start", () => {
  assert.deepEqual(fittingRescheduleStarts([],at("09:00"),at("09:50")),[]);
  assert.deepEqual(fittingRescheduleStarts(hours,undefined,undefined),[]);
  assert.deepEqual(fittingRescheduleStarts(hours,at("10:00"),at("09:00")),[]);
});
