import Editor, { loader } from '@monaco-editor/react';

// You can configure the loader to use a specific version or CDN
loader.config({ paths: { vs: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.43.0/min/vs' } });

interface MonacoEditorProps {
  value: string;
  onChange?: (value: string | undefined) => void;
  language?: string;
  readOnly?: boolean;
  height?: string | number;
  theme?: 'jetbrains-dark' | 'vs-dark' | 'light';
}

export function MonacoEditor({
  value,
  onChange,
  language = 'sql',
  readOnly = false,
  height = '100%',
  theme = 'jetbrains-dark'
}: MonacoEditorProps) {
  
  const handleEditorWillMount = (monaco: any) => {
    // Define JetBrains-like theme
    monaco.editor.defineTheme('jetbrains-dark', {
      base: 'vs-dark',
      inherit: true,
      rules: [
        { token: 'keyword', foreground: 'cc7832', fontStyle: 'bold' },
        { token: 'string', foreground: '6a8759' },
        { token: 'function', foreground: 'ffc66d' },
        { token: 'number', foreground: '6897bb' },
        { token: 'comment', foreground: '808080', fontStyle: 'italic' },
        { token: 'operator', foreground: 'a9b7c6' },
        { token: 'delimiter', foreground: 'a9b7c6' },
      ],
      colors: {
        'editor.background': '#1e1f22',
        'editor.foreground': '#bcbec4',
        'editor.lineHighlightBackground': '#2e2f33',
        'editor.selectionBackground': '#2e436e',
        'editorCursor.foreground': '#a9b7c6',
        'editorLineNumber.foreground': '#606366',
        'editorLineNumber.activeForeground': '#a9b7c6',
        'editorIndentGuide.background': '#373737',
        'editor.border': '#4e5155',
      }
    });
  };

  return (
    <Editor
      height={height}
      language={language}
      value={value}
      theme={theme}
      onChange={onChange}
      beforeMount={handleEditorWillMount}
      options={{
        readOnly,
        minimap: { enabled: false },
        fontSize: 13,
        fontFamily: "'JetBrains Mono', monospace",
        lineHeight: 1.5,
        scrollBeyondLastLine: false,
        automaticLayout: true,
        padding: { top: 10, bottom: 10 },
        renderLineHighlight: 'all',
        scrollbar: {
          vertical: 'visible',
          horizontal: 'visible',
          verticalScrollbarSize: 10,
          horizontalScrollbarSize: 10,
        }
      }}
    />
  );
}
