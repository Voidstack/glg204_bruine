package com.enosistudio.bruine.steam.service;

import com.enosistudio.bruine.steam.dto.SteamGameDTO;
import com.enosistudio.bruine.steam.dto.SteamOpenidLoginDTO;
import com.enosistudio.bruine.steam.dto.SteamPlayerDTO;
import com.enosistudio.bruine.steam.exception.SteamException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SteamServiceTest {

    private static final String BASE_URL = "https://bruine.example";
    private static final String RETURN_TO = BASE_URL + "/steam/login/redirect";
    private static final String ENDPOINT = "https://steamcommunity.com/openid/login";
    private static final String IDENTITY = "https://steamcommunity.com/openid/id/76561198100881386";
    private static final String SIGNED = "signed,op_endpoint,claimed_id,identity,return_to,response_nonce,assoc_handle";

    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer steam = MockRestServiceServer.bindTo(restTemplate).build();
    private final SteamService steamService = new SteamService(restTemplate,
            Validation.buildDefaultValidatorFactory().getValidator(), "cle-factice");

    @Test
    void aPlayerSummaryIsReadIntoTheTypedProfile() {
        steam.expect(requestTo(containsString("GetPlayerSummaries"))).andRespond(withSuccess("""
                {"response": {"players": [{
                    "steamid": "76561198100881386", "personaname": "Joueur", "commentpermission": 1,
                    "avatar": "petit.jpg", "avatarmedium": "moyen.jpg", "avatarfull": "grand.jpg",
                    "personastate": 1, "communityvisibilitystate": 3, "loccountrycode": "FR",
                    "profileurl": "https://steamcommunity.com/id/joueur/"
                }]}}""", MediaType.APPLICATION_JSON));

        SteamPlayerDTO player = steamService.getPlayer("76561198100881386");

        assertEquals("Joueur", player.personaName());
        assertEquals("moyen.jpg", player.avatarMedium());
        assertEquals("grand.jpg", player.avatarFull());
        assertEquals(1, player.personaState());
        assertEquals(3, player.communityVisibilityState());
        assertEquals("FR", player.countryCode());
        steam.verify();
    }

    @Test
    void onlyGamesPlayedAnHourOrMoreAreKeptMostPlayedFirst() {
        steam.expect(requestTo(containsString("GetOwnedGames"))).andRespond(withSuccess("""
                {"response": {"game_count": 3, "games": [
                    {"appid": 1, "name": "Survolé", "playtime_forever": 59},
                    {"appid": 2, "name": "Une heure", "playtime_forever": 60},
                    {"appid": 3, "name": "Favori", "playtime_forever": 600, "img_icon_url": "abc", "rtime_last_played": 0}
                ]}}""", MediaType.APPLICATION_JSON));

        List<SteamGameDTO> played = steamService.getPlayedGames("76561198100881386");

        assertEquals(List.of("Favori", "Une heure"), played.stream().map(SteamGameDTO::name).toList());
        assertEquals(new SteamGameDTO(3, "Favori", 600, "abc"), played.getFirst());
        steam.verify();
    }

    @Test
    void aPrivateGameListIsReportedAsAFailure() {
        steam.expect(requestTo(containsString("GetOwnedGames")))
                .andRespond(withSuccess("{\"response\": {}}", MediaType.APPLICATION_JSON));

        assertThrows(SteamException.class, () -> steamService.getPlayedGames("76561198100881386"));
        steam.verify();
    }

    @Test
    void anIncompleteAssertionIsRejectedBeforeContactingSteam() {
        SteamOpenidLoginDTO dto = new SteamOpenidLoginDTO("http://specs.openid.net/auth/2.0", ENDPOINT, IDENTITY,
                IDENTITY, RETURN_TO, "2026-09-13T10:00:00Zabc", "1234567890", SIGNED, "");

        assertThrows(BadCredentialsException.class, () -> steamService.validateLoginParameters(dto, BASE_URL));
    }

    @Test
    void anAssertionMeantForAnotherSiteIsRejected() {
        SteamOpenidLoginDTO dto = assertion(ENDPOINT, IDENTITY, IDENTITY,
                "https://site-hostile.example/steam/login/redirect", SIGNED);

        assertThrows(BadCredentialsException.class, () -> steamService.validateLoginParameters(dto, BASE_URL));
    }

    @Test
    void anAssertionFromAnotherProviderIsRejected() {
        SteamOpenidLoginDTO dto = assertion("https://faux-steam.example/openid/login", IDENTITY, IDENTITY,
                RETURN_TO, SIGNED);

        assertThrows(BadCredentialsException.class, () -> steamService.validateLoginParameters(dto, BASE_URL));
    }

    @Test
    void aClaimedIdDifferentFromTheIdentityIsRejected() {
        SteamOpenidLoginDTO dto = assertion(ENDPOINT, IDENTITY,
                "https://steamcommunity.com/openid/id/76561198000000000", RETURN_TO, SIGNED);

        assertThrows(BadCredentialsException.class, () -> steamService.validateLoginParameters(dto, BASE_URL));
    }

    @Test
    void anIdentityNotCoveredByTheSignatureIsRejected() {
        SteamOpenidLoginDTO dto = assertion(ENDPOINT, IDENTITY, IDENTITY,
                RETURN_TO, "signed,op_endpoint,return_to,response_nonce,assoc_handle");

        assertThrows(BadCredentialsException.class, () -> steamService.validateLoginParameters(dto, BASE_URL));
    }

    private static SteamOpenidLoginDTO assertion(String opEndpoint, String claimedId, String identity,
                                                 String returnTo, String signed) {
        return new SteamOpenidLoginDTO("http://specs.openid.net/auth/2.0", opEndpoint, claimedId, identity,
                returnTo, "2026-09-13T10:00:00Zabc", "1234567890", signed, "c2lnbmF0dXJl");
    }
}
