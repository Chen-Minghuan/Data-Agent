import { Outlet, NavLink, useLocation } from 'react-router-dom';
import { User, Shield, Lock, ChevronRight } from 'lucide-react';
import { cn } from '../lib/utils';

// TODO: 支持多级 Settings 子菜单（如 /settings/profile/security），
// 导航高亮和结构需要在后续迭代时扩展。
const navItems = [
    { path: '/settings/profile', label: 'Profile', icon: User },
    { path: '/settings/password', label: 'Security', icon: Lock },
    { path: '/settings/sessions', label: 'Sessions', icon: Shield },
] as const;

export default function Settings() {
    const location = useLocation();

    return (
        <div className="max-w-5xl mx-auto">
            {/* Page Header */}
            <div className="mb-8">
                <h1 className="text-3xl font-bold">Settings</h1>
                <p className="text-muted-foreground mt-1">
                    Manage your account settings and preferences
                </p>
            </div>

            <div className="flex flex-col md:flex-row gap-8">
                {/* Sidebar Navigation */}
                <nav className="w-full md:w-64 flex-shrink-0">
                    <div className="bg-card rounded-lg border border-border overflow-hidden">
                        <div className="p-4 border-b border-border bg-muted/30">
                            <h2 className="font-medium text-sm">Account</h2>
                        </div>
                        <ul className="p-2 space-y-1">
                            {navItems.map((item) => {
                                const Icon = item.icon;
                                const isActive = location.pathname === item.path;

                                return (
                                    <li key={item.path}>
                                        <NavLink
                                            to={item.path}
                                            className={cn(
                                                'flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors',
                                                isActive
                                                    ? 'bg-primary/10 text-primary font-medium'
                                                    : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                                            )}
                                        >
                                            <Icon className="h-4 w-4" />
                                            {item.label}
                                            {isActive && (
                                                <ChevronRight className="h-3 w-3 ml-auto" />
                                            )}
                                        </NavLink>
                                    </li>
                                );
                            })}
                        </ul>
                    </div>
                </nav>

                {/* Main Content Area */}
                <main className="flex-1 min-w-0">
                    <div className="bg-card rounded-lg border border-border p-6">
                        <Outlet />
                    </div>
                </main>
            </div>
        </div>
    );
}
