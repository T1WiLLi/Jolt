import { useState } from "react";
import { SidebarGroup } from "../types/SidebarSections";
import { Search, ChevronLeft, ChevronRight } from "lucide-react";
import * as TooltipPrimitive from "@radix-ui/react-tooltip";

function Sidebar({ sections }: { sections: SidebarGroup[] }) {
    const [isCollapsed, setIsCollapsed] = useState(false);
    const [searchQuery, setSearchQuery] = useState("");

    const filteredSections = sections
        .map((group) => ({
            ...group,
            items: group.items.filter((item) =>
                item.label.toLowerCase().includes(searchQuery.toLowerCase())
            ),
        }))
        .filter((group) => group.items.length > 0);

    return (
        <TooltipPrimitive.Provider>
            <div
                className={`flex flex-col border-r border-gray-800 h-screen bg-gray-900 text-white transition-all duration-300 shadow-lg 
                ${isCollapsed ? "w-16" : "w-64"}`}
            >
                <div className="p-4 flex items-center justify-between border-b border-gray-800">
                    {!isCollapsed && (
                        <div className="flex flex-col">
                            <span className="text-2xl font-bold text-white-400">Jolt</span>
                            <span className="text-xs text-gray-500">documentation V2.5.6</span>
                        </div>
                    )}
                    <button
                        onClick={() => setIsCollapsed(!isCollapsed)}
                        className="p-1 cursor-pointer rounded hover:bg-gray-800 transition"
                    >
                        {isCollapsed ? (
                            <ChevronRight className="text-gray-400" />
                        ) : (
                            <ChevronLeft className="text-gray-400" />
                        )}
                    </button>
                </div>

                {!isCollapsed && (
                    <div className="px-3 py-2 border-b border-gray-800">
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-500" />
                            <input
                                type="text"
                                placeholder="Search..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                className="w-full pl-10 pr-3 py-2 bg-gray-800 text-white rounded-md 
                                border border-gray-700 focus:outline-none focus:ring-2 
                                focus:ring-blue-500 focus:border-transparent placeholder-gray-500"
                            />
                        </div>
                    </div>
                )}

                <div className="flex-1 overflow-y-auto pt-2">
                    {filteredSections.map((group, groupIndex) => (
                        <div key={groupIndex} className="mb-4">
                            {!isCollapsed && (
                                <div className="px-4 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide truncate w-full">
                                    {group.group.length > 20 ? `${group.group.substring(0, 20)}...` : group.group}
                                </div>
                            )}
                            {group.items.map((item, itemIndex) => (
                                isCollapsed ? (
                                    <TooltipPrimitive.Root key={itemIndex}>
                                        <TooltipPrimitive.Trigger asChild>
                                            <a
                                                href={`#${item.link}`}
                                                className={`flex items-center px-4 py-2 mx-2 rounded-md cursor-pointer transition-all 
                                                hover:bg-gray-700 ${itemIndex !== group.items.length - 1 ? "mb-1" : ""}`}
                                            >
                                                <item.icon className="w-5 h-5 text-blue-400" />
                                            </a>
                                        </TooltipPrimitive.Trigger>
                                        <TooltipPrimitive.Content
                                            side="right"
                                            className="bg-gray-800 text-white px-3 py-1 rounded text-sm"
                                        >
                                            {item.label}
                                            <TooltipPrimitive.Arrow className="fill-gray-800" />
                                        </TooltipPrimitive.Content>
                                    </TooltipPrimitive.Root>
                                ) : (
                                    <a
                                        key={itemIndex}
                                        href={`#${item.link}`}
                                        className={`flex items-center px-4 py-2 mx-2 rounded-md cursor-pointer transition-all 
                                        hover:bg-gray-700 ${itemIndex !== group.items.length - 1 ? "mb-1" : ""}`}
                                    >
                                        <item.icon className="w-5 h-5 text-blue-400" />
                                        <span className="text-gray-300 ml-3 truncate w-full">{item.label}</span>
                                    </a>
                                )
                            ))}
                        </div>
                    ))}
                </div>

                <div className="p-4 border-t border-gray-800">
                    <button
                        className={`cursor-pointer w-full py-2 rounded bg-blue-500 hover:bg-blue-600 text-white font-medium 
                        transition-all ${isCollapsed ? "text-xs" : ""}`}
                    >
                        {isCollapsed ? "T" : "Template"}
                    </button>
                </div>
            </div>
        </TooltipPrimitive.Provider>
    );
}

export default Sidebar;
