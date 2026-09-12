package com.enosistudio.bruine.steam.service;

import com.enosistudio.bruine.steam.dto.SteamGameDTO;
import com.enosistudio.bruine.steam.dto.SteamOpenidLoginDTO;
import com.enosistudio.bruine.steam.exception.SteamException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SteamService {

    @Value("${steam.token}")
    private String steamApiToken;
    private static final String STEAM_API_URL = "https://api.steampowered.com";
    private static final String OPENID_NAMESPACE = "http://specs.openid.net/auth/2.0";

    public Map<String, Object> getUserData(String steamUserId) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String url = String.format("%s/ISteamUser/GetPlayerSummaries/v2/?key=%s&format=json&steamids=%s", STEAM_API_URL, steamApiToken, steamUserId);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (!response.getStatusCode().isSameCodeAs(HttpStatus.OK)) {
            throw new SteamException("GetPlayerSummaries a répondu " + response.getStatusCode());
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode players = mapper.readTree(response.getBody()).path("response").path("players");

        // Steam répond 200 avec un tableau vide quand l'identifiant ne correspond à personne :
        // sans ce contrôle, le cas partait en NoSuchElementException au premier next().
        if (!players.isArray() || players.isEmpty()) {
            throw new SteamException("Aucun profil Steam pour l'identifiant " + steamUserId);
        }

        return mapper.convertValue(players.get(0), new TypeReference<>() {
        });
    }

    /**
     * Retourne tous les jeux possédés par le joueur.
     * Renvoie une liste vide si le profil est privé ou si l'API ne répond pas.
     */
    public List<SteamGameDTO> getOwnedGames(String steamId) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String url = String.format(
                "%s/IPlayerService/GetOwnedGames/v1/?key=%s&steamid=%s&include_appinfo=1&format=json",
                STEAM_API_URL, steamApiToken, steamId);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (!response.getStatusCode().isSameCodeAs(HttpStatus.OK)) {
            throw new SteamException("GetOwnedGames a répondu " + response.getStatusCode());
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode gamesNode = mapper.readTree(response.getBody())
                .path("response")
                .path("games");

        if (!gamesNode.isArray()) {
            return List.of(); // profil privé ou aucun jeu
        }

        List<SteamGameDTO> games = new ArrayList<>();
        for (JsonNode g : gamesNode) {
            games.add(new SteamGameDTO(
                    g.path("appid").asInt(),
                    g.path("name").asText("Unknown"),
                    g.path("playtime_forever").asInt(0),
                    g.path("img_icon_url").asText("")
            ));
        }
        return games;
    }

    public long getTotalPlaytimeMinutes(String steamId) throws Exception {
        return getOwnedGames(steamId).stream()
                .filter(SteamService::isPlayed)
                .mapToLong(SteamGameDTO::playtimeMinutes)
                .sum();
    }

    /**
     * En dessous d'une heure, un jeu ne compte ni dans la liste ni dans le total.
     */
    public static boolean isPlayed(SteamGameDTO game) {
        return game.playtimeMinutes() >= 60;
    }

    public String buildSteamLoginUrl(String baseUrl) {
        String realm = baseUrl + "/";
        String returnTo = baseUrl + "/steam/login/redirect";

        return "https://steamcommunity.com/openid/login" +
                "?openid.ns=http://specs.openid.net/auth/2.0" +
                "&openid.mode=checkid_setup" +
                "&openid.identity=http://specs.openid.net/auth/2.0/identifier_select" +
                "&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select" +
                "&openid.realm=" + realm +
                "&openid.return_to=" + returnTo;
    }

    public String validateLoginParameters(SteamOpenidLoginDTO dto) throws IllegalArgumentException {
        MultiValueMap<String, String> openidRequest = new LinkedMultiValueMap<>();
        openidRequest.add("openid.ns", dto.getNs());
        openidRequest.add("openid.op_endpoint", dto.getOpEndpoint());
        openidRequest.add("openid.claimed_id", dto.getClaimedId());
        openidRequest.add("openid.identity", dto.getIdentity());
        openidRequest.add("openid.return_to", dto.getReturnTo());
        openidRequest.add("openid.response_nonce", dto.getResponseNonce());
        openidRequest.add("openid.assoc_handle", dto.getAssocHandle());
        openidRequest.add("openid.signed", dto.getSigned());
        openidRequest.add("openid.sig", dto.getSig());
        openidRequest.add("openid.mode", "check_authentication");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(openidRequest, headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.postForEntity("https://steamcommunity.com/openid/login", request, String.class);
        if (!response.getStatusCode().isSameCodeAs(HttpStatus.OK)) {
            throw new IllegalArgumentException(
                    "Steam a répondu " + response.getStatusCode() + " à la vérification du jeton OpenID.");
        }

        Map<String, String> responseBody = this.parseLoginValidationResponse(response.getBody());

        if (!OPENID_NAMESPACE.equals(responseBody.get("ns"))) {
            throw new IllegalArgumentException(
                    "Espace de noms OpenID inattendu dans la réponse Steam : " + responseBody.get("ns"));
        }

        if (!"true".equals(responseBody.get("is_valid"))) {
            throw new IllegalArgumentException(
                    "Steam a rejeté le jeton OpenID (is_valid=" + responseBody.get("is_valid") + ").");
        }

        Pattern p = Pattern.compile("^https?://steamcommunity.com/openid/id/(7656119\\d{10})/?$");
        Matcher m = p.matcher(dto.getIdentity());

        if (!m.find()) {
            throw new IllegalArgumentException(
                    "Identité OpenID inexploitable, aucun identifiant Steam dedans : " + dto.getIdentity());
        }

        return m.group(1); // steamUserId
    }

    private Map<String, String> parseLoginValidationResponse(String response) {
        if (response == null) return Map.of();

        Map<String, String> body = new HashMap<>();
        for (String line : response.split("\n")) {
            // La réponse est une suite de « clé:valeur ». Toute ligne sans séparateur (ligne
            // vide, message inattendu) est ignorée plutôt que de faire planter la connexion.
            String[] keyValue = line.split(":", 2);
            if (keyValue.length < 2) {
                continue;
            }
            body.put(keyValue[0], keyValue[1]);
        }

        return body;
    }

}
