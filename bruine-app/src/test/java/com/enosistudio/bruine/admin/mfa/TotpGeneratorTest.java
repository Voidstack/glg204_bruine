package com.enosistudio.bruine.admin.mfa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotpGeneratorTest {

    private final TotpGenerator generator = new TotpGenerator();

    @Test
    void matchesRfc6238ReferenceVector() {
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        assertEquals("287082", generator.codeAt(secret, 1L));
    }

    @Test
    void generatedSecretIs32Base32Chars() {
        String secret = generator.generateSecret();
        assertEquals(32, secret.length());
        assertTrue(secret.chars().allMatch(c -> "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(c) >= 0),
                "le secret doit être en alphabet Base32 RFC 4648");
    }

    @Test
    void verifyAcceptsCurrentCodeAndRejectsWrongOne() {
        String secret = generator.generateSecret();
        long currentStep = System.currentTimeMillis() / 1000L / 30L;
        String validCode = generator.codeAt(secret, currentStep);

        assertTrue(generator.verify(secret, validCode));
        assertFalse(generator.verify(secret, validCode.equals("000000") ? "111111" : "000000"));
    }

    @Test
    void verifyToleratesPreviousWindowButRejectsFarDrift() {
        String secret = generator.generateSecret();
        long currentStep = System.currentTimeMillis() / 1000L / 30L;

        assertTrue(generator.verify(secret, generator.codeAt(secret, currentStep - 1)), "±1 fenêtre toléré");
        assertFalse(generator.verify(secret, generator.codeAt(secret, currentStep - 5)), "dérive trop grande rejetée");
    }
}
