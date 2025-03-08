package com.auth.authentification_service.Controller;

import com.auth.authentification_service.DTO.UserDto;
import com.auth.authentification_service.Service.KeycloakService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {
    private final KeycloakService keycloakService;

    public AuthController(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> createUser(@RequestBody UserDto userDTO) {
        return keycloakService.createUser(userDTO);
    }
}
