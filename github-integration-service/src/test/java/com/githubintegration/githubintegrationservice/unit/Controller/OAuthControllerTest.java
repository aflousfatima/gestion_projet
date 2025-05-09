package com.githubintegration.githubintegrationservice.unit.Controller;

import com.githubintegration.githubintegrationservice.Controller.OAuthController;
import com.githubintegration.githubintegrationservice.Entity.OAuthState;
import com.githubintegration.githubintegrationservice.Repository.OAuthStateRepository;
import com.githubintegration.githubintegrationservice.Service.GithubTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthControllerTest {

    @Mock
    private GithubTokenService githubTokenService;

    @Mock
    private OAuthStateRepository oauthStateRepository;

    @InjectMocks
    private OAuthController oauthController;

    private static final String USER_ID = "user123";
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String CLIENT_ID = "test-client-id";
    private static final String REDIRECT_URI = "http://localhost:8087/api/github-integration/oauth/callback";

    @BeforeEach
    void setUp() {
        // Injecter les valeurs des propriétés via ReflectionTestUtils
        ReflectionTestUtils.setField(oauthController, "clientId", CLIENT_ID);
        ReflectionTestUtils.setField(oauthController, "redirectUri", REDIRECT_URI);
    }

    @Test
    void loginWithGithub_ValidAccessToken_RedirectsToGithub() {
        // Arrange
        String accessToken = "valid-token";
        when(githubTokenService.getUserIdFromToken("Bearer " + accessToken)).thenReturn(USER_ID);
        OAuthState oauthState = new OAuthState();
        oauthState.setUserId(USER_ID);
        when(oauthStateRepository.save(any(OAuthState.class))).thenAnswer(invocation -> {
            OAuthState state = invocation.getArgument(0);
            oauthState.setState(state.getState()); // Capturer le state généré
            return oauthState;
        });

        // Act
        ResponseEntity<Void> response = oauthController.loginWithGithub(accessToken);

        // Assert
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        String expectedUrl = "https://github.com/login/oauth/authorize?client_id=" + CLIENT_ID +
                "&redirect_uri=" + REDIRECT_URI + "&scope=repo,user&state=" + oauthState.getState();
        assertEquals(URI.create(expectedUrl), response.getHeaders().getLocation());
        verify(oauthStateRepository).save(any(OAuthState.class));
    }

    @Test
    void loginWithGithub_NoAccessToken_RedirectsWithError() {
        // Arrange
        String accessToken = null;

        // Act
        ResponseEntity<Void> response = oauthController.loginWithGithub(accessToken);

        // Assert
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        String expectedUrl = "http://localhost:3000/user/dashboard/integration?github=error&message=Aucun+token+d%27acc%C3%A8s+valide+fourni";
        assertEquals(URI.create(expectedUrl), response.getHeaders().getLocation());
    }

    @Test
    void callback_ValidCodeAndState_SuccessfulRedirect() {
        // Arrange
        String code = "test-code";
        String state = "test-state";
        OAuthState oauthState = new OAuthState();
        oauthState.setState(state);
        oauthState.setUserId(USER_ID);
        when(oauthStateRepository.findById(state)).thenReturn(Optional.of(oauthState));
        doNothing().when(githubTokenService).exchangeCodeForToken(code, USER_ID);
        doNothing().when(oauthStateRepository).deleteById(state);

        // Act
        ResponseEntity<Void> response = oauthController.callback(code, state);

        // Assert
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(URI.create("http://localhost:3000/user/dashboard/integration?github=success"),
                response.getHeaders().getLocation());
        verify(githubTokenService).exchangeCodeForToken(code, USER_ID);
        verify(oauthStateRepository).deleteById(state);
    }

    @Test
    void callback_MissingUserId_RedirectsWithError() {
        // Arrange
        String code = "test-code";
        String state = "test-state";
        when(oauthStateRepository.findById(state)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Void> response = oauthController.callback(code, state);

        // Assert
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        String expectedUrl = "http://localhost:3000/user/dashboard/integration?github=error&message=" +
                URLEncoder.encode("Impossible de déterminer le userId pour associer le token GitHub", StandardCharsets.UTF_8);
        assertEquals(URI.create(expectedUrl), response.getHeaders().getLocation());
    }

    @Test
    void callback_MissingState_RedirectsWithError() {
        // Arrange
        String code = "test-code";
        String state = null;

        // Act
        ResponseEntity<Void> response = oauthController.callback(code, state);

        // Assert
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        String expectedUrl = "http://localhost:3000/user/dashboard/integration?github=error&message=" +
                URLEncoder.encode("Impossible de déterminer le userId pour associer le token GitHub", StandardCharsets.UTF_8);
        assertEquals(URI.create(expectedUrl), response.getHeaders().getLocation());
    }

    @Test
    void checkGithubToken_TokenExists_ReturnsTrue() {
        // Arrange
        String authorization = "Bearer valid-token";
        when(githubTokenService.hasGithubToken(authorization)).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = oauthController.checkGithubToken(authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("hasToken", true), response.getBody());
    }

    @Test
    void checkGithubToken_NoToken_ReturnsFalseWithError() {
        // Arrange
        String authorization = "Bearer invalid-token";
        when(githubTokenService.hasGithubToken(authorization)).thenThrow(new RuntimeException("Invalid token"));

        // Act
        ResponseEntity<Map<String, Object>> response = oauthController.checkGithubToken(authorization);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("hasToken", false, "error", "Invalid token"), response.getBody());
    }

    @Test
    void removeGithubToken_Success_ReturnsOk() {
        // Arrange
        String authorization = "Bearer valid-token";
        doNothing().when(githubTokenService).removeToken(authorization);

        // Act
        ResponseEntity<Void> response = oauthController.removeGithubToken(authorization);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(githubTokenService).removeToken(authorization);
    }

    @Test
    void removeGithubToken_InvalidToken_ReturnsBadRequest() {
        // Arrange
        String authorization = "Bearer invalid-token";
        doThrow(new IllegalArgumentException("Invalid token")).when(githubTokenService).removeToken(authorization);

        // Act
        ResponseEntity<Void> response = oauthController.removeGithubToken(authorization);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getAccessToken_TokenExists_ReturnsToken() {
        // Arrange
        String userId = USER_ID;
        when(githubTokenService.getAccessTokenByUserId(userId)).thenReturn(ACCESS_TOKEN);

        // Act
        ResponseEntity<Map<String, String>> response = oauthController.getAccessToken(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("accessToken", ACCESS_TOKEN), response.getBody());
    }

    @Test
    void getAccessToken_NoToken_ReturnsNotFound() {
        // Arrange
        String userId = USER_ID;
        when(githubTokenService.getAccessTokenByUserId(userId)).thenReturn(null);

        // Act
        ResponseEntity<Map<String, String>> response = oauthController.getAccessToken(userId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(Map.of("error", "No GitHub token found for userId: " + userId), response.getBody());
    }

    @Test
    void getAccessToken_Error_ReturnsInternalServerError() {
        // Arrange
        String userId = USER_ID;
        when(githubTokenService.getAccessTokenByUserId(userId)).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<Map<String, String>> response = oauthController.getAccessToken(userId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(Map.of("error", "Failed to fetch token: Database error"), response.getBody());
    }
}