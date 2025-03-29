// landing-page.tsx
import Sidebar from "./Sidebar";
import { sidebarElements } from "../types/SidebarSections";
import GettingStarted from "../group/GettingStarted";

function LandingPage() {
    return (
        <div className="flex h-screen bg-gray-50 dark:bg-gray-950">
            <Sidebar sections={sidebarElements} />

            <main className="flex-1 p-8 overflow-y-auto">
                <GettingStarted />
            </main>
        </div>
    );
}

export default LandingPage;