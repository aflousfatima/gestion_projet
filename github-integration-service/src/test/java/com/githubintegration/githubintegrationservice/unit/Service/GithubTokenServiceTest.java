package com.githubintegration.githubintegrationservice.unit.Service;
import com.githubintegration.githubintegrationservice.Config.AuthClient;
import com.githubintegration.githubintegrationservice.Entity.GithubToken;
import com.githubintegration.githubintegrationservice.Repository.GithubTokenRepository;
import com.githubintegration.githubintegrationservice.Service.GithubTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubTokenServiceTest {

    @Mock
    private GithubTokenRepository tokenRepository;

    @Mock
    private AuthClient authClient;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GithubTokenService githubTokenService;

    @BeforeEach
    void setUp() {
        // Injecter les valeurs des propriétés via ReflectionTestUtils
        ReflectionTestUtils.setField(githubTokenService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(githubTokenService, "clientSecret", "test-client-secret");
    }

    @Test
    void exchangeCodeForToken_Success() {
        // Arrange
        String code = "test-code";
        String userId = "user123";
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("access_token", "test-access-token");

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // Act
        githubTokenService.exchangeCodeForToken(code, userId);

        // Assert
        verify(tokenRepository).findByUserId(userId);
        verify(tokenRepository).save(any(GithubToken.class));
    }

    @Test
    void exchangeCodeForToken_GithubError_ThrowsException() {
        // Arrange
        String code = "test-code";
        String userId = "user123";
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("error", "invalid_request");
        responseBody.put("error_description", "Invalid code");

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                githubTokenService.exchangeCodeForToken(code, userId));
        assertTrue(exception.getMessage().contains("Erreur lors de l'échange du code GitHub"));
    }

    @Test
    void exchangeCodeForToken_NullUserId_ThrowsException() {
        // Arrange
        String code = "test-code";
        // Configurer le mock pour retourner null, simulant l'erreur actuelle
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                githubTokenService.exchangeCodeForToken(code, null));
        assertTrue(exception.getMessage().contains("Erreur lors de l'échange du code GitHub"));
    }
    @Test
    void getUserIdFromToken_ValidToken_ReturnsUserId() {
        // Arrange
        String authorization = "Bearer valid-token";
        String userId = "user123";
        when(authClient.decodeToken(authorization)).thenReturn(userId);

        // Act
        String result = githubTokenService.getUserIdFromToken(authorization);

        // Assert
        assertEquals(userId, result);
    }

    @Test
    void getUserIdFromToken_InvalidToken_ReturnsNull() {
        // Arrange
        String authorization = "Invalid-token";

        // Act
        String result = githubTokenService.getUserIdFromToken(authorization);

        // Assert
        assertNull(result);
    }

    @Test
    void hasGithubToken_ValidTokenAndUserId_ReturnsTrue() {
        // Arrange
        String authorization = "Bearer valid-token";
        String userId = "user123";
        GithubToken token = new GithubToken();
        token.setAccessToken("test-access-token");

        when(authClient.decodeToken(authorization)).thenReturn(userId);
        when(tokenRepository.findByUserId(userId)).thenReturn(Optional.of(token));

        // Act
        boolean result = githubTokenService.hasGithubToken(authorization);

        // Assert
        assertTrue(result);
    }

    @Test
    void hasGithubToken_NoTokenFound_ReturnsFalse() {
        // Arrange
        String authorization = "Bearer valid-token";
        String userId = "user123";

        when(authClient.decodeToken(authorization)).thenReturn(userId);
        when(tokenRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act
        boolean result = githubTokenService.hasGithubToken(authorization);

        // Assert
        assertFalse(result);
    }

    @Test
    void hasGithubToken_InvalidAuthorization_ThrowsException() {
        // Arrange
        String authorization = "Invalid-token";

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                githubTokenService.hasGithubToken(authorization));
        assertEquals("Erreur lors de la vérification du token GitHub : En-tête Authorization invalide", exception.getMessage());
    }

    @Test
    void saveToken_NewToken_SavesToken() {
        // Arrange
        String userId = "user123";
        String accessToken = "test-access-token";
        when(tokenRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act
        githubTokenService.saveToken(userId, accessToken);

        // Assert
        verify(tokenRepository).save(any(GithubToken.class));
    }

    @Test
    void saveToken_ExistingToken_UpdatesToken() {
        // Arrange
        String userId = "user123";
        String accessToken = "test-access-token";
        GithubToken existingToken = new GithubToken();
        existingToken.setAccessToken("old-token");

        when(tokenRepository.findByUserId(userId)).thenReturn(Optional.of(existingToken));

        // Act
        githubTokenService.saveToken(userId, accessToken);

        // Assert
        verify(tokenRepository).save(existingToken);
        assertEquals(accessToken, existingToken.getAccessToken());
    }

    @Test
    void getAccessTokenByUserId_TokenExists_ReturnsToken() {
        // Arrange
        String userId = "user123";
        GithubToken token = new GithubToken();
        token.setAccessToken("test-access-token");

        when(tokenRepository.findByUserId(userId)).thenReturn(Optional.of(token));

        // Act
        String result = githubTokenService.getAccessTokenByUserId(userId);

        // Assert
        assertEquals("test-access-token", result);
    }

    @Test
    void getAccessTokenByUserId_NoToken_ReturnsNull() {
        // Arrange
        String userId = "user123";
        when(tokenRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act
        String result = githubTokenService.getAccessTokenByUserId(userId);

        // Assert
        assertNull(result);
    }

    @Test
    void removeToken_ValidToken_RemovesToken() {
        // Arrange
        String authorization = "Bearer valid-token";
        String userId = "user123";
        when(authClient.decodeToken(authorization)).thenReturn(userId);

        // Act
        githubTokenService.removeToken(authorization);

        // Assert
        verify(tokenRepository).deleteByUserId(userId);
    }

    @Test
    void removeToken_InvalidToken_ThrowsException() {
        // Arrange
        String authorization = "Bearer invalid-token";
        when(authClient.decodeToken(authorization)).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                githubTokenService.removeToken(authorization));
        assertEquals("Utilisateur non trouvé pour le token fourni", exception.getMessage());
    }
}