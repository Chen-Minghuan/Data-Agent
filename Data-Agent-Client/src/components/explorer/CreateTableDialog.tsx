import { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Plus,
  Trash2,
  ChevronUp,
  ChevronDown,
  FileText,
  ChevronLeft,
  ChevronRight,
  Settings,
  CheckCircle2,
  HelpCircle,
  ChevronDown as ChevronDownIcon,
  Copy,
  ClipboardPaste,
  MoreVertical,
  Square,
  Key,
  Link2,
  List,
  CheckSquare,
  Box,
  Layers,
} from 'lucide-react';
import {
  Dialog,
  DialogPortal,
  DialogOverlay,
  DialogClose,
} from '../ui/Dialog';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { ExplorerNodeType } from '../../constants/explorer';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { tableService } from '../../services/table.service';
import { useToast } from '../../hooks/useToast';
import type { ExplorerNode } from '../../types/explorer';
import type { DbConnection } from '../../types/connection';
import { resolveErrorMessage } from '../../lib/errorMessage';
import { cn } from '../../lib/utils';

const COMMON_TYPES = [
  'INT',
  'BIGINT',
  'VARCHAR(255)',
  'TEXT',
  'DATE',
  'DATETIME',
  'TIMESTAMP',
  'DECIMAL(10,2)',
  'BOOLEAN',
  'FLOAT',
  'DOUBLE',
];

export interface CreateTableColumn {
  name: string;
  type: string;
  nullable: boolean;
}

export interface CreateTableForeignKey {
  column: string;
  refTable: string;
  refColumn: string;
}

export interface CreateTableIndex {
  name: string;
  columns: string;
}

type TreeSectionId = 'columns' | 'keys' | 'foreign_keys' | 'indexes' | 'checks' | 'virtual_columns' | 'virtual_fk';

interface CreateTableDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  node: ExplorerNode | null;
  connections?: DbConnection[];
  onSuccess?: (node: ExplorerNode) => void;
}

function quoteIdentifier(name: string, dbType?: string): string {
  if (!name.trim()) return name;
  const isMysql = dbType?.toLowerCase().includes('mysql');
  return isMysql ? `\`${name}\`` : `"${name.replace(/"/g, '""')}"`;
}

const MIN_WIDTH = 520;
const MIN_HEIGHT = 400;
const DEFAULT_WIDTH = 800;
const DEFAULT_HEIGHT = 980;

const TREE_ICONS: Record<TreeSectionId, React.ReactNode> = {
  columns: <Square className="h-3.5 w-3.5 shrink-0 opacity-70" />,
  keys: <Key className="h-3.5 w-3.5 shrink-0 opacity-70" />,
  foreign_keys: <Link2 className="h-3.5 w-3.5 shrink-0 opacity-70" />,
  indexes: <List className="h-3.5 w-3.5 shrink-0 opacity-70" />,
  checks: <CheckSquare className="h-3.5 w-3.5 shrink-0 opacity-70" />,
  virtual_columns: <Box className="h-3.5 w-3.5 shrink-0 opacity-70" />,
  virtual_fk: <Layers className="h-3.5 w-3.5 shrink-0 opacity-70" />,
};

export function CreateTableDialog({
  open,
  onOpenChange,
  node,
  connections = [],
  onSuccess,
}: CreateTableDialogProps) {
  const { t } = useTranslation();
  const toast = useToast();
  const [tableName, setTableName] = useState('');
  const [tableComment, setTableComment] = useState('');
  const [selectedSection, setSelectedSection] = useState<TreeSectionId>('columns');
  const [checkedSections, setCheckedSections] = useState<Set<TreeSectionId>>(
    () => new Set(['columns', 'keys'])
  );
  const [columns, setColumns] = useState<CreateTableColumn[]>([
    { name: 'id', type: 'BIGINT', nullable: false },
  ]);
  const [foreignKeys, setForeignKeys] = useState<CreateTableForeignKey[]>([]);
  const [indexes, setIndexes] = useState<CreateTableIndex[]>([]);
  const [previewExpanded, setPreviewExpanded] = useState(true);
  const [isPending, setIsPending] = useState(false);
  const [leftRootExpanded, setLeftRootExpanded] = useState(true);
  const formScrollRef = useRef<HTMLDivElement>(null);

  const handleFormWheel = (e: React.WheelEvent) => {
    const el = formScrollRef.current;
    if (!el || el.scrollHeight <= el.clientHeight) return;
    const { deltaY } = e;
    if (deltaY !== 0) {
      el.scrollTop += deltaY;
      e.preventDefault();
    }
  };

  // Draggable state
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const dragRef = useRef<{ startX: number; startY: number; startLeft: number; startTop: number } | null>(null);

  const connectionId = node?.connectionId ? String(node.connectionId) : undefined;
  const catalog = node?.catalog ?? (node?.type === ExplorerNodeType.DB ? node.name : '');
  const schema = node?.schema ?? undefined;
  const conn = connections?.find((c) => String(c.id) === connectionId) ?? node?.dbConnection;
  const dbType = conn?.dbType;
  const connectionName = conn?.name ?? 'localhost';

  const displayTableName = tableName.trim() || 'table_name';
  const rootLabel = `${displayTableName} ${[catalog, schema].filter(Boolean).join('.') || ''} [${connectionName}]`.trim();

  const [size, setSize] = useState({ width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT });
  const resizeRef = useRef<{ startX: number; startY: number; startW: number; startH: number } | null>(null);

  const initPosition = useCallback(() => {
    setSize({ width: DEFAULT_WIDTH, height: DEFAULT_HEIGHT });
    const x = Math.max(0, (window.innerWidth - DEFAULT_WIDTH) / 2);
    const y = Math.max(0, (window.innerHeight - DEFAULT_HEIGHT) / 2);
    setPosition({ x, y });
  }, []);

  useEffect(() => {
    if (open) initPosition();
  }, [open, initPosition]);

  const handleHeaderMouseDown = (e: React.MouseEvent) => {
    if ((e.target as HTMLElement).closest('button, a, [role="button"]')) return;
    setIsDragging(true);
    dragRef.current = {
      startX: e.clientX,
      startY: e.clientY,
      startLeft: position.x,
      startTop: position.y,
    };
  };

  useEffect(() => {
    if (!isDragging) return;
    const onMove = (e: MouseEvent) => {
      if (!dragRef.current) return;
      const dx = e.clientX - dragRef.current.startX;
      const dy = e.clientY - dragRef.current.startY;
      setPosition({
        x: Math.max(0, dragRef.current.startLeft + dx),
        y: Math.max(0, dragRef.current.startTop + dy),
      });
    };
    const onUp = () => {
      setIsDragging(false);
      dragRef.current = null;
    };
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
    return () => {
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
    };
  }, [isDragging]);

  const handleResizeMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    resizeRef.current = {
      startX: e.clientX,
      startY: e.clientY,
      startW: size.width,
      startH: size.height,
    };
    const onMove = (e: MouseEvent) => {
      if (!resizeRef.current) return;
      const dw = e.clientX - resizeRef.current.startX;
      const dh = e.clientY - resizeRef.current.startY;
      setSize({
        width: Math.max(MIN_WIDTH, resizeRef.current.startW + dw),
        height: Math.max(MIN_HEIGHT, resizeRef.current.startH + dh),
      });
    };
    const onUp = () => {
      resizeRef.current = null;
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
    };
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
  };

  const resetForm = () => {
    setTableName('');
    setTableComment('');
    setSelectedSection('columns');
    setCheckedSections(new Set(['columns', 'keys']));
    setColumns([{ name: 'id', type: 'BIGINT', nullable: false }]);
    setForeignKeys([]);
    setIndexes([]);
    setPreviewExpanded(true);
    setLeftRootExpanded(true);
  };

  const handleClose = (openState: boolean) => {
    if (!openState) resetForm();
    onOpenChange(openState);
  };

  const addColumn = () => {
    setColumns((prev) => [...prev, { name: '', type: 'VARCHAR(255)', nullable: true }]);
  };

  const removeColumn = (index: number) => {
    setColumns((prev) => prev.filter((_, i) => i !== index));
  };

  const updateColumn = (index: number, field: keyof CreateTableColumn, value: string | boolean) => {
    setColumns((prev) =>
      prev.map((col, i) => (i === index ? { ...col, [field]: value } : col))
    );
  };

  const addForeignKey = () => {
    setForeignKeys((prev) => [...prev, { column: '', refTable: '', refColumn: '' }]);
  };
  const removeForeignKey = (index: number) => {
    setForeignKeys((prev) => prev.filter((_, i) => i !== index));
  };
  const updateForeignKey = (index: number, field: keyof CreateTableForeignKey, value: string) => {
    setForeignKeys((prev) =>
      prev.map((fk, i) => (i === index ? { ...fk, [field]: value } : fk))
    );
  };
  const addIndex = () => {
    setIndexes((prev) => [...prev, { name: '', columns: '' }]);
  };
  const removeIndex = (index: number) => {
    setIndexes((prev) => prev.filter((_, i) => i !== index));
  };
  const updateIndex = (index: number, field: keyof CreateTableIndex, value: string) => {
    setIndexes((prev) =>
      prev.map((idx, i) => (i === index ? { ...idx, [field]: value } : idx))
    );
  };

  const moveColumn = (index: number, dir: 'up' | 'down') => {
    const newIdx = dir === 'up' ? index - 1 : index + 1;
    if (newIdx < 0 || newIdx >= columns.length) return;
    setColumns((prev) => {
      const arr = [...prev];
      [arr[index], arr[newIdx]] = [arr[newIdx], arr[index]];
      return arr;
    });
  };

  const previewSql = useMemo(() => {
    const q = (s: string) => quoteIdentifier(s, dbType);
    const validCols = columns.filter((c) => c.name.trim());
    const tblName = tableName.trim() || 'table_name';
    if (validCols.length === 0) return `create table ${q(displayTableName)}\n(\n);`;
    const hasKeys = checkedSections.has('keys');
    const hasFk = checkedSections.has('foreign_keys');
    const hasIdx = checkedSections.has('indexes');
    const colDefs = validCols.map((c, idx) => {
      const nullPart = c.nullable ? '' : ' NOT NULL';
      const pkPart = hasKeys && idx === 0 && validCols[0].name.toLowerCase() === 'id' ? ' PRIMARY KEY' : '';
      return `  ${q(c.name.trim())} ${c.type}${nullPart}${pkPart}`;
    });
    const parts = [...colDefs];
    if (hasFk && foreignKeys.length > 0) {
      foreignKeys
        .filter((fk) => fk.column.trim() && fk.refTable.trim() && fk.refColumn.trim())
        .forEach((fk, i) => {
          parts.push(`  CONSTRAINT fk_${tblName}_${i + 1} FOREIGN KEY (${q(fk.column.trim())}) REFERENCES ${q(fk.refTable.trim())} (${q(fk.refColumn.trim())})`);
        });
    }
    if (hasIdx && indexes.length > 0) {
      indexes
        .filter((idx) => idx.name.trim() && idx.columns.trim())
        .forEach((idx) => {
          const cols = idx.columns.trim().split(/\s*,\s*/).map((c) => q(c.trim())).join(', ');
          parts.push(`  KEY ${q(idx.name.trim())} (${cols})`);
        });
    }
    return `create table ${q(tblName)}\n(\n${parts.join(',\n')}\n)`;
  }, [
    tableName,
    columns,
    foreignKeys,
    indexes,
    dbType,
    displayTableName,
    [...checkedSections].sort().join(','),
  ]);

  const buildCreateTableSql = (): string => previewSql;

  const validateTableName = (name: string): boolean => {
    return /^[a-zA-Z_][a-zA-Z0-9_]*$/.test(name.trim());
  };

  const validateColumnName = (name: string): boolean => {
    return name.trim().length > 0 && /^[a-zA-Z_][a-zA-Z0-9_]*$/.test(name.trim());
  };

  const handleSubmit = async () => {
    if (!connectionId || !tableName.trim()) return;
    if (!validateTableName(tableName)) {
      toast.error(t(I18N_KEYS.EXPLORER.CREATE_TABLE_FAILED) + ': ' + t(I18N_KEYS.EXPLORER.CREATE_TABLE_INVALID_NAME));
      return;
    }
    const validCols = columns.filter((c) => c.name.trim());
    if (validCols.length === 0) {
      toast.error(t(I18N_KEYS.EXPLORER.CREATE_TABLE_FAILED) + ': ' + t('explorer.create_table_need_columns'));
      return;
    }
    const invalidCol = validCols.find((c) => !validateColumnName(c.name));
    if (invalidCol) {
      toast.error(t(I18N_KEYS.EXPLORER.CREATE_TABLE_FAILED) + ': ' + t(I18N_KEYS.EXPLORER.CREATE_TABLE_INVALID_COLUMN, { name: invalidCol.name }));
      return;
    }

    const createSql = buildCreateTableSql().replace(/^create table/i, 'CREATE TABLE');

    setIsPending(true);
    try {
      const response = await tableService.createTable({
        connectionId: Number(connectionId),
        databaseName: catalog || undefined,
        schemaName: schema ?? undefined,
        sql: createSql,
      });

      if (response.type === 'ERROR' || !response.success) {
        const errMsg = response.messages?.[0]?.message ?? response.errorMessage ?? 'Unknown error';
        toast.error(t(I18N_KEYS.EXPLORER.CREATE_TABLE_FAILED) + ': ' + errMsg);
        return;
      }

      toast.success(t(I18N_KEYS.EXPLORER.CREATE_TABLE_SUCCESS));
      handleClose(false);
      onSuccess?.(node!);
    } catch (err) {
      toast.error(t(I18N_KEYS.EXPLORER.CREATE_TABLE_FAILED) + ': ' + resolveErrorMessage(err, t(I18N_KEYS.EXPLORER.CREATE_TABLE_FAILED)));
    } finally {
      setIsPending(false);
    }
  };

  if (!node) return null;

  const treeSections: { id: TreeSectionId; labelKey: string; enabled: boolean }[] = [
    { id: 'columns', labelKey: 'explorer.create_dialog_columns', enabled: true },
    { id: 'keys', labelKey: 'explorer.create_dialog_keys', enabled: true },
    { id: 'foreign_keys', labelKey: 'explorer.create_dialog_foreign_keys', enabled: true },
    { id: 'indexes', labelKey: 'explorer.create_dialog_indexes', enabled: true },
    { id: 'checks', labelKey: 'explorer.create_dialog_checks', enabled: false },
    { id: 'virtual_columns', labelKey: 'explorer.create_dialog_virtual_columns', enabled: false },
    { id: 'virtual_fk', labelKey: 'explorer.create_dialog_virtual_fk', enabled: false },
  ];

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogPortal>
        <DialogOverlay />
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="create-table-title"
          className={cn(
            "fixed z-50 flex flex-col border border-border bg-background shadow-lg rounded-lg overflow-hidden",
            "data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0"
          )}
          style={{
            left: position.x,
            top: position.y,
            width: size.width,
            height: size.height,
            minWidth: MIN_WIDTH,
            minHeight: MIN_HEIGHT,
          }}
        >
          {/* Header - drag handle */}
          <div
            className="flex items-center justify-between px-4 py-2.5 pr-12 border-b border-border shrink-0 select-none"
            onMouseDown={handleHeaderMouseDown}
          >
            <h2 id="create-table-title" className="text-base font-semibold">
              {t('explorer.create_dialog_title')}
            </h2>
            <DialogClose asChild>
              <Button variant="ghost" size="icon" className="absolute right-2 top-2.5 h-8 w-8 rounded-sm opacity-70 hover:opacity-100">
                <span className="sr-only">Close</span>
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
              </Button>
            </DialogClose>
          </div>

          {/* Toolbar - DataGrip style */}
          <div className="flex items-center gap-0.5 px-3 py-1.5 border-b border-border shrink-0 bg-muted/5">
            <Button variant="ghost" size="icon" className="h-8 w-8 rounded -ml-1" onClick={addColumn} title={t(I18N_KEYS.EXPLORER.CREATE_TABLE_ADD_COLUMN)}>
              <Plus className="h-5 w-5" />
            </Button>
            <div className="w-px h-5 bg-border mx-0.5" />
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded" title={t(I18N_KEYS.EXPLORER.CREATE_TABLE_REMOVE_COLUMN)}>
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded" title="复制">
              <Copy className="h-3.5 w-3.5" />
            </Button>
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded" title="粘贴">
              <ClipboardPaste className="h-3.5 w-3.5" />
            </Button>
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded" title="上移">
              <ChevronUp className="h-3.5 w-3.5" />
            </Button>
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded" title="下移">
              <ChevronDown className="h-3.5 w-3.5" />
            </Button>
            <div className="w-px h-5 bg-border mx-0.5" />
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded">
              <ChevronLeft className="h-3.5 w-3.5" />
            </Button>
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded">
              <ChevronRight className="h-3.5 w-3.5" />
            </Button>
            <div className="flex items-center gap-1.5 ml-3">
              <FileText className="h-4 w-4 text-muted-foreground shrink-0" />
              <span className="text-sm text-foreground">{displayTableName}</span>
            </div>
            <div className="flex-1 min-w-2" />
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded">
              <MoreVertical className="h-4 w-4" />
            </Button>
          </div>

          {/* Main: Left tree + Right form */}
          <div className="create-table-dialog-form flex flex-1 min-h-0 overflow-hidden">
            {/* Left: Tree panel */}
            <div className="w-[220px] border-r border-border flex flex-col shrink-0 bg-muted/5">
              <button
                type="button"
                className="flex items-center gap-1.5 w-full px-3 py-2 text-left text-xs font-medium text-muted-foreground hover:bg-muted/50 truncate"
                onClick={() => setLeftRootExpanded((e) => !e)}
              >
                <ChevronDownIcon className={cn("h-4 w-4 shrink-0 transition-transform", !leftRootExpanded && "-rotate-90")} />
                <span className="truncate" title={rootLabel}>{rootLabel}</span>
              </button>
              {leftRootExpanded && (
                <div className="flex-1 overflow-y-auto py-1 px-1 text-xs">
                  {treeSections.map((s) => (
                    <div
                      key={s.id}
                      className={cn(
                        "flex items-center gap-2 py-1.5 px-2 rounded cursor-default",
                        selectedSection === s.id ? "bg-primary/15 text-foreground" : "",
                        !s.enabled ? "opacity-50" : "hover:bg-muted/80"
                      )}
                      onClick={() => s.enabled && setSelectedSection(s.id)}
                    >
                      <input
                        type="checkbox"
                        checked={checkedSections.has(s.id)}
                        onChange={(e) => {
                          e.stopPropagation();
                          setCheckedSections((prev) => {
                            const next = new Set(prev);
                            if (next.has(s.id)) next.delete(s.id);
                            else next.add(s.id);
                            return next;
                          });
                        }}
                        onClick={(e) => e.stopPropagation()}
                        className=""
                      />
                      {TREE_ICONS[s.id]}
                      <span>{t(s.labelKey)}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Right: Form area */}
            <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
              <div className="flex items-center gap-2 px-4 py-3 border-b border-border">
                <FileText className="h-4 w-4 text-muted-foreground shrink-0" />
                <span className="text-sm font-medium">{displayTableName}</span>
              </div>
              <div
                className="flex-1 overflow-y-auto overflow-x-hidden p-3 overscroll-contain"
                ref={formScrollRef}
                onWheel={handleFormWheel}
              >
                <div className="space-y-2">
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_name')}</label>
                    <div className="flex items-center gap-1">
                      <Input
                        value={tableName}
                        onChange={(e) => setTableName(e.target.value)}
                        placeholder={t(I18N_KEYS.EXPLORER.CREATE_TABLE_NAME_PLACEHOLDER)}
                        className="h-8 text-sm flex-1"
                      />
                      <Button variant="ghost" size="icon" className="h-8 w-8 shrink-0">...</Button>
                    </div>
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_comment')}</label>
                    <Input
                      value={tableComment}
                      onChange={(e) => setTableComment(e.target.value)}
                      placeholder=""
                      className="h-8 text-sm"
                    />
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_persistence')}</label>
                    <div className="flex items-center gap-2">
                      <select className="h-8 text-sm border rounded-md px-2 flex-1">
                        <option value="PERSISTENT">PERSISTENT</option>
                        <option value="UNLOGGED">UNLOGGED</option>
                        <option value="TEMPORARY">TEMPORARY</option>
                      </select>
                      <label className="flex items-center gap-1.5 text-xs shrink-0">
                        <input type="checkbox" className="rounded border border-border bg-muted/80 accent-primary" />
                        {t('explorer.create_dialog_has_oid')}
                      </label>
                    </div>
                  </div>
                  {selectedSection === 'columns' && (
                    <div className="grid grid-cols-[100px_1fr] items-start gap-x-3 gap-y-1">
                      <label className="text-xs text-muted-foreground pt-1.5 leading-8">
                        {t(I18N_KEYS.EXPLORER.CREATE_TABLE_COLUMNS)}
                      </label>
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-2">
                          <Button type="button" variant="outline" size="sm" className="h-8 text-xs shrink-0" onClick={addColumn}>
                            <Plus className="w-3.5 h-3.5 mr-1" />
                            {t(I18N_KEYS.EXPLORER.CREATE_TABLE_ADD_COLUMN)}
                          </Button>
                        </div>
                        <div className="border border-input rounded-md divide-y divide-border overflow-y-auto max-h-44">
                          {columns.map((col, index) => (
                            <div key={index} className="flex items-center gap-2 px-2 py-1 bg-muted/10 min-h-[36px]">
                              <Input
                                value={col.name}
                                onChange={(e) => updateColumn(index, 'name', e.target.value)}
                                placeholder={t(I18N_KEYS.EXPLORER.CREATE_TABLE_COLUMN_NAME)}
                                className="h-8 text-sm flex-1 min-w-0 shrink-0"
                              />
                              <select
                                value={col.type}
                                onChange={(e) => updateColumn(index, 'type', e.target.value)}
                                className="h-8 text-sm border rounded-md px-2.5 min-w-[110px] shrink-0"
                              >
                                {COMMON_TYPES.map((type) => (
                                  <option key={type} value={type}>{type}</option>
                                ))}
                              </select>
                              <label className="flex items-center gap-1.5 shrink-0 text-sm cursor-default">
                                <input
                                  type="checkbox"
                                  checked={col.nullable}
                                  onChange={(e) => updateColumn(index, 'nullable', e.target.checked)}
                                  className=""
                                />
                                {t(I18N_KEYS.EXPLORER.CREATE_TABLE_COLUMN_NULLABLE)}
                              </label>
                              <div className="flex items-center shrink-0 border-l border-border pl-1">
                                <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => moveColumn(index, 'up')} disabled={index === 0}>
                                  <ChevronUp className="w-3.5 h-3.5" />
                                </Button>
                                <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => moveColumn(index, 'down')} disabled={index === columns.length - 1}>
                                  <ChevronDown className="w-3.5 h-3.5" />
                                </Button>
                              </div>
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7 text-destructive hover:text-destructive shrink-0"
                                onClick={() => removeColumn(index)}
                                disabled={columns.length <= 1}
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </Button>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}
                  {selectedSection === 'foreign_keys' && (
                    <div className="grid grid-cols-[100px_1fr] items-start gap-x-3 gap-y-1">
                      <label className="text-xs text-muted-foreground pt-1.5 leading-8">
                        {t('explorer.create_dialog_foreign_keys')}
                      </label>
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-2">
                          <Button type="button" variant="outline" size="sm" className="h-8 text-xs shrink-0" onClick={addForeignKey}>
                            <Plus className="w-3.5 h-3.5 mr-1" />
                            {t('explorer.create_dialog_add_foreign_key')}
                          </Button>
                        </div>
                        <div className="border border-input rounded-md divide-y divide-border overflow-y-auto max-h-44">
                          {foreignKeys.map((fk, index) => (
                            <div key={index} className="flex items-center gap-2 px-2 py-1 bg-muted/10 min-h-[36px]">
                              <Input
                                value={fk.column}
                                onChange={(e) => updateForeignKey(index, 'column', e.target.value)}
                                placeholder={t('explorer.create_table_column_name')}
                                className="h-8 text-sm flex-1 min-w-0"
                              />
                              <span className="text-muted-foreground shrink-0">→</span>
                              <Input
                                value={fk.refTable}
                                onChange={(e) => updateForeignKey(index, 'refTable', e.target.value)}
                                placeholder={t('explorer.create_dialog_fk_ref_table')}
                                className="h-8 text-sm flex-1 min-w-0"
                              />
                              <Input
                                value={fk.refColumn}
                                onChange={(e) => updateForeignKey(index, 'refColumn', e.target.value)}
                                placeholder="引用列"
                                className="h-8 text-sm flex-1 min-w-0"
                              />
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7 text-destructive shrink-0"
                                onClick={() => removeForeignKey(index)}
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </Button>
                            </div>
                          ))}
                          {foreignKeys.length === 0 && (
                            <div className="py-4 px-2 text-center text-xs text-muted-foreground">
                              {t('explorer.create_dialog_no_content')}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                  {selectedSection === 'indexes' && (
                    <div className="grid grid-cols-[100px_1fr] items-start gap-x-3 gap-y-1">
                      <label className="text-xs text-muted-foreground pt-1.5 leading-8">
                        {t('explorer.create_dialog_indexes')}
                      </label>
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-2">
                          <Button type="button" variant="outline" size="sm" className="h-8 text-xs shrink-0" onClick={addIndex}>
                            <Plus className="w-3.5 h-3.5 mr-1" />
                            {t('explorer.create_dialog_add_index')}
                          </Button>
                        </div>
                        <div className="border border-input rounded-md divide-y divide-border overflow-y-auto max-h-44">
                          {indexes.map((idx, index) => (
                            <div key={index} className="flex items-center gap-2 px-2 py-1 bg-muted/10 min-h-[36px]">
                              <Input
                                value={idx.name}
                                onChange={(e) => updateIndex(index, 'name', e.target.value)}
                                placeholder={t('explorer.create_dialog_index_name')}
                                className="h-8 text-sm flex-1 min-w-0"
                              />
                              <Input
                                value={idx.columns}
                                onChange={(e) => updateIndex(index, 'columns', e.target.value)}
                                placeholder={t('explorer.create_dialog_index_columns')}
                                className="h-8 text-sm flex-1 min-w-0"
                              />
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7 text-destructive shrink-0"
                                onClick={() => removeIndex(index)}
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </Button>
                            </div>
                          ))}
                          {indexes.length === 0 && (
                            <div className="py-4 px-2 text-center text-xs text-muted-foreground">
                              {t('explorer.create_dialog_no_content')}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_partition_expression')}</label>
                    <Input placeholder="" className="h-8 text-sm" />
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_partition_key')}</label>
                    <Input placeholder="" className="h-8 text-sm" />
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_options')}</label>
                    <div className="flex items-center gap-1">
                      <Input placeholder="" className="h-8 text-sm flex-1" />
                      <Button variant="ghost" size="icon" className="h-8 w-8 shrink-0 opacity-60">...</Button>
                    </div>
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_access_method')}</label>
                    <select className="h-8 text-sm border rounded-md px-2 flex-1">
                      <option value="">-</option>
                    </select>
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_tablespace')}</label>
                    <select className="h-8 text-sm border rounded-md px-2 flex-1">
                      <option value="">-</option>
                    </select>
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_owner')}</label>
                    <select className="h-8 text-sm border rounded-md px-2 flex-1">
                      <option value="">-</option>
                    </select>
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-start gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground pt-1.5">{t('explorer.create_dialog_authorization')}</label>
                    <div className="space-y-1">
                      <div className="flex items-center gap-1">
                        <Button variant="ghost" size="icon" className="h-7 w-7">
                          <Plus className="h-3.5 w-3.5" />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-7 w-7">
                          <ChevronUp className="h-3.5 w-3.5" />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-7 w-7">
                          <ChevronDown className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                      <div className="rounded-md border border-dashed border-border py-6 px-4 text-center text-xs text-muted-foreground">
                        {t('explorer.create_dialog_no_content')}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Bottom: SQL Preview */}
          <div className="border-t border-border shrink-0">
            <button
              type="button"
              className="w-full flex items-center justify-between px-4 py-2 hover:bg-muted/50 text-left text-sm"
              onClick={() => setPreviewExpanded((e) => !e)}
            >
              <span className="flex items-center gap-1">
                {t('explorer.create_dialog_preview')}
                <ChevronDownIcon className={cn("h-4 w-4 transition-transform", !previewExpanded && "-rotate-90")} />
              </span>
              <div className="flex items-center gap-2">
                <Button variant="ghost" size="icon" className="h-6 w-6">
                  <Settings className="h-3.5 w-3.5 opacity-70" />
                </Button>
                <CheckCircle2 className="h-4 w-4 text-green-500" />
              </div>
            </button>
            {previewExpanded && (
              <div className="px-4 pb-3 shrink-0">
                <pre className="text-xs bg-muted/30 rounded-md p-3 overflow-x-auto font-mono max-h-32 overflow-y-auto border border-border">
                  {buildCreateTableSql()}
                </pre>
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="flex items-center justify-between px-4 py-3 border-t border-border shrink-0 relative">
            <Button variant="ghost" size="icon" className="h-8 w-8">
              <HelpCircle className="h-4 w-4" />
            </Button>
            <div className="flex gap-2">
              <Button variant="outline" onClick={() => handleClose(false)}>
                {t(I18N_KEYS.CONNECTIONS.CANCEL)}
              </Button>
              <Button disabled={isPending || !tableName.trim()} onClick={handleSubmit}>
                {isPending ? t(I18N_KEYS.CONNECTIONS.SAVING) : t(I18N_KEYS.EXPLORER.CREATE_DIALOG_CONFIRM)}
              </Button>
            </div>
          </div>

          {/* Resize handle */}
          <div
            className="absolute right-0 bottom-0 w-5 h-5 cursor-se-resize select-none flex items-end justify-end z-10"
            onMouseDown={handleResizeMouseDown}
            title={t('explorer.create_dialog_resize')}
          >
            <svg className="w-3.5 h-3.5 text-muted-foreground/40 mb-0.5 mr-0.5" viewBox="0 0 16 16">
              <path d="M14 14v-4M14 14h-4" stroke="currentColor" strokeWidth="1.2" fill="none" strokeLinecap="round" />
            </svg>
          </div>
        </div>
      </DialogPortal>
    </Dialog>
  );
}
