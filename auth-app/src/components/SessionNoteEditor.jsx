import { useEffect, useRef, useState } from 'react';
import { EditorContent, useEditor } from '@tiptap/react';
import { Extension } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import { TextStyleKit } from '@tiptap/extension-text-style';
import TextAlign from '@tiptap/extension-text-align';
import Highlight from '@tiptap/extension-highlight';
import { TableKit } from '@tiptap/extension-table';
import { TaskList, TaskItem } from '@tiptap/extension-list';
import Placeholder from '@tiptap/extension-placeholder';
import { Bold, Italic, Underline, Strikethrough, List, ListOrdered, ListChecks, Undo2, Redo2, RemoveFormatting } from 'lucide-react';
import { cleanDocument, decodeNote, encodeNote, FONTS, SIZES, safeLink } from '../utils/sessionNotes.mjs';
import './SessionNoteEditor.css';

const Indent = Extension.create({
  name: 'noteIndent',
  addGlobalAttributes() {
    return [{ types: ['paragraph', 'heading'], attributes: { indent: {
      default: 0,
      parseHTML: el => Math.min(6, Math.max(0, parseInt(el.getAttribute('data-indent'), 10) || 0)),
      renderHTML: attrs => attrs.indent ? { 'data-indent': attrs.indent, style: `margin-left: ${attrs.indent * 1.5}em` } : {},
    }, lineHeight: {
      default: null,
      parseHTML: el => ['1', '1.5', '2'].includes(el.style.lineHeight) ? el.style.lineHeight : null,
      renderHTML: attrs => attrs.lineHeight ? { style: `line-height: ${attrs.lineHeight}` } : {},
    } } }];
  },
});

const extensions = placeholder => [
  StarterKit.configure({ heading: { levels: [1, 2, 3] }, code: false, codeBlock: false,
    link: { openOnClick: false, protocols: ['http', 'https', 'mailto'], isAllowedUri: safeLink } }),
  TextStyleKit.configure({ lineHeight: false }), TextAlign.configure({ types: ['heading', 'paragraph'] }),
  Highlight.configure({ multicolor: true }), TableKit.configure({ table: { resizable: false } }),
  TaskList, TaskItem.configure({ nested: true }), Indent,
  Placeholder.configure({ placeholder }),
];

export default function SessionNoteEditor({ value = '', onChange, placeholder = 'Write session notes…', autoFocus = false, disabled = false, onSave }) {
  const initial = useRef(value);
  const baseline = useRef(null);
  const latest = useRef({ onChange, onSave, disabled });
  latest.current = { onChange, onSave, disabled };
  const [linkOpen, setLinkOpen] = useState(false);
  const [url, setUrl] = useState('');
  const [linkError, setLinkError] = useState('');
  const editor = useEditor({
    extensions: extensions(placeholder), content: decodeNote(initial.current), shouldRerenderOnTransaction: true,
    autofocus: autoFocus ? 'end' : false,
    onCreate: ({ editor: instance }) => { baseline.current = encodeNote(instance.getJSON()); },
    onUpdate: ({ editor: instance }) => {
      const encoded = encodeNote(instance.getJSON());
      latest.current.onChange(encoded === baseline.current ? initial.current : encoded);
    },
    editorProps: {
      attributes: { role: 'textbox', 'aria-label': 'Session notes', 'aria-multiline': 'true', spellcheck: 'true' },
      handleKeyDown: (_view, event) => {
        if ((event.ctrlKey || event.metaKey) && event.key === 'Enter' && latest.current.onSave) {
          event.preventDefault();
          if (!latest.current.disabled) latest.current.onSave();
          return true;
        }
        return false;
      },
      // Re-parse only supported schema content; no images, scripts or attachments.
      transformPasted: (slice, view) => {
        const json = cleanDocument({ type: 'doc', content: slice.content.toJSON() });
        const fragment = view.state.schema.nodeFromJSON(json).content;
        return new slice.constructor(fragment, slice.openStart, slice.openEnd);
      },
    },
  });
  useEffect(() => { editor?.setEditable(!disabled, false); }, [editor, disabled]);
  if (!editor) return null;
  const chain = () => editor.chain().focus();
  const button = (label, action, active, Glyph, blocked = false) => <button type="button" key={label} title={label} aria-label={label} aria-pressed={active === undefined ? undefined : active} disabled={disabled || blocked} onMouseDown={e => e.preventDefault()} onClick={action}>{Glyph ? <Glyph size={17} aria-hidden="true" /> : label}</button>;
  const indent = delta => {
    const type = editor.isActive('taskItem') ? 'taskItem' : editor.isActive('listItem') ? 'listItem' : null;
    if (type) { delta > 0 ? chain().sinkListItem(type).run() : chain().liftListItem(type).run(); return; }
    const { state, view } = editor;
    const tr = state.tr;
    state.doc.nodesBetween(state.selection.from, state.selection.to, (node, pos) => {
      if (['paragraph', 'heading'].includes(node.type.name)) tr.setNodeMarkup(pos, undefined, { ...node.attrs, indent: Math.max(0, Math.min(6, (node.attrs.indent || 0) + delta)) });
    });
    view.dispatch(tr); editor.commands.focus();
  };
  const applyLink = () => {
    const next = url.trim();
    if (next && !safeLink(next)) { setLinkError('Use an https://, http:// or mailto: address.'); return; }
    next ? chain().extendMarkRange('link').setLink({ href: next }).run() : chain().extendMarkRange('link').unsetLink().run();
    setLinkOpen(false); setLinkError('');
  };
  return <div className="session-note-editor" aria-busy={disabled}>
    <div className="session-note-toolbar" role="group" aria-label="Note formatting">
      <select aria-label="Paragraph style" disabled={disabled} value={editor.isActive('heading') ? String(editor.getAttributes('heading').level) : 'p'} onChange={e => e.target.value === 'p' ? chain().setParagraph().run() : chain().setHeading({ level: Number(e.target.value) }).run()}>
        <option value="p">Paragraph</option>{[1, 2, 3].map(n => <option value={n} key={n}>Heading {n}</option>)}
      </select>
      {button('Bold (Ctrl/Cmd+B)', () => chain().toggleBold().run(), editor.isActive('bold'), Bold)}
      {button('Italic (Ctrl/Cmd+I)', () => chain().toggleItalic().run(), editor.isActive('italic'), Italic)}
      {button('Underline (Ctrl/Cmd+U)', () => chain().toggleUnderline().run(), editor.isActive('underline'), Underline)}
      {button('Strikethrough', () => chain().toggleStrike().run(), editor.isActive('strike'), Strikethrough)}
      {button('Bullet list', () => chain().toggleBulletList().run(), editor.isActive('bulletList'), List)}
      {button('Numbered list', () => chain().toggleOrderedList().run(), editor.isActive('orderedList'), ListOrdered)}
      {button('Checklist', () => chain().toggleTaskList().run(), editor.isActive('taskList'), ListChecks)}
      {button('Undo', () => chain().undo().run(), undefined, Undo2, !editor.can().undo())}
      {button('Redo', () => chain().redo().run(), undefined, Redo2, !editor.can().redo())}
      {button('Clear formatting', () => chain().unsetAllMarks().clearNodes().resetAttributes('paragraph', ['indent', 'textAlign', 'lineHeight']).run(), undefined, RemoveFormatting)}
    </div>
    <details className="session-note-more">
      <summary>More formatting</summary>
      <div className="session-note-toolbar" role="group" aria-label="Additional note formatting">
        <select aria-label="Font" disabled={disabled} value={editor.getAttributes('textStyle').fontFamily || ''} onChange={e => e.target.value ? chain().setFontFamily(e.target.value).run() : chain().unsetFontFamily().run()}><option value="">Default font</option>{FONTS.map(font => <option key={font}>{font}</option>)}</select>
        <select aria-label="Font size" disabled={disabled} value={(editor.getAttributes('textStyle').fontSize || '').replace('px', '')} onChange={e => e.target.value ? chain().setFontSize(`${e.target.value}px`).run() : chain().unsetFontSize().run()}><option value="">Default size</option>{SIZES.map(n => <option key={n} value={n}>{n} px</option>)}</select>
        <label>Text color <input aria-label="Text color" type="color" disabled={disabled} value={editor.getAttributes('textStyle').color || '#222222'} onChange={e => chain().setColor(e.target.value).run()} /></label>
        <label>Highlight <input aria-label="Highlight color" type="color" disabled={disabled} value={editor.getAttributes('highlight').color || '#fff59d'} onChange={e => chain().setHighlight({ color: e.target.value }).run()} /></label>
        {button('Remove highlight', () => chain().unsetHighlight().run())}
        <select aria-label="Alignment" disabled={disabled} value={editor.getAttributes(editor.isActive('heading') ? 'heading' : 'paragraph').textAlign || 'left'} onChange={e => chain().setTextAlign(e.target.value).run()}>{['left', 'center', 'right', 'justify'].map(v => <option key={v}>{v}</option>)}</select>
        <select aria-label="Line spacing" disabled={disabled} value={editor.getAttributes(editor.isActive('heading') ? 'heading' : 'paragraph').lineHeight || ''} onChange={e => chain().updateAttributes('paragraph', { lineHeight: e.target.value || null }).updateAttributes('heading', { lineHeight: e.target.value || null }).run()}><option value="">Default spacing</option>{['1', '1.5', '2'].map(v => <option key={v} value={v}>Spacing {v}</option>)}</select>
        {button('Decrease indent', () => indent(-1))}{button('Increase indent', () => indent(1))}
        {button('Link', () => { setUrl(editor.getAttributes('link').href || ''); setLinkError(''); setLinkOpen(!linkOpen); }, editor.isActive('link'))}
        {button('Quote', () => chain().toggleBlockquote().run(), editor.isActive('blockquote'))}
        {button('Divider', () => chain().setHorizontalRule().run())}
        {button('Insert table', () => chain().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run())}
        {editor.isActive('table') && <select aria-label="Table actions" disabled={disabled} value="" onChange={e => { const command = e.target.value; if (command) chain()[command]().run(); }}>
          <option value="">Table actions…</option>{[['addRowBefore', 'Row above'], ['addRowAfter', 'Row below'], ['deleteRow', 'Delete row'], ['addColumnBefore', 'Column before'], ['addColumnAfter', 'Column after'], ['deleteColumn', 'Delete column'], ['mergeCells', 'Merge cells'], ['splitCell', 'Split cell'], ['toggleHeaderRow', 'Toggle header row'], ['deleteTable', 'Delete table']].map(([command, label]) => <option key={command} value={command} disabled={!editor.can()[command]()}>{label}</option>)}
        </select>}
      </div>
    </details>
    {linkOpen && <div className="session-note-link" onKeyDown={e => { if (e.key === 'Escape') { e.stopPropagation(); setLinkOpen(false); editor.commands.focus(); } if (e.key === 'Enter') { e.preventDefault(); applyLink(); } }}>
      <label>Link address<input aria-label="Link address" type="url" value={url} onChange={e => setUrl(e.target.value)} placeholder="https://example.com" autoFocus /></label>
      <button type="button" onClick={applyLink} disabled={disabled}>Apply link</button>
      <button type="button" onClick={() => { chain().extendMarkRange('link').unsetLink().run(); setLinkOpen(false); }} disabled={disabled}>Remove link</button>
      <button type="button" onClick={() => { setLinkOpen(false); editor.commands.focus(); }}>Cancel</button>
      {linkError && <span role="alert">{linkError}</span>}
    </div>}
    <EditorContent editor={editor} className="session-note-content" />
  </div>;
}

export function SessionNoteView({ value, className = '' }) {
  const editor = useEditor({ extensions: extensions(''), content: decodeNote(value), editable: false,
    editorProps: { attributes: { 'aria-label': 'Saved session notes' } } });
  useEffect(() => { editor?.commands.setContent(decodeNote(value), { emitUpdate: false }); }, [editor, value]);
  return <EditorContent editor={editor} className={`session-note-content session-note-view ${className}`} />;
}
