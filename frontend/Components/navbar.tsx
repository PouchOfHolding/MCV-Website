"use client";

import { useState, useRef, useEffect } from "react";
import Link from "next/link";
import styles from "./navbar.module.css";

export default function Navbar() {
  const [open, setOpen] = useState(false);
  const [momentosOpen, setMomentosOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setMomentosOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <header className={styles.navbar}>
      <Link href="/" className={styles.brand} aria-label="MCV UAlg - Página inicial">
        <img src="/images/MCV.png" alt="MCV UAlg" className={styles['brand-logo']} />
      </Link>

      <button
        className={styles['menu-toggle']}
        onClick={() => setOpen(!open)}
        aria-label="Abrir menu"
        aria-expanded={open}
      >
        <i /><i />
      </button>

      <nav className={open ? styles['nav-open'] : ""}>
        <Link href="/equipa" onClick={() => setOpen(false)}>MCV</Link>
        <Link href="/momentos-solenes" onClick={() => setOpen(false)}>Momentos Solenes</Link>
        <Link href="/Brasao" onClick={() => setOpen(false)}>Brasão</Link>
        <Link href="/Traje" onClick={() => setOpen(false)}>Traje Universitario</Link>
        <Link href="/Galeria" onClick={() => setOpen(false)}>Galeria</Link>
        <Link href="/documentacao" onClick={() => setOpen(false)}>Documentação</Link>
        
      </nav>
    </header>
  );
}