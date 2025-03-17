package com.auth.authentification_service.Controller;

import com.auth.authentification_service.DTO.LoginRequest;
import com.auth.authentification_service.DTO.TokenDto;
import com.auth.authentification_service.DTO.UserDto;
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
        return keycloakService.createUser(userDTO);
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



}
