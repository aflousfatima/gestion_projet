package com.auth.authentification_service.Controller;

import com.auth.authentification_service.DTO.LoginRequest;
import com.auth.authentification_service.DTO.TokenDto;
import com.auth.authentification_service.DTO.UserDto;
import com.auth.authentification_service.DTO.UserInfoDto;
import com.auth.authentification_service.Service.KeycloakService;
import com.auth.authentification_service.Service.LoginService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/")
@OpenAPIDefinition(info = @Info(
        title = "API d'authentification",
        version = "1.0",
        description = "Cette API gère l'authentification des utilisateurs et la gestion des tokens."
))
public class AuthController {
    private final KeycloakService keycloakService;
    private  final LoginService loginService;
    public AuthController(KeycloakService keycloakService , LoginService loginService) {
        this.keycloakService = keycloakService;
        this.loginService = loginService;
    }

    @Operation(summary = "Créer un utilisateur",
            description = "Cette méthode permet de créer un nouvel utilisateur dans Keycloak.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Erreur lors de la création de l'utilisateur")
    })
    @PostMapping("/signup")
    public ResponseEntity<String> createUser(@RequestBody UserDto userDTO) {
        try {
            return keycloakService.createUser(userDTO);
        }catch (RuntimeException e) {
            System.out.println("❌ Erreur lors de la création de l'utilisateur : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Erreur inattendue lors de la création de l'utilisateur : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur serveur : " + e.getMessage());
        }
    }

    @Operation(summary = "Authentification d'un utilisateur",
            description = "Cette méthode permet d'authentifier un utilisateur et de lui retourner un access token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentification réussie"),
            @ApiResponse(responseCode = "401", description = "Échec de l'authentification")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            TokenDto tokens = loginService.authenticateUser(request.getEmail(), request.getPassword());

            // Création du cookie HttpOnly avec le refresh_token
            Cookie refreshTokenCookie = new Cookie("REFRESH_TOKEN", tokens.getRefreshToken());
            refreshTokenCookie.setSecure(false); // ⚠ Met à true en prod (HTTPS obligatoire)
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setDomain("localhost");  // Exemple de domaine explicitement défini
            refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 jours
            refreshTokenCookie.setAttribute("SameSite", "Lax"); // Ajoute ceci
            response.addCookie(refreshTokenCookie);  // Envoie du cookie au navigateur

            // Retourner seulement l'access_token dans la réponse JSON
            return ResponseEntity.ok(Collections.singletonMap("access_token", tokens.getAccessToken()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Échec de l'authentification");
        }
    }

    @Operation(summary = "Rafraîchir le token d'accès",
            description = "Cette méthode permet de rafraîchir un token d'accès en utilisant le refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token d'accès rafraîchi avec succès"),
            @ApiResponse(responseCode = "401", description = "Échec du rafraîchissement du token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token manquant !");
        }

        try {
            TokenDto newTokens = loginService.refreshToken(refreshToken);

            // Optionnel : Si Keycloak renvoie un nouveau refresh token, on met à jour le cookie
            Cookie newRefreshTokenCookie = new Cookie("REFRESH_TOKEN", newTokens.getRefreshToken());
            newRefreshTokenCookie.setSecure(false); // ⚠ Met à true en prod (HTTPS obligatoire)
            newRefreshTokenCookie.setHttpOnly(true);
            newRefreshTokenCookie.setPath("/");
            newRefreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 jours
            newRefreshTokenCookie.setAttribute("SameSite", "Lax"); // Ajoute ceci

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, newRefreshTokenCookie.toString());

            // Retourner le nouvel access_token dans la réponse JSON
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(Collections.singletonMap("access_token", newTokens.getAccessToken()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Échec du renouvellement du token");
        }
    }

    @Operation(summary = "Assigner un rôle de manager",
            description = "Cette méthode permet d'assigner un rôle de manager à un utilisateur, après avoir extrait son ID depuis le token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rôle de manager attribué avec succès"),
            @ApiResponse(responseCode = "400", description = "Token invalide ou mal formé"),
            @ApiResponse(responseCode = "500", description = "Erreur lors du décodage du token")
    })
    @GetMapping("/assign-manager-role")
    public ResponseEntity<String> extractManagerId(@RequestHeader("Authorization") String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body("Token invalide ou mal formé");
            }

            // Extraction du token en enlevant le "Bearer "
            String token = authorization.substring(7);

            // Appel au service pour décoder le token et obtenir l'ID utilisateur
            String userId = loginService.decodeToken(token);

            if (userId != null) {
                loginService.assignManagerRoleToUser(userId);
                return ResponseEntity.ok(userId);
            } else {
                return ResponseEntity.badRequest().body("Token invalide ou mal formé");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors du décodage du token : " + e.getMessage());
        }
    }


    @Operation(summary = "Extraire l'ID utilisateur",
            description = "Cette méthode permet d'extraire l'ID de l'utilisateur à partir de son token d'accès.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ID utilisateur extrait avec succès"),
            @ApiResponse(responseCode = "400", description = "Token invalide ou mal formé"),
            @ApiResponse(responseCode = "500", description = "Erreur lors du décodage du token")
    })
    @GetMapping("/user-id")
    public ResponseEntity<String> extractUserId(@RequestHeader("Authorization") String authorization) {
        try {
            // Vérifier si l'en-tête Authorization est présent et bien formé
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body("Token invalide ou mal formé");
            }

            // Extraction du token en enlevant le "Bearer "
            String token = authorization.substring(7);

            // Appel au service pour décoder le token et obtenir l'ID utilisateur
            String userId = loginService.decodeToken(token);

            // Vérifier si l'ID utilisateur a été extrait avec succès
            if (userId != null) {
                return ResponseEntity.ok(userId); // Retourner uniquement l'ID utilisateur
            } else {
                return ResponseEntity.badRequest().body("Token invalide ou mal formé");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors du décodage du token : " + e.getMessage());
        }
    }


    @Operation(summary = "Obtenir les informations de l'utilisateur",
            description = "Cette méthode permet de récupérer les informations d'un utilisateur à partir de son token d'accès.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Informations utilisateur récupérées avec succès"),
            @ApiResponse(responseCode = "401", description = "Token invalide")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            String accessToken = authorizationHeader.replace("Bearer ", "");
            UserInfoDto userInfo = loginService.getUserInfo(accessToken);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalide");
        }
    }


    @Operation(summary = "Déconnexion de l'utilisateur",
            description = "Cette méthode permet de déconnecter l'utilisateur en révoquant son refresh token et supprimant le cookie associé.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Déconnexion réussie"),
            @ApiResponse(responseCode = "400", description = "Refresh token manquant"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la déconnexion")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Aucun refresh token fourni");
        }
        try {
            // Révoquer le refresh token dans Keycloak
            loginService.logout(refreshToken);

            // Supprimer le cookie côté client en mettant une expiration immédiate
            Cookie expiredCookie = new Cookie("REFRESH_TOKEN", "");
            expiredCookie.setHttpOnly(true);
            expiredCookie.setSecure(false); // ⚠ Mettre true en prod
            expiredCookie.setPath("/");
            expiredCookie.setMaxAge(0); // Expiration immédiate
            expiredCookie.setAttribute("SameSite", "Lax");
            response.addCookie(expiredCookie);

            return ResponseEntity.ok().body("Déconnexion réussie");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la déconnexion");
        }
    }

    @Operation(summary = "recuperation des membres de tous les projets de l'entreprise",
            description = "Cette méthode permet de recuperer lesutilisateur qui sont accompagner et affilier a l'un des projets de l'entreprise.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "recuperation réussie"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la recupertion")
    })
    @GetMapping("/team-members")
    public ResponseEntity<?> getTeamMembers(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            String accessToken = authorizationHeader.replace("Bearer ", "");
            List<Map<String, Object>> teamMembers = keycloakService.getTeamMembers(accessToken);
            return ResponseEntity.ok(teamMembers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalide ou erreur lors de la récupération des membres de l'équipe");
        }
    }

    @Operation(summary = "recuperation des membres de l'un des projets de l'entreprise",
            description = "Cette méthode permet de recuperer lesutilisateur qui sont accompagner et affilier a l'un des projets de l'entreprise.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "recuperation réussie"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la recupertion")
    })
    @GetMapping("/team-members/{projectId}")
    public List<Map<String, Object>> getTeamMembersbyProject(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String projectId) {
        String accessToken = authorizationHeader.replace("Bearer ", "");
        return keycloakService.getTeamMembersbyProject(accessToken, projectId);
    }

    @GetMapping("/auth/users/{authId}")
    public ResponseEntity<Map<String, Object>> getUserDetailsByAuthId(
            @PathVariable String authId,
            @RequestHeader("Authorization") String authorizationHeader) {
        String adminToken = authorizationHeader.replace("Bearer ", "");
        Map<String, Object> userDetails = keycloakService.getUserDetailsByAuthId(authId, adminToken);
        return ResponseEntity.ok(userDetails);
    }

    @GetMapping("/auth/decode-token")
    public ResponseEntity<?> decodeToken(@RequestHeader("Authorization") String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity
                        .badRequest()
                        .body("Le token est manquant ou mal formaté.");
            }

            String token = authorization.replace("Bearer ", "");
            String result = loginService.decodeToken(token);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du décodage du token : " + e.getMessage());
        }
    }


    @PostMapping("/tasks_reponsibles/by-ids")
    public List<UserDto> getUsersByIds(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody List<String> userIds
    ) {
        System.out.println("Received Authorization header: " + authorizationHeader);
        System.out.println("Received userIds: " + userIds);
        return keycloakService.getUsersByIds(userIds);
    }

}
