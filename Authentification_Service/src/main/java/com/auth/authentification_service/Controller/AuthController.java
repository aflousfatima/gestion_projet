package com.auth.authentification_service.Controller;

import com.auth.authentification_service.DTO.LoginRequest;
import com.auth.authentification_service.DTO.TokenDto;
import com.auth.authentification_service.DTO.UserDto;
import com.auth.authentification_service.DTO.UserInfoDto;
import com.auth.authentification_service.Service.KeycloakService;
import com.auth.authentification_service.Service.LoginService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;

import java.util.Collections;

@RestController
@RequestMapping("/api/")
public class AuthController {
    private final KeycloakService keycloakService;
    private  final LoginService loginService;
    public AuthController(KeycloakService keycloakService , LoginService loginService) {
        this.keycloakService = keycloakService;
        this.loginService = loginService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> createUser(@RequestBody UserDto userDTO) {
        try {
            return keycloakService.createUser(userDTO);
        }catch (Exception e) {
            return ResponseEntity.status(400).body("Erreur lors de l'inscription : " + e.getMessage());
        }
    }
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


}
