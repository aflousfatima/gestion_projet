package com.githubintegration.githubintegrationservice.Service;

import com.githubintegration.githubintegrationservice.Config.AuthClient;
import com.githubintegration.githubintegrationservice.Entity.GithubToken;
import com.githubintegration.githubintegrationservice.Repository.GithubTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class GithubTokenService {

    private static final Logger LOGGER = Logger.getLogger(GithubTokenService.class.getName());

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.client-secret}")
    private String clientSecret;

    private final GithubTokenRepository tokenRepository;
    private final AuthClient authClient;
    private final RestTemplate restTemplate;

    public GithubTokenService(GithubTokenRepository tokenRepository,
                              AuthClient authClient,
                              RestTemplate restTemplate) {
        this.tokenRepository = tokenRepository;
        this.authClient = authClient;
        this.restTemplate = restTemplate;
    }

    public void exchangeCodeForToken(String code, String userId) {
        LOGGER.info("Échange du code GitHub pour un access_token: " + code + ", userId: " + userId);
        try {
            String tokenUrl = "https://github.com/login/oauth/access_token";
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getBody() == null || response.getBody().containsKey("error")) {
                String errorDesc = response.getBody() != null ? (String) response.getBody().get("error_description") : "Réponse vide";
                LOGGER.severe("Erreur GitHub : " + errorDesc);
                throw new RuntimeException("Erreur lors de l'échange du code GitHub : " + errorDesc);
            }

            String accessToken = (String) response.getBody().get("access_token");
            if (accessToken == null || accessToken.isEmpty()) {
                LOGGER.severe("Access_token manquant dans la réponse GitHub");
                throw new RuntimeException("Access_token manquant dans la réponse GitHub");
            }

            if (userId == null || userId.trim().isEmpty()) {
                LOGGER.severe("UserId null ou vide");
                throw new IllegalArgumentException("UserId requis pour associer le token GitHub");
            }

            saveToken(userId, accessToken);
            LOGGER.info("Token GitHub enregistré pour userId: " + userId);
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de l'échange du code : " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'échange du code GitHub : " + e.getMessage(), e);
        }
    }

    public String getUserIdFromToken(String authorization) {
        LOGGER.info("Tentative de décodage du token: " + (authorization != null ? authorization : "absent"));
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            LOGGER.warning("En-tête Authorization manquant ou mal formé");
            return null;
        }
        try {
            String userId = authClient.decodeToken(authorization);
            if (userId == null || userId.isEmpty()) {
                LOGGER.warning("UserId null ou vide après décodage du token JWT");
                return null;
            }
            LOGGER.info("UserId récupéré: " + userId);
            return userId;
        } catch (Exception e) {
            LOGGER.severe("Erreur lors du décodage du token : " + e.getMessage());
            return null;
        }
    }

    public boolean hasGithubToken(String authorization) {
        LOGGER.info("Vérification du token GitHub pour Authorization: " + authorization);
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                LOGGER.warning("En-tête Authorization manquant ou mal formé");
                throw new IllegalArgumentException("En-tête Authorization invalide");
            }

            String userId = authClient.decodeToken(authorization);
            if (userId == null || userId.isEmpty()) {
                LOGGER.warning("UserId null ou vide après décodage du token JWT");
                throw new RuntimeException("Impossible de récupérer le userId depuis le token JWT");
            }

            boolean hasToken = tokenRepository.findByUserId(userId)
                    .map(GithubToken::getAccessToken)
                    .isPresent();
            LOGGER.info("hasToken: " + hasToken + " pour userId: " + userId);
            return hasToken;
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de la vérification du token : " + e.getMessage());
            throw new RuntimeException("Erreur lors de la vérification du token GitHub : " + e.getMessage(), e);
        }
    }

    public void saveToken(String userId, String accessToken) {
        LOGGER.info("Enregistrement du token pour userId: " + userId);
        tokenRepository.findByUserId(userId).ifPresentOrElse(
                existingToken -> {
                    existingToken.setAccessToken(accessToken);
                    tokenRepository.save(existingToken);
                    LOGGER.info("Token mis à jour pour userId: " + userId);
                },
                () -> {
                    GithubToken newToken = new GithubToken();
                    newToken.setUserId(userId);
                    newToken.setAccessToken(accessToken);
                    tokenRepository.save(newToken);
                    LOGGER.info("Nouveau token créé pour userId: " + userId);
                }
        );
    }

    public String getAccessTokenByUserId(String userId) {
        LOGGER.info("Récupération du token pour userId: " + userId);
        return tokenRepository.findByUserId(userId)
                .map(GithubToken::getAccessToken)
                .orElse(null);
    }
}