package com.githubintegration.githubintegrationservice.Controller;

import com.githubintegration.githubintegrationservice.Entity.OAuthState;
import com.githubintegration.githubintegrationservice.Repository.OAuthStateRepository;
import com.githubintegration.githubintegrationservice.Service.GithubTokenService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
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
@OpenAPIDefinition(info = @Info(
        title = "API d'Authentification GitHub",
        version = "1.0",
        description = "Cette API gère l'authentification OAuth avec GitHub et la gestion des tokens d'accès."
),
        servers = @Server(
                url = "http://localhost:8087/"
        ))
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
    @Operation(summary = "Initiation de la connexion OAuth avec GitHub",
            description = "Cette méthode redirige l'utilisateur vers la page d'autorisation OAuth de GitHub pour commencer le processus d'authentification.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirection vers la page d'autorisation GitHub"),
            @ApiResponse(responseCode = "400", description = "Requête invalide, token d'accès manquant ou invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de l'initiation de la connexion")
    })
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
    @Operation(summary = "Callback OAuth GitHub",
            description = "Cette méthode traite le callback de GitHub après l'autorisation OAuth, échange le code contre un token d'accès et redirige l'utilisateur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirection vers le tableau de bord après un échange de token réussi ou en cas d'erreur"),
            @ApiResponse(responseCode = "400", description = "Requête invalide, code ou état manquant"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors du traitement du callback")
    })
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

    @Operation(summary = "Vérifier l'existence d'un token GitHub",
            description = "Cette méthode vérifie si un token GitHub est associé à l'utilisateur authentifié.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vérification effectuée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide, token d'accès manquant ou invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la vérification du token")
    })
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
    @Operation(summary = "Supprimer un token GitHub",
            description = "Cette méthode supprime le token GitHub associé à l'utilisateur authentifié.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token supprimé avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide, token non trouvé"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la suppression du token")
    })
    @DeleteMapping("/remove-token")
    public ResponseEntity<Void> removeGithubToken(@RequestHeader("Authorization") String authorization) {
        LOGGER.info("Requête de suppression du token reçue avec Authorization: " + authorization);
        try {
            githubTokenService.removeToken(authorization);
            LOGGER.info("Token GitHub supprimé avec succès");
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Échec de la suppression du token : " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de la suppression du token : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @Operation(summary = "Récupérer un token d'accès GitHub",
            description = "Cette méthode récupère le token d'accès GitHub associé à un userId spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token d'accès récupéré avec succès"),
            @ApiResponse(responseCode = "404", description = "Aucun token trouvé pour l'userId spécifié"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération du token")
    })
    @GetMapping("/token/{userId}")
    public ResponseEntity<Map<String, String>> getAccessToken(@PathVariable String userId) {
        try {
            String accessToken = githubTokenService.getAccessTokenByUserId(userId);
            if (accessToken != null) {
                LOGGER.info("Retrieved GitHub token for userId: " + userId);
                return ResponseEntity.ok(Map.of("accessToken", accessToken));
            }
            LOGGER.warning("No GitHub token found for userId: " + userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No GitHub token found for userId: " + userId));
        } catch (Exception e) {
            LOGGER.severe("Error fetching token for userId: " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch token: " + e.getMessage()));
        }
    }
}