package com.enosistudio.bruine.steam.service;

import com.enosistudio.bruine.steam.dto.SteamOpenidLoginDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SteamServiceTest {

    private static final String BASE_URL = "https://bruine.example";
    private static final String RETURN_TO = BASE_URL + "/steam/login/redirect";
    private static final String ENDPOINT = "https://steamcommunity.com/openid/login";
    private static final String IDENTITY = "https://steamcommunity.com/openid/id/76561198100881386";
    private static final String SIGNED = "signed,op_endpoint,claimed_id,identity,return_to,response_nonce,assoc_handle";

    private final SteamService steamService = new SteamService(new RestTemplate(), new ObjectMapper(),
            Validation.buildDefaultValidatorFactory().getValidator(), "cle-factice");

    @Test
    void anIncompleteAssertionIsRejectedBeforeContactingSteam() {
        SteamOpenidLoginDTO dto = new SteamOpenidLoginDTO("http://specs.openid.net/auth/2.0", ENDPOINT, IDENTITY,
                IDENTITY, RETURN_TO, "2026-09-13T10:00:00Zabc", "1234567890", SIGNED, "");

        assertThrows(ConstraintViolationException.class, () -> steamService.validateLoginParameters(dto, BASE_URL));
    }

    @Test
    void anAssertionMeantForAnotherSiteIsRejected() {
        SteamOpenidLoginDTO dto = assertion(ENDPOINT, IDENTITY, IDENTITY,
                "https://site-hostile.example/steam/login/redirect", SIGNED);

        assertThrows(IllegalArgumentException.class, () -> steamService.validateLoginParameters(dto, BASE_URL));
    }

    @Test
    void anAssertionFromAnotherProviderIsRejected() {
        SteamOpenidLoginDTO dto = assertion("https://faux-steam.example/openid/login", IDENTITY, IDENTITY,
                RETURN_TO, SIGNED);

        assertThrows(IllegalArgumentException.class, () -> steamService.validateLoginParameters(dto, BASE_URL));
    }

    @Test
    void aClaimedIdDifferentFromTheIdentityIsRejected() {
        SteamOpenidLoginDTO dto = assertion(ENDPOINT, IDENTITY,
                "https://steamcommunity.com/openid/id/76561198000000000", RETURN_TO, SIGNED);

        assertThrows(IllegalArgumentException.class, () -> steamService.validateLoginParameters(dto, BASE_URL));
    }

    @Test
    void anIdentityNotCoveredByTheSignatureIsRejected() {
        SteamOpenidLoginDTO dto = assertion(ENDPOINT, IDENTITY, IDENTITY,
                RETURN_TO, "signed,op_endpoint,return_to,response_nonce,assoc_handle");

        assertThrows(IllegalArgumentException.class, () -> steamService.validateLoginParameters(dto, BASE_URL));
    }

    private static SteamOpenidLoginDTO assertion(String opEndpoint, String claimedId, String identity,
                                                 String returnTo, String signed) {
        return new SteamOpenidLoginDTO("http://specs.openid.net/auth/2.0", opEndpoint, claimedId, identity,
                returnTo, "2026-09-13T10:00:00Zabc", "1234567890", signed, "c2lnbmF0dXJl");
    }
}
