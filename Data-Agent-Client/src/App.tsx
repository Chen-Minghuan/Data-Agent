import { useState } from "react";
import { BrowserRouter as Router, useRoutes } from "react-router-dom";
import { Dialog } from "./components/ui/Dialog";
import { LoginModal } from "./components/common/LoginModal";
import { RegisterModal } from "./components/common/RegisterModal";
import { ThemeProvider } from "./hooks/useTheme";
import { Header } from "./components/layouts/Header";
import { ToastContainer } from "./components/ui/Toast";
import { useAuthStore } from "./store/authStore";
import { routerConfig } from "./router.tsx";
import { useOAuthCallbackFromUrl } from "./hooks/useOAuthCallbackFromUrl";

function AppRoutes() {
    const element = useRoutes(routerConfig);
    return element;
}

function App() {
    const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
    const [modalType, setModalType] = useState<"login" | "register">("login");
    const { isLoginModalOpen, closeLoginModal } = useAuthStore();

    // 处理 OAuth 登录回调：从 URL 读取 token 同步到 authStore
    useOAuthCallbackFromUrl();

    const handleSwitchToRegister = () => {
        setModalType("register");
    };

    const handleSwitchToLogin = () => {
        setModalType("login");
    };

    return (
        <ThemeProvider>
            <Router>
                <div className="min-h-screen bg-background text-foreground transition-colors duration-300">
                    <Header onLoginClick={() => {
                        setModalType("login");
                        setIsAuthModalOpen(true);
                    }} />
                    <main className="container mx-auto px-4 py-8">
                        <AppRoutes />
                        <Dialog
                            open={isAuthModalOpen || isLoginModalOpen}
                            onOpenChange={(open) => {
                                setIsAuthModalOpen(open);
                                if (!open) {
                                    closeLoginModal();
                                } else if (open && isLoginModalOpen) {
                                    setModalType("login");
                                }
                            }}
                        >
                            {modalType === "login" ? (
                                <LoginModal
                                    onSwitchToRegister={handleSwitchToRegister}
                                    onClose={() => {
                                        setIsAuthModalOpen(false);
                                        closeLoginModal();
                                    }}
                                />
                            ) : (
                                <RegisterModal onSwitchToLogin={handleSwitchToLogin} />
                            )}
                        </Dialog>
                    </main>
                    <ToastContainer />
                </div>
            </Router>
        </ThemeProvider>
    );
}

export default App;
