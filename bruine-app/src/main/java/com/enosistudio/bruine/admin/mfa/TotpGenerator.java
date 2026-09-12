package com.enosistudio.bruine.admin.mfa;

import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Implémentation TOTP (RFC 6238) au-dessus du HMAC-SHA1 fourni par le JDK.
 *
 * <p>Aucune dépendance TOTP tierce : le HMAC vient de {@link javax.crypto.Mac} (implémentation JDK
 * éprouvée), on ne fait qu'appliquer l'algorithme public de la RFC 6238 / RFC 4226 (HOTP).
 * Le secret est encodé en Base32 (via Apache Commons Codec) car c'est le format attendu par les
 * applications d'authentification (Google Authenticator, Authy, etc.).
 */
public final class TotpGenerator {

    private static final int SECRET_BYTES = 20;        // 160 bits taille recommandée pour HMAC-SHA1
    private static final int DIGITS = 6;               // longueur du code affiché
    private static final int PERIOD_SECONDS = 30;      // durée de validité d'un code
    private static final int ALLOWED_DRIFT_STEPS = 1;  // tolère ±1 fenêtre pour la dérive d'horloge
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    private final SecureRandom random = new SecureRandom();
    private final Base32 base32 = new Base32();

    /**
     * Génère un secret aléatoire encodé en Base32 (sans padding), à faire scanner par l'utilisateur.
     */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        random.nextBytes(bytes);
        return base32.encodeAsString(bytes).replace("=", "");
    }

    /**
     * Vérifie un code sur la fenêtre courante ± la dérive d'horloge tolérée.
     */
    public boolean verify(String base32Secret, String code) {
        if (base32Secret == null || code == null) {
            return false;
        }
        String cleaned = code.trim();
        if (cleaned.length() != DIGITS || !cleaned.chars().allMatch(Character::isDigit)) {
            return false;
        }

        byte[] key = base32.decode(base32Secret);
        long currentStep = System.currentTimeMillis() / 1000L / PERIOD_SECONDS;
        for (long i = -ALLOWED_DRIFT_STEPS; i <= ALLOWED_DRIFT_STEPS; i++) {
            if (constantTimeEquals(generateCode(key, currentStep + i), cleaned)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Code TOTP pour une fenêtre donnée visible pour les tests (vecteurs RFC 6238).
     */
    String codeAt(String base32Secret, long step) {
        return generateCode(base32.decode(base32Secret), step);
    }

    /**
     * Calcule le code HOTP (RFC 4226) pour une fenêtre temporelle donnée.
     */
    private String generateCode(byte[] key, long step) {
        byte[] data = ByteBuffer.allocate(Long.BYTES).putLong(step).array();
        byte[] hash;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            hash = mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Calcul HMAC TOTP impossible", e);
        }
        // Troncature dynamique (RFC 4226 §5.3)
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
        int otp = binary % (int) Math.pow(10, DIGITS);
        return String.format("%0" + DIGITS + "d", otp);
    }

    /**
     * Comparaison à temps constant pour ne pas fuiter d'information via le temps de réponse.
     */
    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.US_ASCII),
                b.getBytes(StandardCharsets.US_ASCII));
    }
}
