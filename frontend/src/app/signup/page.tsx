"use client";

import Link from "next/link";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { useSignup } from "@/lib/auth-hooks";

export default function SignupPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const signup = useSignup();
  const errorMessage =
    signup.error instanceof ApiError
      ? signup.error.message
      : signup.error
        ? "회원가입에 실패했어요."
        : null;

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    signup.mutate({ email, password, nickname });
  };

  return (
    <div className="mx-auto max-w-sm py-10">
      <h1 className="text-2xl font-bold tracking-tight">회원가입</h1>
      <p className="mt-1 text-sm text-muted">
        크루의 스케줄을 한눈에 모아보세요.
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
          <span className="text-xs font-medium text-muted">닉네임</span>
          <input
            type="text"
            required
            minLength={2}
            maxLength={50}
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            className="rounded-lg border bg-surface px-3 py-2.5 text-sm focus:border-primary focus:outline-none"
            placeholder="크루원들에게 보일 이름"
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
          disabled={signup.isPending}
          className="mt-2 rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground shadow-sm transition-transform hover:scale-[1.01] active:scale-95 disabled:opacity-50 disabled:hover:scale-100"
        >
          {signup.isPending ? "가입 중..." : "가입하기"}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-muted">
        이미 계정이 있나요?{" "}
        <Link href="/login" className="font-semibold text-primary hover:underline">
          로그인
        </Link>
      </p>
    </div>
  );
}
