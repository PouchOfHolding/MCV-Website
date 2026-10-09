import Link from "next/link";
import styles from "./page.module.css";

export default function Home() {
  return (
    <main>
      <section className={styles.hero}>
        <img src="/images/MCV.png" alt="" className={styles['hero-watermark']} aria-hidden="true" />
        <div className={styles['hero-content']}>
          <p className={styles['hero-copy']}>"Momentos que passam, Saudade que fica".</p>
        </div>
      </section>

      <section className={styles['about-section']}>
        <div className={styles['about-square']}>
          <span className={styles['about-eyebrow']}>Descubra mais sobre o MCV</span>
          <h2>QUEM SOMOS</h2>

          <p className={styles['about-text']}>
            O MCV é o órgão da Praxe da Universidade do Algarve, responsável por preservar,
            organizar e promover as tradições académicas da instituição. Fundado em 2013, o
            MCV sucede ao antigo Grémio Académico, dando continuidade ao espírito académico
            e ao compromisso com a integração dos estudantes na vida universitária. Desde
            então, tem desempenhado um papel central na coordenação das atividades de praxe,
            procurando garantir que estas sejam vividas com respeito, responsabilidade e
            sentido de comunidade. Mais do que um órgão organizador, o MCV representa os
            valores da academia algarvia: tradição, companheirismo, entreajuda e orgulho
            em pertencer à Universidade do Algarve.
          </p>
        </div>
      </section>

      <section className={styles['closing-section']}>
        <p className={`${styles.eyebrow} ${styles.light}`}>Dura Praxis Sed Praxis</p>
        <h2>A tua vida academica<br />começa <em>aqui.</em></h2>
      </section>
    </main>
  );
}