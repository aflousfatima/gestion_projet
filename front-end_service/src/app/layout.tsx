"use client";

import { useEffect} from "react";
import { Inter, Geist_Mono } from "next/font/google";
import "./globals.css";
import "font-awesome/css/font-awesome.min.css";
import "bootstrap/dist/css/bootstrap.min.css";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { AuthProvider } from "../context/AuthContext";
import { usePathname } from "next/navigation";

const geistSans = Inter({
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
  const pathname = usePathname();
  // Liste des pages où la navbar et le footer ne doivent pas s'afficher
  const shouldExcludeNavbarFooter = pathname.startsWith("/user/dashboard");

  useEffect(() => {
    console.log("RootLayout rendu, pathname:", pathname);

    // Mesurer le temps de chargement au montage initial
    const startTime = performance.now();
    const loadTime = (performance.now() - startTime) / 1000; // En secondes
    console.log("Page montée, temps:", loadTime, "secondes, chemin:", pathname);
    fetch("/api/metrics", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        type: "page_load",
        page: pathname,
        value: loadTime,
      }),
    }).then((res) => console.log("Réponse page_load:", res.status));

    // Capturer les erreurs JavaScript globales
    const handleError = (event: ErrorEvent) => {
      console.log("Erreur JS détectée:", event.message, "sur:", pathname);
      fetch("/api/metrics", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          type: "js_error",
          page: pathname,
        }),
      }).then((res) => console.log("Réponse js_error:", res.status));
    };
    window.addEventListener("error", handleError);

    // Capturer les erreurs non gérées dans les promesses
    const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
      console.log(
        "Erreur de promesse non gérée:",
        event.reason,
        "sur:",
        pathname
      );
      fetch("/api/metrics", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          type: "js_error",
          page: pathname,
        }),
      }).then((res) => console.log("Réponse js_error:", res.status));
    };
    window.addEventListener("unhandledrejection", handleUnhandledRejection);

    // Capturer les interactions utilisateur (clics sur boutons)
    const handleClick = (event: MouseEvent) => {
      if (
        event.target instanceof HTMLElement &&
        event.target.tagName === "BUTTON"
      ) {
        console.log("Clic sur bouton détecté sur:", pathname);
        fetch("/api/metrics", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            type: "user_interaction",
            page: pathname,
            action: "button_click",
          }),
        }).then((res) => console.log("Réponse user_interaction:", res.status));
      }
    };
    document.addEventListener("click", handleClick);

    // Nettoyage des écouteurs d'événements
    return () => {
      window.removeEventListener("error", handleError);
      window.removeEventListener(
        "unhandledrejection",
        handleUnhandledRejection
      );
      document.removeEventListener("click", handleClick);
    };
  }, [pathname]); // Dépendance sur pathname pour réexécuter à chaque changement de page

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
          {!shouldExcludeNavbarFooter && <Navbar />}
          <main>{children}</main>
          {!shouldExcludeNavbarFooter && <Footer />}
        </AuthProvider>
      </body>
    </html>
  );
}
