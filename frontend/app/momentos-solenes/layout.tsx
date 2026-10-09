"use client";

import { useState, useRef, useEffect } from "react";
import Sidebar from "../../Components/sidebar";
import styles from "./layout.module.css";

export default function MomentosSolenesLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div className={styles.layout}>
      <div ref={wrapperRef} className={styles.sidebarWrapper}>
        <button
          className={styles.toggle}
          onClick={() => setOpen(!open)}
          aria-label="Alternar menu"
          aria-expanded={open}
        >
          {open ? "✕" : "☰"}
        </button>
        <Sidebar open={open} onLinkClick={() => setOpen(false)} />
      </div>
      <div className={open ? styles.content : `${styles.content} ${styles.contentExpanded}`}>
        {children}
      </div>
    </div>
  );
}