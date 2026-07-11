import type { Metadata } from "next";
import { Providers } from "@/lib/providers";
import { AppShell } from "@/components/AppShell";
import "./globals.css";

export const metadata: Metadata = {
  title: "Crew Schedule — 우리끼리 스케줄",
  description:
    "친구 그룹의 스케줄을 모아 다 같이 만날 수 있는 시간을 자동으로 찾아주는 그룹 스케줄링 플랫폼",
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
        </Providers>
      </body>
    </html>
  );
}
