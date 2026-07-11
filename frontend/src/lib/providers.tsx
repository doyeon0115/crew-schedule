"use client";

import { AuthGuard } from "./auth-guard";
import { QueryProvider } from "./query-provider";

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <QueryProvider>
      <AuthGuard>{children}</AuthGuard>
    </QueryProvider>
  );
}
