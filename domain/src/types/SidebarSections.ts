// SidebarSections.ts
import { Database, FileSliders, LayoutTemplate, LucideIcon } from 'lucide-react';
import {
    LayoutDashboard,
    Folder,
    CheckSquare,
    Settings,
    BookOpen,
    Code,
    Server,
    Upload,
    Globe,
    FileText,
    Dock,
    Lock,
    HelpCircle,
    MessageCircleQuestion,
    Languages
} from 'lucide-react';

export interface SidebarSectionElement {
    label: string;
    icon: LucideIcon;
}

export interface SidebarGroup {
    group: string;
    items: SidebarSectionElement[];
}

export default interface SidebarSections {
    label: string;
    icon: LucideIcon;
}

// Define the sidebar elements with groups
export const sidebarElements: SidebarGroup[] = [
    {
        group: "GETTING STARTED",
        items: [
            { label: "Introduction", icon: BookOpen },
            { label: "Installation", icon: LayoutDashboard },
            { label: "Configuration", icon: Settings },
        ],
    },
    {
        group: "CORE CONCEPTS",
        items: [
            { label: "JoltApplication", icon: Code },
            { label: "Routes", icon: Folder },
            { label: "JoltContext", icon: Server },
            { label: "Dependency Injection", icon: CheckSquare },
        ],
    },
    {
        group: "FRAMEWORK COMPONENTS",
        items: [
            { label: "Filters", icon: Settings },
            { label: "Logging", icon: FileText },
            { label: "Cookies", icon: Code },
            { label: "Forms", icon: CheckSquare },
            { label: "File Uploads", icon: Upload },
            { label: "External API Requests", icon: Globe },
            { label: "Security", icon: Lock },
            { label: "Database", icon: Database },
            { label: "Localization", icon: Languages },
            { label: "Templating Engine", icon: LayoutTemplate },
            { label: "Configuration", icon: FileSliders },
        ],
    },
    {
        group: "DEPLOYMENT",
        items: [
            { label: "Docker X Jolt", icon: Dock },
            { label: "Running in Production", icon: Server },
        ],
    },
    {
        group: "API DOCUMENTATION",
        items: [
            { label: "Generated Javadocs API", icon: FileText },
        ],
    },
    {
        group: "FAQ",
        items: [
            { label: "Jolt-Related", icon: HelpCircle },
            { label: "General Web Developement", icon: MessageCircleQuestion },
        ],
    },
];