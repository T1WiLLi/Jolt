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
    link: string;
    icon: LucideIcon;
}

export interface SidebarGroup {
    group: string;
    items: SidebarSectionElement[];
}

export default interface SidebarSections {
    label: string;
    link: string;
    icon: LucideIcon;
}

export const sidebarElements: SidebarGroup[] = [
    {
        group: "GETTING STARTED",
        items: [
            { label: "Introduction", link: "introduction", icon: BookOpen },
            { label: "Installation", link: "installation", icon: LayoutDashboard },
            { label: "Configuration", link: "configuration", icon: Settings },
        ],
    },
    {
        group: "CORE CONCEPTS",
        items: [
            { label: "JoltApplication", link: "jolt", icon: Code },
            { label: "Routes", link: "routes", icon: Folder },
            { label: "JoltContext", link: "joltcontext", icon: Server },
            { label: "Dependency Injection", link: "Ioc", icon: CheckSquare },
        ],
    },
    {
        group: "FRAMEWORK COMPONENTS",
        items: [
            { label: "Filters", link: "filters", icon: Settings },
            { label: "Logging", link: "logging", icon: FileText },
            { label: "Cookies", link: "cookies", icon: Code },
            { label: "Forms", link: "forms", icon: CheckSquare },
            { label: "File Uploads", link: "file-uploads", icon: Upload },
            { label: "External API Requests", link: "external-api-requests", icon: Globe },
            { label: "Database", link: "database", icon: Database },
            { label: "Templating Engine", link: "templating-engine", icon: LayoutTemplate },
            { label: "Localization", link: "localization", icon: Languages },
            { label: "Security", link: "security", icon: Lock },
            { label: "Configuration", link: "configuration", icon: FileSliders },
        ],
    },
    {
        group: "DEPLOYMENT",
        items: [
            { label: "Docker X Jolt", link: "docker-x-jolt", icon: Dock },
            { label: "Running in Production", link: "running-in-production", icon: Server },
        ],
    },
    {
        group: "API DOCUMENTATION",
        items: [
            { label: "Generated Javadocs API", link: "generated-javadocs-api", icon: FileText },
        ],
    },
    {
        group: "FAQ",
        items: [
            { label: "Jolt-Related", link: "jolt-related", icon: HelpCircle },
            { label: "General Web Development", link: "general-web-development", icon: MessageCircleQuestion },
        ],
    },
];
