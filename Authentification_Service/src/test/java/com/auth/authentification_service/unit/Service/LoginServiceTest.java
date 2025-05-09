package com.auth.authentification_service.unit.Service;

import com.auth.authentification_service.DTO.KeycloakTokenResponse;
import com.auth.authentification_service.DTO.TokenDto;
import com.auth.authentification_service.DTO.UserInfoDto;
import com.auth.authentification_service.Service.KeycloakService;
import com.auth.authentification_service.Service.LoginService;
import com.auth.authentification_service.Service.VaultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private VaultService vaultService;

    @Mock
    private KeycloakService keycloakService;

    @InjectMocks
    private LoginService loginService;

    private final String keycloakUrl = "http://localhost:8080/auth";
    private final String keycloakRealm = "my-realm";
    private final String keycloakClientId = "my-client";
    private final String clientSecret = "secret";

    @BeforeEach
    void setUp() throws Exception {
        loginService = new LoginService(restTemplate, vaultService, keycloakService);
        setField(loginService, "keycloakUrl", keycloakUrl);
        setField(loginService, "keycloakRealm", keycloakRealm);
        setField(loginService, "keycloakClientId", keycloakClientId);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("Should authenticate user successfully and return tokens")
    void authenticateUser_success() throws Exception {
        // Arrange
        when(vaultService.getClientSecret()).thenReturn(clientSecret);
        String email = "test@example.com";
        String password = "password";
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse();
        tokenResponse.setAccessToken("access-token");
        tokenResponse.setRefreshToken("refresh-token");

        ResponseEntity<KeycloakTokenResponse> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(KeycloakTokenResponse.class)))
                .thenReturn(responseEntity);

        // Act
        TokenDto result = loginService.authenticateUser(email, password);

        // Assert
        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        verify(restTemplate).exchange(
                eq(keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(KeycloakTokenResponse.class)
        );
        verify(vaultService).getClientSecret();
    }

    @Test
    @DisplayName("Should throw exception when authentication fails")
    void authenticateUser_failure_throwsException() throws Exception {
        // Arrange
        when(vaultService.getClientSecret()).thenReturn(clientSecret);
        String email = "test@example.com";
        String password = "password";
        ResponseEntity<KeycloakTokenResponse> responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(KeycloakTokenResponse.class)))
                .thenReturn(responseEntity);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> loginService.authenticateUser(email, password));
        assertEquals("Échec de l'authentification avec Keycloak", exception.getMessage());
        verify(vaultService).getClientSecret();
    }

    @Test
    @DisplayName("Should refresh token successfully and return new tokens")
    void refreshToken_success() throws Exception {
        // Arrange
        when(vaultService.getClientSecret()).thenReturn(clientSecret);
        String refreshToken = "refresh-token";
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse();
        tokenResponse.setAccessToken("new-access-token");
        tokenResponse.setRefreshToken("new-refresh-token");

        ResponseEntity<KeycloakTokenResponse> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(KeycloakTokenResponse.class)))
                .thenReturn(responseEntity);

        // Act
        TokenDto result = loginService.refreshToken(refreshToken);

        // Assert
        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
        verify(restTemplate).exchange(
                eq(keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(KeycloakTokenResponse.class)
        );
        verify(vaultService).getClientSecret();
    }

    @Test
    @DisplayName("Should throw exception when token refresh fails")
    void refreshToken_failure_throwsException() throws Exception {
        // Arrange
        when(vaultService.getClientSecret()).thenReturn(clientSecret);
        String refreshToken = "refresh-token";
        ResponseEntity<KeycloakTokenResponse> responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(KeycloakTokenResponse.class)))
                .thenReturn(responseEntity);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> loginService.refreshToken(refreshToken));
        assertEquals("Échec du rafraîchissement du token", exception.getMessage());
        verify(vaultService).getClientSecret();
    }

    @Test
    @DisplayName("Should decode valid token and return user ID")
    void decodeToken_success() {
        // Arrange
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaXNzIjoiYXV0aC1zZXJ2ZXIiLCJleHAiOjE3Mjg2Mzk5OTl9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        // Act
        String userId = loginService.decodeToken(token);

        // Assert
        assertEquals("1234567890", userId);
    }

    @Test
    @DisplayName("Should return null for invalid token")
    void decodeToken_invalidToken_returnsNull() {
        // Arrange
        String invalidToken = "invalid-token";

        // Act
        String userId = loginService.decodeToken(invalidToken);

        // Assert
        assertNull(userId);
    }

    @Test
    @DisplayName("Should extract user info from valid token")
    void getUserInfo_success() {
        // Arrange
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiZ2l2ZW5fbmFtZSI6IkpvaG4iLCJmYW1pbHlfbmFtZSI6IkRvZSIsImVtYWlsIjoidGVzdEBleGFtcGxlLmNvbSJ9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        // Act
        UserInfoDto userInfo = loginService.getUserInfo(token);

        // Assert
        assertNotNull(userInfo);
        assertEquals("1234567890", userInfo.getId());
        assertEquals("John", userInfo.getFirstName());
        assertEquals("Doe", userInfo.getLastName());
        assertEquals("test@example.com", userInfo.getEmail());
    }

    @Test
    @DisplayName("Should throw exception for invalid token in getUserInfo")
    void getUserInfo_invalidToken_throwsException() {
        // Arrange
        String invalidToken = "invalid-token";

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> loginService.getUserInfo(invalidToken));
        assertEquals("Impossible de décoder le token", exception.getMessage());
    }

    @Test
    @DisplayName("Should assign MANAGER role to user successfully")
    void assignManagerRoleToUser_success() throws Exception {
        // Arrange
        String userId = "user-123";
        String adminToken = "admin-token";
        String roleId = "role-456";
        String rolesResponseJson = "[{\"id\": \"" + roleId + "\", \"name\": \"MANAGER\"}]";

        when(keycloakService.getAdminToken()).thenReturn(adminToken);
        when(restTemplate.exchange(
                eq(keycloakUrl + "/admin/realms/" + keycloakRealm + "/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(rolesResponseJson, HttpStatus.OK));
        when(restTemplate.exchange(
                eq(keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId + "/role-mappings/realm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        // Act
        loginService.assignManagerRoleToUser(userId);

        // Assert
        verify(keycloakService).getAdminToken();
        verify(restTemplate, times(2)).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("Should throw exception when MANAGER role is not found")
    void assignManagerRoleToUser_roleNotFound_throwsException() throws Exception {
        // Arrange
        String userId = "user-123";
        String adminToken = "admin-token";
        String rolesResponseJson = "[{\"id\": \"role-789\", \"name\": \"OTHER_ROLE\"}]";

        when(keycloakService.getAdminToken()).thenReturn(adminToken);
        when(restTemplate.exchange(
                eq(keycloakUrl + "/admin/realms/" + keycloakRealm + "/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(rolesResponseJson, HttpStatus.OK));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> loginService.assignManagerRoleToUser(userId));
        assertEquals("Rôle MANAGER non trouvé.", exception.getMessage());
        verify(keycloakService).getAdminToken();
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }
    @Test
    @DisplayName("Should logout successfully")
    void logout_success() throws Exception {
        // Arrange
        when(vaultService.getClientSecret()).thenReturn(clientSecret);
        String refreshToken = "refresh-token";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(HttpStatus.NO_CONTENT);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        loginService.logout(refreshToken);

        // Assert
        verify(restTemplate).exchange(
                eq(keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/logout"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(vaultService).getClientSecret();
    }

    @Test
    @DisplayName("Should throw exception when logout fails")
    void logout_failure_throwsException() throws Exception {
        // Arrange
        when(vaultService.getClientSecret()).thenReturn(clientSecret);
        String refreshToken = "refresh-token";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> loginService.logout(refreshToken));
        assertEquals("Échec de la déconnexion auprès de Keycloak", exception.getMessage());
        verify(vaultService).getClientSecret();
    }
}