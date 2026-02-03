import React from 'react';
import { ChevronRight, ChevronDown, MoreVertical } from 'lucide-react';
import { cn } from '../../lib/utils';

export interface TreeNode {
  id: string;
  name: string;
  type: string;
  children?: TreeNode[];
  expanded?: boolean;
  icon?: React.ReactNode;
}

interface TreeProps {
  nodes: TreeNode[];
  onToggle: (nodeId: string) => void;
  renderIcon: (type: string, expanded?: boolean) => React.ReactNode;
  onNodeClick?: (node: TreeNode) => void;
  level?: number;
}

export function Tree({ nodes, onToggle, renderIcon, onNodeClick, level = 0 }: TreeProps) {
  return (
    <>
      {nodes.map((node) => (
        <div key={node.id}>
          <div 
            className={cn(
              "flex items-center py-1 px-2 hover:bg-accent/50 cursor-pointer group select-none text-xs",
              node.expanded && "bg-accent/30"
            )}
            style={{ paddingLeft: `${level * 12 + 8}px` }}
            onClick={() => {
              if (node.children) {
                onToggle(node.id);
              }
              onNodeClick?.(node);
            }}
          >
            <span className="w-4 flex items-center justify-center mr-1">
              {node.children && (
                node.expanded ? <ChevronDown className="w-3 h-3" /> : <ChevronRight className="w-3 h-3" />
              )}
            </span>
            <span className="mr-2">
              {renderIcon(node.type, node.expanded)}
            </span>
            <span className="flex-1 truncate theme-text-primary">
              {node.name}
            </span>
            <div className="opacity-0 group-hover:opacity-100 flex items-center space-x-1">
              <button className="p-0.5 hover:bg-accent rounded" onClick={(e) => { e.stopPropagation(); /* More actions */ }}>
                <MoreVertical className="w-3 h-3 theme-text-secondary" />
              </button>
            </div>
          </div>
          
          {node.expanded && node.children && (
            <Tree 
              nodes={node.children} 
              onToggle={onToggle} 
              renderIcon={renderIcon} 
              onNodeClick={onNodeClick} 
              level={level + 1} 
            />
          )}
        </div>
      ))}
    </>
  );
}
