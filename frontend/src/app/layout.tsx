import type { Metadata, Viewport } from "next";
import { Providers } from "@/lib/providers";
import { AppShell } from "@/components/AppShell";
import { PwaBootstrap } from "@/components/PwaBootstrap";
import "./globals.css";

export const metadata: Metadata = {
  title: "Crew Schedule — 우리끼리 스케줄",
  description:
    "친구 그룹의 스케줄을 모아 다 같이 만날 수 있는 시간을 자동으로 찾아주는 그룹 스케줄링 플랫폼",
  applicationName: "Crew Schedule",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "Crew Schedule",
  },
  icons: {
    icon: "/icon.svg",
    apple: "/icon.svg",
  },
};

export const viewport: Viewport = {
  themeColor: "#2563eb",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className="h-full antialiased">
      {/* Pretendard (React 19가 <head>로 hoist) */}
      <link
        rel="stylesheet"
        href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css"
      />
      <body className="min-h-full flex flex-col">
        <Providers>
          <AppShell>{children}</AppShell>
          <PwaBootstrap />
        </Providers>
      </body>
    </html>
  );
}
