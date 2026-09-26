// A versioned envelope distinguishes rich notes from legacy text (including HTML).
export const NOTE_FORMAT = 'therapyconnect.session-note';
export const FONTS = ['Arial', 'Georgia', 'Times New Roman', 'Verdana', 'Courier New'];
export const SIZES = ['12', '14', '16', '18', '20', '24', '28', '32'];
const nodes = new Set(['doc', 'paragraph', 'text', 'heading', 'hardBreak', 'bulletList', 'orderedList', 'listItem', 'taskList', 'taskItem', 'blockquote', 'horizontalRule', 'table', 'tableRow', 'tableCell', 'tableHeader']);
const marks = new Set(['bold', 'italic', 'underline', 'strike', 'textStyle', 'highlight', 'link']);
const color = value => typeof value === 'string' && (/^#[\da-f]{3,8}$/i.test(value) || /^rgba?\([\d.,%\s]+\)$/.test(value));
export const safeLink = value => typeof value === 'string' && /^(https?:\/\/|mailto:)/i.test(value) && !/[\u0000-\u0020\u007f]/.test(value);

function cleanAttrs(attrs = {}) {
  const out = {};
  if (['left', 'center', 'right', 'justify'].includes(attrs.textAlign)) out.textAlign = attrs.textAlign;
  if (FONTS.includes(attrs.fontFamily)) out.fontFamily = attrs.fontFamily;
  if (SIZES.some(n => attrs.fontSize === `${n}px`)) out.fontSize = attrs.fontSize;
  if (['1', '1.5', '2'].includes(String(attrs.lineHeight))) out.lineHeight = String(attrs.lineHeight);
  for (const key of ['color', 'backgroundColor']) if (color(attrs[key])) out[key] = attrs[key];
  if (Number.isInteger(attrs.level) && attrs.level >= 1 && attrs.level <= 3) out.level = attrs.level;
  if (Number.isInteger(attrs.indent)) out.indent = Math.min(6, Math.max(0, attrs.indent));
  for (const key of ['colspan', 'rowspan', 'start']) if (Number.isInteger(attrs[key]) && attrs[key] > 0 && attrs[key] <= 1000) out[key] = attrs[key];
  if (typeof attrs.checked === 'boolean') out.checked = attrs.checked;
  if (safeLink(attrs.href)) { out.href = attrs.href; out.target = '_blank'; out.rel = 'noopener noreferrer'; }
  return out;
}

export function cleanDocument(node, depth = 0) {
  if (!node || !nodes.has(node.type) || depth > 60) return null;
  if (node.type === 'text') {
    if (typeof node.text !== 'string' || !node.text) return null;
    return { type: 'text', text: node.text, ...(Array.isArray(node.marks) ? { marks: node.marks.filter(m => marks.has(m.type) && (m.type !== 'link' || safeLink(m.attrs?.href))).map(m => ({ type: m.type, attrs: cleanAttrs(m.attrs) })) } : {}) };
  }
  return { type: node.type, attrs: cleanAttrs(node.attrs), ...(Array.isArray(node.content) ? { content: node.content.map(n => cleanDocument(n, depth + 1)).filter(Boolean) } : {}) };
}

const blocks = new Set(['paragraph', 'heading', 'bulletList', 'orderedList', 'taskList', 'blockquote', 'horizontalRule', 'table']);
function validStructure(node) {
  const children = node.content || [];
  if (!children.every(validStructure)) return false;
  switch (node.type) {
    case 'doc': case 'blockquote': case 'tableCell': case 'tableHeader':
      return children.length > 0 && children.every(n => blocks.has(n.type));
    case 'paragraph': case 'heading': return children.every(n => ['text', 'hardBreak'].includes(n.type));
    case 'bulletList': case 'orderedList': return children.length > 0 && children.every(n => n.type === 'listItem');
    case 'taskList': return children.length > 0 && children.every(n => n.type === 'taskItem');
    case 'listItem': case 'taskItem': return children[0]?.type === 'paragraph' && children.every(n => blocks.has(n.type));
    case 'table': return children.length > 0 && children.every(n => n.type === 'tableRow');
    case 'tableRow': return children.length > 0 && children.every(n => ['tableCell', 'tableHeader'].includes(n.type));
    default: return children.length === 0;
  }
}

export function decodeNote(value = '') {
  try {
    const envelope = JSON.parse(value);
    if (envelope?.format === NOTE_FORMAT && envelope.version === 1 && envelope.doc?.type === 'doc') {
      const doc = cleanDocument(envelope.doc);
      if (validStructure(doc)) return doc;
    }
  } catch { /* Old notes are literal text, never interpreted as HTML. */ }
  return { type: 'doc', content: String(value ?? '').split('\n').map(text => ({ type: 'paragraph', ...(text ? { content: [{ type: 'text', text }] } : {}) })) };
}

export function encodeNote(doc) {
  return JSON.stringify({ format: NOTE_FORMAT, version: 1, doc: cleanDocument(doc) });
}

function textOf(node) {
  if (node.type === 'text') return node.text;
  if (node.type === 'hardBreak') return '\n';
  const text = (node.content || []).map(textOf).join('');
  return text + (['paragraph', 'heading', 'tableCell', 'tableHeader'].includes(node.type) ? '\n' : '');
}
export const noteText = value => textOf(decodeNote(value)).trim();
export const hasNoteText = value => Boolean(noteText(value));
