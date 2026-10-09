"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { momentosSolenes } from "@/lib/momentos-solenes";
import styles from "./sidebar.module.css";

export default function Sidebar({
  open,
  onLinkClick,
}: {
  open: boolean;
  onLinkClick: () => void;
}) {
  const pathname = usePathname();

  return (
    <aside className={open ? `${styles.sidebar} ${styles.open}` : styles.sidebar}>
      <nav>
        {momentosSolenes.map((momento) => {
          const href = `/momentos-solenes/${momento.slug}`;
          const active = pathname === href;
          return (
            <Link
              key={momento.slug}
              href={href}
              onClick={onLinkClick}
              className={active ? `${styles.link} ${styles.active}` : styles.link}
            >
              {momento.name}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}