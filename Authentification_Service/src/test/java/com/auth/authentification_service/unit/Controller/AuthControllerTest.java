
package com.auth.authentification_service.unit.Controller;

import com.auth.authentification_service.Controller.AuthController;
import com.auth.authentification_service.DTO.*;
import com.auth.authentification_service.Service.KeycloakService;
import com.auth.authentification_service.Service.LoginService;
import com.auth.authentification_service.unit.config.TestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KeycloakService keycloakService;

    @MockitoBean
    private LoginService loginService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void whenCreateUserSuccess_thenReturnSuccessMessage() throws Exception {
        // Given
        UserDto userDto = new UserDto();
        userDto.setUsername("testuser");
        userDto.setEmail("test@example.com");
        userDto.setPassword("password123");
        userDto.setFirstName("Test");
        userDto.setLastName("User");

        when(keycloakService.createUser(any(UserDto.class)))
                .thenReturn(ResponseEntity.ok("Utilisateur créé avec succès"));

        // When & Then
        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Utilisateur créé avec succès"));

        verify(keycloakService).createUser(any(UserDto.class));
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenCreateUserWithRuntimeException_thenReturnBadRequest() throws Exception {
        // Given
        UserDto userDto = new UserDto();
        userDto.setUsername("testuser");
        userDto.setEmail("test@example.com");
        userDto.setPassword("password123");
        userDto.setFirstName("Test");
        userDto.setLastName("User");

        when(keycloakService.createUser(any(UserDto.class)))
                .thenThrow(new RuntimeException("Utilisateur existe déjà"));

        // When & Then
        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Utilisateur existe déjà"));

        verify(keycloakService).createUser(any(UserDto.class));
        verifyNoInteractions(loginService);
    }


    @Test
    public void whenCreateUserWithGenericException_thenReturnBadRequest() throws Exception {
        // Given
        UserDto userDto = new UserDto();
        userDto.setUsername("testuser");
        userDto.setEmail("test@example.com");
        userDto.setPassword("password123");
        userDto.setFirstName("Test");
        userDto.setLastName("User");

        when(keycloakService.createUser(any(UserDto.class)))
                .thenThrow(new RuntimeException("Erreur serveur inattendue"));

        // When & Then
        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Erreur serveur inattendue"));

        verify(keycloakService).createUser(any(UserDto.class));
        verifyNoInteractions(loginService);
    }


    // Tests pour /api/login

    // Tests pour /api/login
    @Test
    public void whenLoginSuccess_thenReturnAccessTokenAndSetRefreshTokenCookie() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        TokenDto tokenDto = new TokenDto("access-token-123", "refresh-token-123");

        when(loginService.authenticateUser(anyString(), anyString())).thenReturn(tokenDto);

        // When & Then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token-123"))
                .andExpect(cookie().exists("REFRESH_TOKEN"))
                .andExpect(cookie().value("REFRESH_TOKEN", "refresh-token-123"))
                .andExpect(cookie().httpOnly("REFRESH_TOKEN", true))
                .andExpect(cookie().path("REFRESH_TOKEN", "/"))
                .andExpect(cookie().maxAge("REFRESH_TOKEN", 7 * 24 * 60 * 60));

        verify(loginService).authenticateUser("test@example.com", "password123");
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenLoginFails_thenReturnUnauthorized() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("wrongpassword");

        when(loginService.authenticateUser(anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Échec de l'authentification: Invalid credentials"));

        verify(loginService).authenticateUser("test@example.com", "wrongpassword");
        verifyNoInteractions(keycloakService);
    }

    // Tests pour /api/refresh

    @Test
    public void whenRefreshTokenMissing_thenReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Refresh token manquant !"));

        verifyNoInteractions(loginService);
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenRefreshTokenInvalid_thenReturnUnauthorized() throws Exception {
        // Given
        String refreshToken = "invalid-refresh-token";

        when(loginService.refreshToken(refreshToken))
                .thenThrow(new RuntimeException("Invalid refresh token"));

        // When & Then
        mockMvc.perform(post("/api/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Échec du renouvellement du token"));

        verify(loginService).refreshToken(refreshToken);
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenAssignManagerRoleSuccess_thenReturnUserId() throws Exception {
        // Given
        String token = "valid-token";
        String userId = "user-123";
        when(loginService.decodeToken(token)).thenReturn(userId);
        doNothing().when(loginService).assignManagerRoleToUser(userId);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/assign-manager-role")
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", userId).claim("scope", "USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string(userId));

        verify(loginService).decodeToken(token);
        verify(loginService).assignManagerRoleToUser(userId);
        verifyNoInteractions(keycloakService);
    }


    @Test
    public void whenAssignManagerRoleInvalidToken_thenReturnBadRequest() throws Exception {
        // Given
        String token = "invalid-token";
        when(loginService.decodeToken(token)).thenReturn(null);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/assign-manager-role")
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "user-123").claim("scope", "USER"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token invalide ou mal formé"));

        verify(loginService).decodeToken(token);
        verify(loginService, never()).assignManagerRoleToUser(anyString());
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenAssignManagerRoleThrowsException_thenReturnInternalServerError() throws Exception {
        // Given
        String token = "valid-token";
        when(loginService.decodeToken(token)).thenThrow(new RuntimeException("Decode error"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/assign-manager-role")
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "user-123").claim("scope", "USER"))))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erreur lors du décodage du token : Decode error"));

        verify(loginService).decodeToken(token);
        verify(loginService, never()).assignManagerRoleToUser(anyString());
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenExtractUserIdSuccess_thenReturnUserId() throws Exception {
        // Given
        String token = "valid-token";
        String userId = "user-123";
        when(loginService.decodeToken(token)).thenReturn(userId);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user-id")
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", userId).claim("scope", "USER"))))
                .andExpect(status().isOk())
                .andExpect(content().string(userId));

        verify(loginService).decodeToken(token);
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenExtractUserIdInvalidToken_thenReturnBadRequest() throws Exception {
        // Given
        String token = "invalid-token";
        when(loginService.decodeToken(token)).thenReturn(null);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user-id")
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "user-123").claim("scope", "USER"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token invalide ou mal formé"));

        verify(loginService).decodeToken(token);
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenExtractUserIdThrowsException_thenReturnInternalServerError() throws Exception {
        // Given
        String token = "valid-token";
        when(loginService.decodeToken(token)).thenThrow(new RuntimeException("Decode error"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user-id")
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.claim("sub", "user-123").claim("scope", "USER"))))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erreur lors du décodage du token : Decode error"));

        verify(loginService).decodeToken(token);
        verifyNoInteractions(keycloakService);
    }


    @Test
    public void whenGetUserInfoSuccess_thenReturnUserInfo() throws Exception {
        // Given
        String token = "valid-token";
        UserInfoDto userInfo = new UserInfoDto("user-123", "testuser1", "testuser2", "test@example.com");
        when(loginService.getUserInfo(token)).thenReturn(userInfo);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/me")
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.user("user-123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.firstName").value("testuser1"))
                .andExpect(jsonPath("$.lastName").value("testuser2"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(loginService).getUserInfo(token);
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenGetUserInfoInvalidToken_thenReturnUnauthorized() throws Exception {
        // Given
        String token = "invalid-token";
        when(loginService.getUserInfo(token)).thenThrow(new RuntimeException("Invalid token"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/me")
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.user("user-123").roles("USER")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Token invalide")); // Corrigé pour correspondre à la réponse réelle

        verify(loginService).getUserInfo(token);
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenLogoutSuccess_thenReturnSuccessMessageAndClearCookie() throws Exception {
        // Given
        String refreshToken = "refresh-token-123";
        doNothing().when(loginService).logout(refreshToken);

        // When & Then
        mockMvc.perform(post("/api/logout")
                        .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Déconnexion réussie"))
                .andExpect(cookie().exists("REFRESH_TOKEN"))
                .andExpect(cookie().value("REFRESH_TOKEN", ""))
                .andExpect(cookie().maxAge("REFRESH_TOKEN", 0));

        verify(loginService).logout(refreshToken);
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenLogoutMissingRefreshToken_thenReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Aucun refresh token fourni"));

        verifyNoInteractions(loginService);
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenLogoutThrowsException_thenReturnInternalServerError() throws Exception {
        // Given
        String refreshToken = "refresh-token-123";
        doThrow(new RuntimeException("Logout error")).when(loginService).logout(refreshToken);

        // When & Then
        mockMvc.perform(post("/api/logout")
                        .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", refreshToken)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erreur lors de la déconnexion"));

        verify(loginService).logout(refreshToken);
        verifyNoInteractions(keycloakService);
    }


    @Test
    public void whenUpdateUserSuccess_thenReturnSuccessMessage() throws Exception {
        // Given
        String token = "valid-token";
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", "John");
        userData.put("lastName", "Doe");
        when(keycloakService.updateUser(token, userData)).thenReturn(ResponseEntity.ok("Utilisateur mis à jour avec succès"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"John\",\"lastName\":\"Doe\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Utilisateur mis à jour avec succès"));

        verify(keycloakService).updateUser(token, userData);
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenUpdateUserInvalidToken_thenReturnUnauthorized() throws Exception {
        // Given
        String token = "invalid-token";
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", "John");
        userData.put("lastName", "Doe");
        when(keycloakService.updateUser(token, userData)).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalide ou non autorisé"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"John\",\"lastName\":\"Doe\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Token invalide ou non autorisé"));

        verify(keycloakService).updateUser(token, userData);
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenUpdateUserServerError_thenReturnInternalServerError() throws Exception {
        // Given
        String token = "valid-token";
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", "John");
        userData.put("lastName", "Doe");
        when(keycloakService.updateUser(token, userData)).thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur serveur lors de la mise à jour"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"John\",\"lastName\":\"Doe\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erreur serveur lors de la mise à jour"));

        verify(keycloakService).updateUser(token, userData);
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenChangePasswordSuccess_thenReturnSuccessMessage() throws Exception {
        // Given
        String token = "valid-token";
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("newPassword", "newPass123");
        when(keycloakService.changePassword(token, passwordData)).thenReturn(ResponseEntity.ok("Mot de passe changé avec succès"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"newPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mot de passe changé avec succès"));

        verify(keycloakService).changePassword(token, passwordData);
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenChangePasswordInvalidToken_thenReturnUnauthorized() throws Exception {
        // Given
        String token = "invalid-token";
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("newPassword", "newPass123");
        when(keycloakService.changePassword(token, passwordData)).thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalide ou non autorisé"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"newPass123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Token invalide ou non autorisé"));

        verify(keycloakService).changePassword(token, passwordData);
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenChangePasswordServerError_thenReturnInternalServerError() throws Exception {
        // Given
        String token = "valid-token";
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("newPassword", "newPass123");
        when(keycloakService.changePassword(token, passwordData)).thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur serveur lors du changement de mot de passe"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"newPass123\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erreur serveur lors du changement de mot de passe"));

        verify(keycloakService).changePassword(token, passwordData);
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenGetUserDetailsByAuthIdSuccess_thenReturnUserDetails() throws Exception {
        // Given
        String authId = "auth-123";
        String token = "valid-token";
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("id", "user-123");
        userDetails.put("username", "testuser");
        when(keycloakService.getUserDetailsByAuthId(authId, token)).thenReturn(userDetails);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/users/" + authId)
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.user("user-123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(keycloakService).getUserDetailsByAuthId(authId, token);
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenGetUserDetailsByAuthIdInvalidToken_thenReturnUnauthorized() throws Exception {
        // Given
        String authId = "auth-123";
        String token = "invalid-token";
        when(keycloakService.getUserDetailsByAuthId(authId, token)).thenReturn(null);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/users/" + authId)
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.user("user-123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());

        verify(keycloakService).getUserDetailsByAuthId(authId, token);
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenGetUserDetailsByAuthIdServerError_thenReturnInternalServerError() throws Exception {
        // Given
        String authId = "auth-123";
        String token = "valid-token";
        when(keycloakService.getUserDetailsByAuthId(authId, token)).thenReturn(null);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/users/" + authId)
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.user("user-123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());

        verify(keycloakService).getUserDetailsByAuthId(authId, token);
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenDecodeTokenSuccess_thenReturnDecodedResult() throws Exception {
        // Given
        String token = "valid-token";
        String result = "user-123";
        when(loginService.decodeToken(token)).thenReturn(result);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/decode-token")
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.user("user-123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(result));

        verify(loginService).decodeToken(token);
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenDecodeTokenMissingOrMalformed_thenReturnBadRequest() throws Exception {
        // Given
        String token = "invalid-token";

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/decode-token")
                        .header("Authorization", token) // Pas de "Bearer "
                        .with(SecurityMockMvcRequestPostProcessors.user("user-123").roles("USER")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Le token est manquant ou mal formaté."));

        verifyNoInteractions(loginService);
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenDecodeTokenThrowsException_thenReturnInternalServerError() throws Exception {
        // Given
        String token = "valid-token";
        when(loginService.decodeToken(token)).thenThrow(new RuntimeException("Decode error"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/decode-token")
                        .header("Authorization", "Bearer " + token)
                        .with(SecurityMockMvcRequestPostProcessors.user("user-123").roles("USER")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erreur lors du décodage du token : Decode error"));

        verify(loginService).decodeToken(token);
        verifyNoInteractions(keycloakService);
    }

    @Test
    public void whenGetUsersByIdsSuccess_thenReturnUserList() throws Exception {
        // Given
        String token = "valid-token";
        List<String> userIds = Arrays.asList("user-123", "user-456");
        UserDto user1 = new UserDto();
        user1.setId("user-123");
        user1.setUsername("testuser1");
        user1.setEmail("test1@example.com");
        UserDto user2 = new UserDto();
        user2.setId("user-456");
        user2.setUsername("testuser2");
        user2.setEmail("test2@example.com");
        List<UserDto> users = Arrays.asList(user1, user2);
        when(keycloakService.getUsersByIds(userIds)).thenReturn(users);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks_reponsibles/by-ids")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"user-123\", \"user-456\"]")
                        .with(SecurityMockMvcRequestPostProcessors.user("user-123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("user-123"))
                .andExpect(jsonPath("$[0].username").value("testuser1"))
                .andExpect(jsonPath("$[0].email").value("test1@example.com"))
                .andExpect(jsonPath("$[1].id").value("user-456"))
                .andExpect(jsonPath("$[1].username").value("testuser2"))
                .andExpect(jsonPath("$[1].email").value("test2@example.com"));

        verify(keycloakService).getUsersByIds(userIds);
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenGetUsersByIdsInvalidToken_thenReturnUnauthorized() throws Exception {
        // Given
        String token = "invalid-token";
        List<String> userIds = Arrays.asList("user-123", "user-456");
        when(keycloakService.getUsersByIds(userIds)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks_reponsibles/by-ids")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"user-123\", \"user-456\"]")
                        .with(SecurityMockMvcRequestPostProcessors.user("user-123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(keycloakService).getUsersByIds(userIds);
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenGetUsersByIdsServerError_thenReturnInternalServerError() throws Exception {
        // Given
        String token = "valid-token";
        List<String> userIds = Arrays.asList("user-123", "user-456");
        when(keycloakService.getUsersByIds(userIds)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks_reponsibles/by-ids")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"user-123\", \"user-456\"]")
                        .with(SecurityMockMvcRequestPostProcessors.user("user-123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(keycloakService).getUsersByIds(userIds);
        verifyNoInteractions(loginService);
    }


    @Test
    public void whenGetProjectMembersByUserIdSuccess_thenReturnProjectMembers() throws Exception {
        // Given
        String userId = "user-123";
        ProjectMemberDTO member1 = new ProjectMemberDTO(2L, "MEMBER");
        ProjectMemberDTO member2 = new ProjectMemberDTO(3L, "LEADER");
        List<ProjectMemberDTO> projectMembers = Arrays.asList(member1, member2);
        when(keycloakService.getProjectMembersByUserId(userId)).thenReturn(projectMembers);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/project-members/by-user")
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(2L))
                .andExpect(jsonPath("$[0].roleInProject").value("MEMBER"))
                .andExpect(jsonPath("$[1].projectId").value(3L))
                .andExpect(jsonPath("$[1].roleInProject").value("LEADER"));

        verify(keycloakService).getProjectMembersByUserId(userId);
        verifyNoInteractions(loginService);
    }
    @Test
    public void whenGetProjectMembersByUserIdEmptyResult_thenReturnEmptyList() throws Exception {
        // Given
        String userId = "user-123";
        when(keycloakService.getProjectMembersByUserId(userId)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/project-members/by-user")
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(keycloakService).getProjectMembersByUserId(userId);
        verifyNoInteractions(loginService);
    }

    @Test
    public void whenGetProjectMembersByUserIdServerError_thenReturnInternalServerError() throws Exception {
        // Given
        String userId = "user-123";
        when(keycloakService.getProjectMembersByUserId(userId)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/project-members/by-user")
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(keycloakService).getProjectMembersByUserId(userId);
        verifyNoInteractions(loginService);
    }
}
