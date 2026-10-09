import type { Metadata } from "next";
import "./globals.css";
import Navbar from "../Components/navbar";

export const metadata: Metadata = {
  title: "MCV | Universidade do Algarve",
  description: "A comunidade académica da Universidade do Algarve.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="pt-PT">
      <body className="min-h-full flex flex-col">
        <Navbar />
        {children}
      </body>
    </html>
  );
}