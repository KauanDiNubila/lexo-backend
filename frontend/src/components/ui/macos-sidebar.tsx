import { SidebarLeftIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import { motion, AnimatePresence } from "motion/react";
import { useState, type ReactNode } from "react";
import { cn } from "@/lib/utils";

export interface SidebarNavItem {
  label: string;
  icon: IconSvgElement;
  to: string;
}

export interface MacOSSidebarProps {
  items: SidebarNavItem[];
  activeIndex: number;
  onSelect: (item: SidebarNavItem, index: number) => void;
  defaultOpen?: boolean;
  header?: (isOpen: boolean) => ReactNode;
  footer?: (isOpen: boolean) => ReactNode;
  children?: ReactNode;
  className?: string;
}

export function MacOSSidebar({
  items,
  activeIndex,
  onSelect,
  defaultOpen = true,
  header,
  footer,
  children,
  className = "",
}: MacOSSidebarProps) {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const [isOpen, setIsOpen] = useState<boolean>(defaultOpen);

  return (
    <div className={cn("flex h-screen w-full bg-background", className)}>
      <motion.div
        animate={{ width: isOpen ? 240 : 68 }}
        transition={{ type: "spring", bounce: 0.3, duration: 0.6 }}
        className="flex shrink-0 flex-col overflow-hidden border-r border-border bg-card p-3"
      >
        <div className={cn("flex items-center gap-2 p-2", isOpen ? "justify-between" : "justify-center")}>
          {header && <div className="min-w-0">{header(isOpen)}</div>}
          <button
            onClick={() => setIsOpen(!isOpen)}
            className="grid size-7 shrink-0 place-items-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground"
          >
            <HugeiconsIcon icon={SidebarLeftIcon} className="size-[18px]" />
          </button>
        </div>

        <AnimatePresence mode="wait">
          <motion.nav
            key={isOpen ? "open" : "closed"}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.15 }}
            className="mt-3 flex flex-1 flex-col gap-1"
            onMouseLeave={() => setHoveredIndex(null)}
          >
            {items.map((item, index) => {
              const active = activeIndex === index;
              return (
                <button
                  key={item.to}
                  onClick={() => onSelect(item, index)}
                  onMouseEnter={() => setHoveredIndex(index)}
                  className={cn(
                    "relative flex items-center gap-3 rounded-md px-2.5 py-2 text-left",
                    !isOpen && "justify-center px-0"
                  )}
                >
                  {active && (
                    <motion.div
                      layoutId="sidebar-active-bg"
                      className="absolute inset-0 z-0 rounded-md bg-primary/10"
                      transition={{ duration: 0.2, ease: "easeOut" }}
                    />
                  )}
                  {hoveredIndex === index && !active && (
                    <motion.div
                      layoutId="sidebar-hover-bg"
                      className="absolute inset-0 z-0 rounded-md bg-muted"
                      transition={{ type: "spring", stiffness: 350, damping: 30 }}
                    />
                  )}
                  <HugeiconsIcon
                    icon={item.icon}
                    className={cn(
                      "relative z-10 size-[18px] shrink-0",
                      active ? "text-primary" : "text-muted-foreground"
                    )}
                  />
                  {isOpen && (
                    <span
                      className={cn(
                        "relative z-10 truncate text-sm tracking-tight",
                        active ? "font-semibold text-foreground" : "text-muted-foreground"
                      )}
                    >
                      {item.label}
                    </span>
                  )}
                </button>
              );
            })}
          </motion.nav>
        </AnimatePresence>

        {footer && <div className="mt-auto pt-2">{footer(isOpen)}</div>}
      </motion.div>

      <div className="min-w-0 flex-1 overflow-y-auto">{children}</div>
    </div>
  );
}
