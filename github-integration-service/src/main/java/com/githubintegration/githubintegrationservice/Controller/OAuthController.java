package com.githubintegration.githubintegrationservice.Controller;

import com.githubintegration.githubintegrationservice.Entity.OAuthState;
import com.githubintegration.githubintegrationservice.Repository.OAuthStateRepository;
import com.githubintegration.githubintegrationservice.Service.GithubTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/github-integration")
public class OAuthController {

    private static final Logger LOGGER = Logger.getLogger(OAuthController.class.getName());

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    private final GithubTokenService githubTokenService;
    private final OAuthStateRepository oauthStateRepository;

    public OAuthController(GithubTokenService githubTokenService, OAuthStateRepository oauthStateRepository) {
        this.githubTokenService = githubTokenService;
        this.oauthStateRepository = oauthStateRepository;
    }

    @GetMapping("/oauth/login")
    public ResponseEntity<Void> loginWithGithub(@RequestParam(value = "accessToken", required = false) String accessToken) {
        try {
            LOGGER.info("Requête /oauth/login reçue avec accessToken: " + (accessToken != null ? accessToken : "absent"));
            String state = UUID.randomUUID().toString();
            String userId = accessToken != null ? githubTokenService.getUserIdFromToken("Bearer " + accessToken) : null;
            if (userId != null) {
                OAuthState oauthState = new OAuthState();
                oauthState.setState(state);
                oauthState.setUserId(userId);
                oauthStateRepository.save(oauthState);
                LOGGER.info("État OAuth enregistré: state=" + state + ", userId=" + userId);
            } else {
                LOGGER.warning("Aucun userId disponible pour l'état OAuth. Le state sera utilisé sans userId: " + state);
                String encodedMessage = URLEncoder.encode("Aucun token d'accès valide fourni", StandardCharsets.UTF_8);
                String redirectUrl = "http://localhost:3000/user/dashboard/integration?github=error&message=" + encodedMessage;
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
            }

            String githubUrl = "https://github.com/login/oauth/authorize"
                    + "?client_id=" + clientId
                    + "&redirect_uri=" + redirectUri
                    + "&scope=repo,user"
                    + "&state=" + state;
            LOGGER.info("Redirection vers GitHub OAuth: " + githubUrl);
            URI redirect = URI.create(githubUrl);
            return ResponseEntity.status(HttpStatus.FOUND).location(redirect).build();
        } catch (Exception e) {
            LOGGER.severe("Erreur dans /oauth/login: " + e.getMessage());
            String encodedMessage = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            String redirectUrl = "http://localhost:3000/user/dashboard/integration?github=error&message=" + encodedMessage;
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
        }
    }

    @GetMapping("/oauth/callback")
    public ResponseEntity<Void> callback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state) {
        try {
            LOGGER.info("Callback reçu avec code: " + code + ", state: " + (state != null ? state : "absent"));
            String userId = null;
            if (state != null) {
                userId = oauthStateRepository.findById(state)
                        .map(OAuthState::getUserId)
                        .orElse(null);
                if (userId != null) {
                    LOGGER.info("UserId récupéré depuis state: " + userId);
                    oauthStateRepository.deleteById(state); // Nettoyage
                } else {
                    LOGGER.warning("Aucun userId trouvé pour state: " + state);
                }
            } else {
                LOGGER.warning("Paramètre state manquant dans le callback");
            }

            if (userId == null) {
                throw new IllegalArgumentException("Impossible de déterminer le userId pour associer le token GitHub");
            }

            githubTokenService.exchangeCodeForToken(code, userId);
            LOGGER.info("Token GitHub échangé avec succès pour le code: " + code);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:3000/user/dashboard/integration?github=success"))
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Erreur dans /oauth/callback: " + e.getMessage());
            String encodedMessage = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            String redirectUrl = "http://localhost:3000/user/dashboard/integration?github=error&message=" + encodedMessage;
            LOGGER.info("Redirection avec erreur: " + redirectUrl);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        }
    }

    @GetMapping("/check-token")
    public ResponseEntity<Map<String, Object>> checkGithubToken(
            @RequestHeader("Authorization") String authorization) {
        try {
            LOGGER.info("Vérification du token pour Authorization: " + authorization);
            boolean hasToken = githubTokenService.hasGithubToken(authorization);
            return ResponseEntity.ok(Map.of("hasToken", hasToken));
        } catch (Exception e) {
            LOGGER.severe("Erreur dans /check-token: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("hasToken", false, "error", e.getMessage()));
        }
    }
}