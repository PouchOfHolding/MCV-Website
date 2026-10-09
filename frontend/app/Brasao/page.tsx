"use client";

import { useState } from "react";
import styles from "./brasao.module.css";

const items = [
  { label: "Ceptro", text: "Descrição do Ceptro em breve.", image: "/images/ceptro.png" },
  { label: "Moca", text: "Descrição da Moça em breve.", image: "/images/moca.png" },
  { label: "Tesoura", text: "Descrição do Tesouro em breve.", image: "/images/tesoura.png" },
  { label: "Colher", text: "Descrição da Colher em breve.", image: "/images/colher.png" },
];

const DEFAULT_TITLE = "O significado do brasão";
const DEFAULT_TEXT = "Cada elemento do brasão carrega uma tradição própria, transmitida ao longo de gerações de estudantes da UAlg.";

export default function BrasaoPage() {
  const [selected, setSelected] = useState<string | null>(null);

  const toggle = (label: string) => {
    setSelected((prev) => (prev === label ? null : label));
  };

  const activeItem = items.find((item) => item.label === selected);

  return (
    <main className={styles.page}>
      <section className={styles['photo-section']} />

      <div className={styles.gallery}>
        {items.map((item) => (
          <div key={item.label} className={styles.cardWrapper}>
            <div
              className={`${styles.card} ${selected === item.label ? styles.flipped : ""}`}
              onClick={() => toggle(item.label)}
            >
              <div className={`${styles.face} ${styles.front}`}>
                <span className={styles.label}>{item.label}</span>
              </div>
              <div className={`${styles.face} ${styles.back}`}>
                <img src={item.image} alt={item.label} className={styles.frontImage} />
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className={`${styles.scrollSection} ${styles.visible}`}>
        <h2>{activeItem ? activeItem.label : DEFAULT_TITLE}</h2>
        <p>{activeItem ? activeItem.text : DEFAULT_TEXT}</p>
      </div>
    </main>
  );
}