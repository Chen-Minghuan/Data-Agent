import { MessageSquarePlus, Clock, ListTodo } from 'lucide-react';

export interface SlashCommandItem {
  id: string;
  slug: string;
  label: string;
  icon?: React.ReactNode;
}

export const SLASH_COMMAND_IDS = {
  NEW: 'new',
  HISTORY: 'history',
  PLAN: 'plan',
} as const;

export const SLASH_COMMANDS: SlashCommandItem[] = [
  {
    id: SLASH_COMMAND_IDS.NEW,
    slug: SLASH_COMMAND_IDS.NEW,
    label: 'New conversation',
    icon: <MessageSquarePlus className="w-3.5 h-3.5 shrink-0" />,
  },
  {
    id: SLASH_COMMAND_IDS.HISTORY,
    slug: SLASH_COMMAND_IDS.HISTORY,
    label: 'Show conversation history',
    icon: <Clock className="w-3.5 h-3.5 shrink-0" />,
  },
  {
    id: SLASH_COMMAND_IDS.PLAN,
    slug: SLASH_COMMAND_IDS.PLAN,
    label: 'Show plans in conversation',
    icon: <ListTodo className="w-3.5 h-3.5 shrink-0" />,
  },
];
