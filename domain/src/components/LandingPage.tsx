// landing-page.tsx
import Sidebar from "./Sidebar";
import { sidebarElements } from "../types/SidebarSections";

function LandingPage() {
    return (
        <div className="flex h-screen bg-gray-50 dark:bg-gray-950">
            <Sidebar sections={sidebarElements} />

            <main className="flex-1 p-8 overflow-y-auto">
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">
                    Bienvenue sur Jolt
                </h1>
                <p className="text-gray-600 dark:text-gray-400">
                    Sélectionnez une option dans la barre latérale pour commencer.
                </p>
            </main>
        </div>
    );
}

export default LandingPage;