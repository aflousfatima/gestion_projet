"use client"; // Directive pour indiquer un composant client
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import "font-awesome/css/font-awesome.min.css";
import "bootstrap/dist/css/bootstrap.min.css"; // Importer Bootstrap CSS globalement
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { AuthProvider } from "../context/AuthContext";
import { usePathname } from "next/navigation"; // Importation de usePathname
const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const pathname = usePathname(); // Récupérer le pathname actuel

  // Liste des pages où la navbar et le footer ne doivent pas s'afficher
  const shouldExcludeNavbarFooter = pathname.startsWith("/user/dashboard");
  return (
    <html lang="en">
      <head>
        <link
          rel="stylesheet"
          href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.3/css/all.min.css"
        />
      </head>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        <AuthProvider>
          {!shouldExcludeNavbarFooter && <Navbar />}{" "}
          {/* Affiche Navbar uniquement si nécessaire */}
          <main>{children}</main>
          {!shouldExcludeNavbarFooter && <Footer />}{" "}
          {/* Affiche Footer uniquement si nécessaire */}
        </AuthProvider>
      </body>
    </html>
  );
}
