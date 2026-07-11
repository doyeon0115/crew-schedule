import type { NextConfig } from "next";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      // 프론트에서는 same-origin /api/*를 호출하고, Next가 백엔드로 프록시.
      // 개발/프로덕션 모두 CORS 회피.
      { source: "/api/:path*", destination: `${BACKEND_URL}/api/:path*` },
    ];
  },
};

export default nextConfig;
