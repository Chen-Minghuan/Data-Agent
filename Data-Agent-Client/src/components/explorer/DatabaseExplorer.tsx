import { useState, useEffect } from 'react';
import { Database, Folder, Table, ChevronRight, ChevronDown, Plus, Search, RefreshCw } from 'lucide-react';
import { cn } from '../../lib/utils';
import { useWorkspaceStore } from '../../store/workspaceStore';
import { useTranslation } from 'react-i18next';
import { Tree, TreeNode } from '../common/Tree';

export function DatabaseExplorer() {
  const { t } = useTranslation();
  const { connections, isConnectionsLoading: loading, fetchConnections } = useWorkspaceStore();
  const [nodes, setNodes] = useState<TreeNode[]>([]);

  useEffect(() => {
    fetchConnections();
  }, []);

  useEffect(() => {
    // Convert connections to initial root nodes
    const rootNodes: TreeNode[] = connections.map(conn => ({
      id: `conn-${conn.id}`,
      name: conn.name,
      type: 'root',
      expanded: false,
      children: [] // Will be loaded on demand
    }));
    setNodes(rootNodes);
  }, [connections]);

  const handleRefresh = () => {
    fetchConnections(true);
  };

  const toggleNode = (nodeId: string) => {
    setNodes(prevNodes => {
      const updateNodes = (list: TreeNode[]): TreeNode[] => {
        return list.map(node => {
          if (node.id === nodeId) {
            return { ...node, expanded: !node.expanded };
          }
          if (node.children) {
            return { ...node, children: updateNodes(node.children) };
          }
          return node;
        });
      };
      return updateNodes(prevNodes);
    });
  };

  const renderIcon = (type: string, expanded?: boolean) => {
    switch (type) {
      case 'root':
        return <Database className="w-3.5 h-3.5 text-blue-400" />;
      case 'db':
        return <Database className="w-3.5 h-3.5 text-teal-400" />;
      case 'folder':
      case 'schema':
        return expanded ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />;
      case 'table':
        return <Table className="w-3.5 h-3.5 text-green-400" />;
      default:
        return <Folder className="w-3.5 h-3.5 text-gray-400" />;
    }
  };

  return (
    <div className="flex flex-col h-full overflow-hidden">
      <div className="flex items-center justify-between px-3 py-2 theme-text-secondary text-[10px] uppercase font-bold tracking-wider border-b theme-border shrink-0">
        <span>{t('explorer.title')}</span>
        <div className="flex items-center space-x-2">
          <button onClick={handleRefresh} title={t('common.refresh')}>
            <RefreshCw className={cn("w-3 h-3 hover:text-blue-500 transition-colors", loading && "animate-spin")} />
          </button>
          <button title={t('common.add')}>
            <Plus className="w-3.5 h-3.5 hover:text-blue-500 cursor-pointer" />
          </button>
        </div>
      </div>

      <div className="p-2 shrink-0">
        <div className="relative">
          <Search className="absolute left-2 top-1/2 -translate-y-1/2 w-3 h-3 theme-text-secondary" />
          <input 
            type="text" 
            placeholder={t('common.search')} 
            className="w-full bg-accent/30 border theme-border rounded px-7 py-1 text-[11px] focus:outline-none focus:border-primary/50"
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto no-scrollbar py-1">
        {nodes.length === 0 && !loading ? (
          <div className="p-4 text-center text-xs theme-text-secondary opacity-50">
            {t('common.no_connections')}
          </div>
        ) : (
          <Tree nodes={nodes} onToggle={toggleNode} renderIcon={renderIcon} />
        )}
      </div>
    </div>
  );
}

