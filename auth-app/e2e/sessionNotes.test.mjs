import test from 'node:test';
import assert from 'node:assert/strict';
import { decodeNote, encodeNote, noteText, hasNoteText, safeLink, NOTE_FORMAT } from '../src/utils/sessionNotes.mjs';

test('legacy markup and line breaks remain literal text', () => {
  const legacy = '<b>literal</b>\nsecond line';
  assert.equal(noteText(legacy), legacy);
  assert.equal(decodeNote(legacy).content[0].content[0].text, '<b>literal</b>');
});
test('rich marks, nested lists, tables and text survive serialization', () => {
  const doc = { type: 'doc', content: [{ type: 'bulletList', content: [{ type: 'listItem', content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Plan', marks: [{ type: 'bold' }, { type: 'underline' }] }] }] }] }, { type: 'table', content: [{ type: 'tableRow', content: [{ type: 'tableCell', attrs: { colspan: 2 }, content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Next session' }] }] }] }] }] };
  const encoded = encodeNote(doc);
  assert.equal(encodeNote(decodeNote(encoded)), encoded);
  assert.equal(noteText(encoded), 'Plan\nNext session');
  assert.equal(JSON.parse(encoded).format, NOTE_FORMAT);
});
test('empty blocks and formatting alone do not count as a note', () => {
  assert.equal(hasNoteText(encodeNote({ type: 'doc', content: [{ type: 'paragraph' }, { type: 'horizontalRule' }] })), false);
  assert.equal(hasNoteText(' \n '), false);
});
test('untrusted nodes, styles and unsafe links are removed', () => {
  const doc = { type: 'doc', content: [{ type: 'image', attrs: { src: 'https://tracker.test' } }, { type: 'paragraph', attrs: { indent: 999, textAlign: 'bad', style: 'bad' }, content: [{ type: 'text', text: 'safe', marks: [{ type: 'link', attrs: { href: 'javascript:alert(1)' } }, { type: 'textStyle', attrs: { color: 'url(https://tracker.test)', fontFamily: 'untrusted' } }] }] }] };
  const cleaned = decodeNote(encodeNote(doc));
  assert.equal(cleaned.content.length, 1);
  assert.deepEqual(cleaned.content[0].attrs, { indent: 6 });
  assert.deepEqual(cleaned.content[0].content[0].marks, [{ type: 'textStyle', attrs: {} }]);
  assert.equal(safeLink('https://example.com'), true);
  assert.equal(safeLink('mailto:a@example.com'), true);
  assert.equal(safeLink('java\nscript:alert(1)'), false);
});
test('unrecognized envelopes and malformed JSON stay readable', () => {
  for (const text of ['{oops', '{"doc":{}}', '{"format":"other","version":1}', JSON.stringify({ format: NOTE_FORMAT, version: 1, doc: { type: 'doc', content: [{ type: 'text', text: 'invalid structure' }] } })]) assert.equal(noteText(text), text);
});
