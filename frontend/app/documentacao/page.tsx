"use client";

import { useState, useRef } from "react";
import styles from "./documentacao.module.css";

type Doc = {
  id: string;
  name: string;
  url: string;
};

export default function DocumentacaoPage() {
  const [docs, setDocs] = useState<Doc[]>([]);
  const [previewDoc, setPreviewDoc] = useState<Doc | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const url = URL.createObjectURL(file);
    const newDoc: Doc = {
      id: crypto.randomUUID(),
      name: file.name,
      url,
    };
    setDocs((prev) => [...prev, newDoc]);
    e.target.value = "";
  };


// the function on line 40 to 46, is what adds the square to add files 

  return (
    <main className={styles.page}>
      <h1 className={styles.title}>Documentação</h1>

      <div className={styles.grid}>
        
        <button
          className={styles.addCard}
          onClick={() => fileInputRef.current?.click()}
        >
          <span className={styles.plus}>+</span>
          <span>Adicionar documento</span>
        </button>

        {docs.map((doc) => (
          <button
            key={doc.id}
            className={styles.docCard}
            onClick={() => setPreviewDoc(doc)}
          >
            <span className={styles.docIcon}>📄</span>
            <span className={styles.docName}>{doc.name}</span>
          </button>
        ))}

        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.doc,.docx"
          onChange={handleFileSelect}
          className={styles.hiddenInput}
        />
      </div>

      {previewDoc && (
        <div className={styles.overlay} onClick={() => setPreviewDoc(null)}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <span>{previewDoc.name}</span>
              <button
                className={styles.closeButton}
                onClick={() => setPreviewDoc(null)}
                aria-label="Fechar"
              >
                ✕
              </button>
            </div>
            <iframe
              src={previewDoc.url}
              className={styles.iframe}
              title={previewDoc.name}
            />
          </div>
        </div>
      )}
    </main>
  );
}