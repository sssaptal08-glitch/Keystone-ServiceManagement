package com.KEYSTONE.ServiceManagement.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Year;

@Component
public class WorkOrderCodeGenerator {

    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    /** Generates a human-friendly work order code, e.g. WO-2026-7F3K9. */
    public String generate() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(ALPHANUM.charAt(random.nextInt(ALPHANUM.length())));
        }
        return "WO-" + Year.now().getValue() + "-" + sb;
    }
}
