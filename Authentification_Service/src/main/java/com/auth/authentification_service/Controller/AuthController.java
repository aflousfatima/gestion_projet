package com.auth.authentification_service.Controller;

import com.auth.authentification_service.DTO.LoginRequest;
import com.auth.authentification_service.DTO.UserDto;
import com.auth.authentification_service.Service.KeycloakService;
import com.auth.authentification_service.Service.LoginService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/api/")
@CrossOrigin(origins = "http://localhost:3000")
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
            System.out.println("Tentative d'authentification pour l'utilisateur : " + request.getEmail());

            // Authentifier l'utilisateur avec email et mot de passe
            String accessToken = loginService.authenticateUser(request.getEmail(), request.getPassword());

            // Vérifier si le token d'accès est obtenu
            if (accessToken != null) {
                System.out.println("Token d'accès obtenu avec succès pour l'utilisateur : " + request.getEmail());
                // Décoder le token pour récupérer l'ID utilisateur
                String userId = loginService.decodeToken(accessToken);  // Appelle la méthode pour décoder le token
                System.out.println("User ID extrait du token : " + userId);

                // Stocker le token dans un cookie sécurisé HttpOnly
                Cookie cookie = new Cookie("SESSION_TOKEN", accessToken);
                cookie.setHttpOnly(true);
                cookie.setSecure(true);  // Assurez-vous d'utiliser HTTPS
                cookie.setPath("/");  // Rendre le cookie accessible sur tout le domaine
                response.addCookie(cookie);
                System.out.println("Cookie SESSION_TOKEN ajouté à la réponse");

                return ResponseEntity.ok().build();
            } else {
                System.out.println("Échec de l'authentification pour l'utilisateur : " + request.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Échec de l'authentification");
            }
        } catch (Exception e) {
            System.out.println("Erreur pendant le processus d'authentification : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Échec de l'authentification");
        }
    }


}
