// landing-page.tsx
import Sidebar from "./Sidebar";
import { sidebarElements } from "../types/SidebarSections";
import GettingStarted from "../group/GettingStarted";
import CoreConcepts from "../group/CoreConcepts";
import UnderConstruction from "../helper/StillBuilding";
import FrameworkComponent from "../group/FrameworkComponent";

function LandingPage() {
    return (
        <div className="flex h-screen bg-gray-50 dark:bg-gray-950">
            <Sidebar sections={sidebarElements} />

            <main className="flex p-8 overflow-y-auto flex-col gap-5">
                <GettingStarted />
                <CoreConcepts />
                <FrameworkComponent />
                <UnderConstruction />
            </main>
        </div>
    );
}

export default LandingPage;