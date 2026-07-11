"use client";

import Link from "next/link";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { useLogin } from "@/lib/auth-hooks";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const login = useLogin();
  const errorMessage =
    login.error instanceof ApiError
      ? login.error.message
      : login.error
        ? "로그인에 실패했어요."
        : null;

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    login.mutate({ email, password });
  };

  return (
    <div className="mx-auto max-w-sm py-10">
      <h1 className="text-2xl font-bold tracking-tight">로그인</h1>
      <p className="mt-1 text-sm text-muted">
        Crew Schedule에 다시 오신 것을 환영해요.
      </p>

      <form className="mt-8 flex flex-col gap-4" onSubmit={submit}>
        <label className="flex flex-col gap-1.5">
          <span className="text-xs font-medium text-muted">이메일</span>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="rounded-lg border bg-surface px-3 py-2.5 text-sm focus:border-primary focus:outline-none"
            placeholder="you@example.com"
          />
        </label>
        <label className="flex flex-col gap-1.5">
          <span className="text-xs font-medium text-muted">비밀번호</span>
          <input
            type="password"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="rounded-lg border bg-surface px-3 py-2.5 text-sm focus:border-primary focus:outline-none"
            placeholder="8자 이상"
          />
        </label>

        {errorMessage && (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700">
            {errorMessage}
          </p>
        )}

        <button
          type="submit"
          disabled={login.isPending}
          className="mt-2 rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-sm transition-transform hover:scale-[1.01] active:scale-95 disabled:opacity-50 disabled:hover:scale-100"
        >
          {login.isPending ? "로그인 중..." : "로그인"}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-muted">
        아직 계정이 없나요?{" "}
        <Link href="/signup" className="font-semibold text-primary hover:underline">
          회원가입
        </Link>
      </p>
    </div>
  );
}
