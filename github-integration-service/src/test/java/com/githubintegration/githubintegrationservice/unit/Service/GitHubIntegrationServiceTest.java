package com.githubintegration.githubintegrationservice.unit.Service;

import com.githubintegration.githubintegrationservice.Config.AuthClient;
import com.githubintegration.githubintegrationservice.Service.GitHubIntegrationService;
import com.githubintegration.githubintegrationservice.Service.GithubTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubIntegrationServiceTest {

    @Mock
    private AuthClient authClient;

    @Mock
    private GithubTokenService githubTokenService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GitHubIntegrationService gitHubIntegrationService;

    private static final String USER_ID = "user123";
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String OWNER = "test-owner";
    private static final String REPO = "test-repo";

    @BeforeEach
    void setUp() {
        // Configurer le mock pour retourner un token valide par défaut
        when(githubTokenService.getAccessTokenByUserId(USER_ID)).thenReturn(ACCESS_TOKEN);
    }

    @Test
    void extractUserId_ValidToken_ReturnsUserId() {
        // Arrange
        String authorization = "Bearer valid-token";
        when(authClient.decodeToken(authorization)).thenReturn(USER_ID);
        lenient().when(githubTokenService.getAccessTokenByUserId(USER_ID)).thenReturn(ACCESS_TOKEN);

        // Act
        String result = gitHubIntegrationService.extractUserId(authorization);

        // Assert
        assertEquals(USER_ID, result);
    }
    @Test
    void extractUserId_InvalidToken_ThrowsException() {
        // Arrange
        String authorization = "Bearer invalid-token";
        when(authClient.decodeToken(authorization)).thenThrow(new RuntimeException("Invalid token"));
        lenient().when(githubTokenService.getAccessTokenByUserId(USER_ID)).thenReturn(ACCESS_TOKEN);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                gitHubIntegrationService.extractUserId(authorization));
        assertEquals("Failed to decode token: Invalid token", exception.getMessage());
    }

    @Test
    void checkRepositoryExists_RepositoryExists_ReturnsTrue() {
        // Arrange
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO;
        ResponseEntity<Object> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(response);

        // Act
        boolean result = gitHubIntegrationService.checkRepositoryExists(OWNER, REPO, USER_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    void checkRepositoryExists_RepositoryNotFound_ReturnsFalse() {
        // Arrange
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO;
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // Act
        boolean result = gitHubIntegrationService.checkRepositoryExists(OWNER, REPO, USER_ID);

        // Assert
        assertFalse(result);
    }

    @Test
    void checkRepositoryExists_NoAccessToken_ThrowsException() {
        // Arrange
        when(githubTokenService.getAccessTokenByUserId(USER_ID)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                gitHubIntegrationService.checkRepositoryExists(OWNER, REPO, USER_ID));
        assertEquals("Unexpected error checking repository: No access token found for user", exception.getMessage());
    }

    @Test
    void getCommits_Success_ReturnsCommits() {
        // Arrange
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/commits";
        List<Object> commits = List.of(new Object());
        ResponseEntity<Object> response = new ResponseEntity<>(commits, HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(response);

        // Act
        Object result = gitHubIntegrationService.getCommits(OWNER, REPO, USER_ID, null, null, null, null);

        // Assert
        assertEquals(commits, result);
    }

    @Test
    void getCommits_EmptyRepository_ReturnsEmptyList() {
        // Arrange
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/commits";
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Git Repository is empty", new HttpHeaders(),
                "{\"message\":\"Git Repository is empty\"}".getBytes(), null);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenThrow(exception);

        // Act
        Object result = gitHubIntegrationService.getCommits(OWNER, REPO, USER_ID, null, null, null, null);

        // Assert
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void getCommitDetails_Success_ReturnsCommitDetails() {
        // Arrange
        String sha = "abc123";
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/commits/" + sha;
        Object commitDetails = new Object();
        ResponseEntity<Object> response = new ResponseEntity<>(commitDetails, HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(response);

        // Act
        Object result = gitHubIntegrationService.getCommitDetails(OWNER, REPO, sha, USER_ID);

        // Assert
        assertEquals(commitDetails, result);
    }

    @Test
    void getCommitDetails_HttpError_ThrowsException() {
        // Arrange
        String sha = "abc123";
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/commits/" + sha;
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                gitHubIntegrationService.getCommitDetails(OWNER, REPO, sha, USER_ID));
        assertTrue(exception.getMessage().contains("Failed to fetch commit details"));
    }

    @Test
    void getBranches_Success_ReturnsBranches() {
        // Arrange
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/branches";
        List<Object> branches = List.of(new Object());
        ResponseEntity<Object> response = new ResponseEntity<>(branches, HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(response);

        // Act
        Object result = gitHubIntegrationService.getBranches(OWNER, REPO, USER_ID);

        // Assert
        assertEquals(branches, result);
    }

    @Test
    void getBranches_EmptyRepository_ReturnsEmptyList() {
        // Arrange
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/branches";
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Git Repository is empty", new HttpHeaders(),
                "{\"message\":\"Git Repository is empty\"}".getBytes(), null);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenThrow(exception);

        // Act
        Object result = gitHubIntegrationService.getBranches(OWNER, REPO, USER_ID);

        // Assert
        assertEquals(Collections.emptyList(), result);
    }
    @Test
    void getBranchDetails_Success_ReturnsBranchDetails() {
        // Arrange
        String branch = "main";
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/branches/" + branch;
        Object branchDetails = new Object();
        ResponseEntity<Object> response = new ResponseEntity<>(branchDetails, HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(response);

        // Act
        Object result = gitHubIntegrationService.getBranchDetails(OWNER, REPO, branch, USER_ID);

        // Assert
        assertEquals(branchDetails, result);
    }

    @Test
    void getPullRequests_Success_ReturnsPullRequests() {
        // Arrange
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/pulls";
        List<Object> pullRequests = List.of(new Object());
        ResponseEntity<Object> response = new ResponseEntity<>(pullRequests, HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(response);

        // Act
        Object result = gitHubIntegrationService.getPullRequests(OWNER, REPO, USER_ID, null);

        // Assert
        assertEquals(pullRequests, result);
    }

    @Test
    void getPullRequests_RepositoryNotFound_ThrowsException() {
        // Arrange
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/pulls";
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                gitHubIntegrationService.getPullRequests(OWNER, REPO, USER_ID, null));
        assertEquals("Repository not found", exception.getMessage());
    }

    @Test
    void getPullRequestCommits_Success_ReturnsCommits() {
        // Arrange
        String pullNumber = "1";
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/pulls/" + pullNumber + "/commits";
        List<Object> commits = List.of(new Object());
        ResponseEntity<Object> response = new ResponseEntity<>(commits, HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(response);

        // Act
        Object result = gitHubIntegrationService.getPullRequestCommits(OWNER, REPO, pullNumber, USER_ID);

        // Assert
        assertEquals(commits, result);
    }

    @Test
    void getPullRequestFiles_Success_ReturnsFiles() {
        // Arrange
        String pullNumber = "1";
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/pulls/" + pullNumber + "/files";
        List<Object> files = List.of(new Object());
        ResponseEntity<Object> response = new ResponseEntity<>(files, HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(response);

        // Act
        Object result = gitHubIntegrationService.getPullRequestFiles(OWNER, REPO, pullNumber, USER_ID);

        // Assert
        assertEquals(files, result);
    }

    @Test
    void getPullRequestReviews_Success_ReturnsReviews() {
        // Arrange
        String pullNumber = "1";
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/pulls/" + pullNumber + "/reviews";
        List<Object> reviews = List.of(new Object());
        ResponseEntity<Object> response = new ResponseEntity<>(reviews, HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(response);

        // Act
        Object result = gitHubIntegrationService.getPullRequestReviews(OWNER, REPO, pullNumber, USER_ID);

        // Assert
        assertEquals(reviews, result);
    }

    @Test
    void getPullRequestEvents_Success_ReturnsEvents() {
        // Arrange
        String pullNumber = "1";
        String url = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/issues/" + pullNumber + "/events";
        List<Object> events = List.of(new Object());
        ResponseEntity<Object> response = new ResponseEntity<>(events, HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(response);

        // Act
        Object result = gitHubIntegrationService.getPullRequestEvents(OWNER, REPO, pullNumber, USER_ID);

        // Assert
        assertEquals(events, result);
    }

    @Test
    void getAuthenticatedUser_Success_ReturnsUser() {
        // Arrange
        String url = "https://api.github.com/user";
        Object user = new Object();
        ResponseEntity<Object> response = new ResponseEntity<>(user, HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(response);

        // Act
        Object result = gitHubIntegrationService.getAuthenticatedUser(USER_ID);

        // Assert
        assertEquals(user, result);
    }

    @Test
    void getAuthenticatedUser_NoAccessToken_ThrowsException() {
        // Arrange
        when(githubTokenService.getAccessTokenByUserId(USER_ID)).thenReturn(null);

        // ActSecondo & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                gitHubIntegrationService.getAuthenticatedUser(USER_ID));
        assertEquals("Unexpected error fetching user: No access token found for user", exception.getMessage());
    }
}