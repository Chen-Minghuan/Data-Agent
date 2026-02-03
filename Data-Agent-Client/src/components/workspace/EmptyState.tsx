import { useTranslation } from 'react-i18next';

export function EmptyState() {
    const { t } = useTranslation();
    
    const shortcuts = [
        { label: t('common.execute_query'), keys: 'Ctrl+Enter / Cmd+Enter' },
        { label: t('common.insert_indent'), keys: 'Tab' },
        { label: t('common.open_settings'), keys: 'Ctrl+Shift+, / Cmd+Shift+,' },
        { label: t('common.close_explorer'), keys: 'Esc' },
        { label: t('common.toggle_ai'), keys: 'Ctrl+B / Cmd+B' },
    ];

    return (
        <div className="flex-1 h-full flex flex-col items-center justify-center theme-bg-main select-none">
            <div className="text-base theme-text-secondary space-y-3 text-left">
                <div className="space-y-2">
                    {shortcuts.map((s, i) => (
                        <div key={i} className="flex gap-6">
                            <span className="theme-text-primary w-56">{s.label}</span>
                            <span className="font-mono text-sm">{s.keys}</span>
                        </div>
                    ))}
                </div>
                <div className="mt-4 text-sm">
                    {t('common.drag_hint')}
                </div>
            </div>
        </div>
    );
}
