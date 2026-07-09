package com.crewschedule.crew.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** 크루 초대 코드 생성기. 혼동되기 쉬운 문자(0/O, 1/I/L)를 제외한 8자리 코드를 만든다. */
@Component
public class InviteCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
