import { useNavigate } from "react-router-dom";
import { Button } from "../ui/Button";
import { ThemeSwitcher } from "../common/ThemeSwitcher";
import { useAuthStore } from "../../store/authStore";
import { authService } from "../../services/auth.service";
import { LogOut } from "lucide-react";

interface HeaderProps {
    onLoginClick: () => void;
}

export function Header({ onLoginClick }: HeaderProps) {
    const navigate = useNavigate();
    const { user, accessToken, clearAuth } = useAuthStore();

    const handleLogout = async () => {
        try {
            await authService.logout();
            clearAuth();
            navigate('/');
        } catch (error) {
            console.error("Logout failed", error);
        }
    };

    const userInitial = user?.username?.charAt(0).toUpperCase() || user?.email?.charAt(0).toUpperCase() || "?";

    return (
        <>
            <header className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
                <div className="container flex h-14 max-w-screen-2xl items-center justify-between px-4 mx-auto">
                    <div className="flex items-center gap-2">
                        <span className="text-xl font-bold bg-gradient-to-r from-blue-600 to-blue-400 bg-clip-text text-transparent">
                            Data Agent
                        </span>
                    </div>

                    <div className="flex items-center gap-4">
                        {accessToken ? (
                            <div className="flex items-center gap-3">
                                <button
                                    onClick={() => navigate("/settings")}
                                    className="flex items-center gap-2 px-2 py-1 rounded-full bg-muted/50 border border-border hover:bg-muted transition-colors cursor-pointer"
                                    title="Open Settings"
                                >
                                    {user?.avatarUrl ? (
                                        <img src={user.avatarUrl} alt={user.username} className="h-6 w-6 rounded-full object-cover" />
                                    ) : (
                                        <div className="h-6 w-6 rounded-full bg-primary/10 flex items-center justify-center text-[10px] font-bold text-primary">
                                            {userInitial}
                                        </div>
                                    )}
                                    <span className="text-sm font-medium hidden sm:inline-block max-w-[100px] truncate">
                                        {user?.username || user?.email}
                                    </span>
                                </button>
                                <Button variant="ghost" size="icon" onClick={handleLogout} title="Logout">
                                    <LogOut className="h-4 w-4" />
                                </Button>
                            </div>
                        ) : (
                            <Button variant="ghost" size="sm" onClick={onLoginClick}>
                                Login
                            </Button>
                        )}
                        <ThemeSwitcher />
                    </div>
                </div>
            </header>
        </>
    );
}
