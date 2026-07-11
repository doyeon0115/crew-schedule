import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Crew Schedule — 우리끼리 스케줄",
    short_name: "Crew Schedule",
    description:
      "친구·동료의 근무/휴무 스케줄을 한눈에 모아, 다 같이 만날 시간을 자동으로 찾아주는 그룹 스케줄링 앱",
    start_url: "/",
    display: "standalone",
    background_color: "#ffffff",
    theme_color: "#2563eb",
    orientation: "portrait",
    lang: "ko",
    icons: [
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "any" },
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "maskable" },
    ],
  };
}
