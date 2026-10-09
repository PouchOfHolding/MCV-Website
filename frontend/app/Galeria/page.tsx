"use client";

import { useState, useRef } from "react";
import styles from "./galeria.module.css";

type Photo = {
  id: string;
  name: string;
  url: string;
};

export default function GaleriaPage() {
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [previewPhoto, setPreviewPhoto] = useState<Photo | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    const newPhotos: Photo[] = Array.from(files).map((file) => ({
      id: crypto.randomUUID(),
      name: file.name,
      url: URL.createObjectURL(file),
    }));

    setPhotos((prev) => [...prev, ...newPhotos]);
    e.target.value = "";
  };

  return (
    <main className={styles.page}>
      <h1 className={styles.title}>Galeria</h1>

      <div className={styles.grid}>
        <button
          className={styles.addCard}
          onClick={() => fileInputRef.current?.click()}
        >
          <span className={styles.plus}>+</span>
          <span>Adicionar fotos</span>
        </button>

        {photos.map((photo) => (
          <button
            key={photo.id}
            className={styles.photoCard}
            onClick={() => setPreviewPhoto(photo)}
          >
            <img src={photo.url} alt={photo.name} className={styles.thumbnail} />
          </button>
        ))}

        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          multiple
          onChange={handleFileSelect}
          className={styles.hiddenInput}
        />
      </div>

      {previewPhoto && (
        <div className={styles.overlay} onClick={() => setPreviewPhoto(null)}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <button
              className={styles.closeButton}
              onClick={() => setPreviewPhoto(null)}
              aria-label="Fechar"
            >
              ✕
            </button>
            <img
              src={previewPhoto.url}
              alt={previewPhoto.name}
              className={styles.previewImage}
            />
          </div>
        </div>
      )}
    </main>
  );
}