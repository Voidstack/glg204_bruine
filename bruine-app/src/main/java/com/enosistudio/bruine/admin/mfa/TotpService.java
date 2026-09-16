package com.enosistudio.bruine.admin.mfa;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Service MFA : génération/vérification de codes TOTP (RFC 6238) et du QR code d'enrôlement.
 * Le cœur TOTP est implémenté maison ({@link TotpGenerator}, HMAC-SHA1 du JDK). ZXing (Google)
 * ne sert qu'à transformer l'URI {@code otpauth://} en image PNG.
 */
@Service
public class TotpService {

    /**
     * Nom affiché dans l'appli d'authentification à côté du compte.
     */
    private static final String ISSUER = "Bruine";
    private static final int QR_SIZE = 220;

    private final TotpGenerator totpGenerator = new TotpGenerator();

    /**
     * Génère un nouveau secret base32 à faire scanner par l'utilisateur.
     */
    public String generateSecret() {
        return totpGenerator.generateSecret();
    }

    /**
     * Vérifie un code à 6 chiffres (tolère une légère dérive d'horloge).
     */
    public boolean verify(String secret, String code) {
        return totpGenerator.verify(secret, code);
    }

    /**
     * Construit une image (data-URI PNG) du QR code {@code otpauth://} à afficher pour l'enrôlement.
     *
     * @param accountName identifiant affiché dans l'appli (ici le nom de l'admin)
     * @param secret      secret TOTP généré pour ce compte
     */
    public String qrCodeDataUri(String accountName, String secret) {
        byte[] png = renderQrPng(buildOtpAuthUri(accountName, secret));
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
    }

    /**
     * URI standard {@code otpauth://totp/...} reconnue par toutes les applis d'authentification.
     */
    private String buildOtpAuthUri(String accountName, String secret) {
        String label = urlEncode(accountName);
        String issuer = urlEncode(ISSUER);
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + issuer + "&algorithm=SHA1&digits=6&period=30";
    }

    private String urlEncode(String value) {
        // URLEncoder encode l'espace en '+', or l'URI otpauth attend '%20'
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private byte[] renderQrPng(String content) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1, EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Impossible de générer le QR code MFA", e);
        }
    }
}
