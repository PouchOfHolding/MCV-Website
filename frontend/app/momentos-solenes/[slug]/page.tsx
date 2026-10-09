import { momentosSolenes } from "@/lib/momentos-solenes";
import { notFound } from "next/navigation";
import styles from "./momento.module.css";

export default async function MomentoPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const momento = momentosSolenes.find((m) => m.slug === slug);

  if (!momento) notFound();

  return (
    <main className={styles.page}>
      <h1>{momento.name}</h1>
      {momento.image && (
        <div className={styles.wobbleWrapper}>
          <img src={momento.image} alt={momento.name} className={styles.wobbleImage} />
        </div>
      )}
      <p>{momento.description}</p>
    </main>
  );
}