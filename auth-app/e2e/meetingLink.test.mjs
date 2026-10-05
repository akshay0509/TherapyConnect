import test from 'node:test';
import assert from 'node:assert/strict';
import { safeMeetUrl, meetingView } from '../src/utils/meetingLink.mjs';

test('accepts only canonical HTTPS Google Meet destinations', () => {
  assert.equal(safeMeetUrl('https://meet.google.com/abc-defg-hij'), 'https://meet.google.com/abc-defg-hij');
  for (const value of ['javascript:alert(1)', 'https://meet.google.com.evil.test/abc-defg-hij',
    'https://user@meet.google.com/abc-defg-hij', 'http://meet.google.com/abc-defg-hij',
    'https://meet.google.com:8443/abc-defg-hij', 'https://meet.google.com/', null]) assert.equal(safeMeetUrl(value), null);
});
test('only ready active online appointments can join', () => {
  const a = { status: 'CONFIRMED', meetingStatus: 'READY', meetingUrl: 'https://meet.google.com/abc-defg-hij' };
  assert(meetingView(a, 'ONLINE').url);
  assert(meetingView({ ...a, status: 'RESCHEDULED' }, 'ONLINE').url);
  for (const status of ['SCHEDULED', 'CANCELLED', 'COMPLETED', 'ABANDONED']) assert.equal(meetingView({ ...a, status }, 'ONLINE'), null);
  assert.equal(meetingView(a, 'OFFLINE'), null);
  assert.equal(meetingView({ status: 'CONFIRMED' }, 'ONLINE'), null);
  assert.equal(meetingView({ ...a, meetingStatus: 'NOT_APPLICABLE' }, 'ONLINE'), null);
});
test('pending and invalid links never retain a join destination', () => {
  const a = { appointmentStatus: 'RESCHEDULED', meetingUrl: 'https://meet.google.com/abc-defg-hij' };
  assert.equal(meetingView({ ...a, meetingStatus: 'PENDING' }, 'ONLINE').url, null);
  assert.equal(meetingView({ ...a, meetingStatus: 'FAILED' }, 'ONLINE').failed, true);
  assert.equal(meetingView({ ...a, meetingStatus: 'READY', meetingUrl: 'https://evil.test' }, 'ONLINE').failed, true);
});
