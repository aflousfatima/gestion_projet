package com.githubintegration.githubintegrationservice.unit.Controller;

import com.githubintegration.githubintegrationservice.Controller.FetchRepoData;
import com.githubintegration.githubintegrationservice.Service.GitHubIntegrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FetchRepoDataTest {

    @Mock
    private GitHubIntegrationService gitHubIntegrationService;

    @InjectMocks
    private FetchRepoData fetchRepoData;

    private static final String OWNER = "test-owner";
    private static final String REPO = "test-repo";
    private static final String USER_ID = "user123";
    private static final String AUTHORIZATION = "Bearer valid-token";

    @Test
    void checkRepositoryExists_RepositoryExists_ReturnsTrue() {
        // Arrange
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.checkRepositoryExists(OWNER, REPO, USER_ID)).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Boolean>> response = fetchRepoData.checkRepositoryExists(OWNER, REPO, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("exists", true), response.getBody());
    }

    @Test
    void checkRepositoryExists_InvalidToken_ReturnsBadRequest() {
        // Arrange
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        // Act
        ResponseEntity<Map<String, Boolean>> response = fetchRepoData.checkRepositoryExists(OWNER, REPO, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("exists", false), response.getBody());
    }

    @Test
    void getCommits_Success_ReturnsCommits() {
        // Arrange
        List<Object> commits = List.of(new Object());
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getCommits(eq(OWNER), eq(REPO), eq(USER_ID), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(commits);

        // Act
        ResponseEntity<Object> response = fetchRepoData.getCommits(OWNER, REPO, AUTHORIZATION, null, null, null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(commits, response.getBody());
    }

    @Test
    void getCommits_InvalidToken_ReturnsBadRequest() {
        // Arrange
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        // Act
        ResponseEntity<Object> response = fetchRepoData.getCommits(OWNER, REPO, AUTHORIZATION, null, null, null, null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Invalid token"), response.getBody());
    }

    @Test
    void getCommitsByUserId_Success_ReturnsCommits() {
        // Arrange
        List<Object> commits = List.of(new Object());
        when(gitHubIntegrationService.getCommits(eq(OWNER), eq(REPO), eq(USER_ID), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(commits);

        // Act
        ResponseEntity<Object> response = fetchRepoData.getCommitsByUserId(OWNER, REPO, USER_ID, null, null, null, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(commits, response.getBody());
    }

    @Test
    void getCommitsByUserId_RuntimeException_ReturnsBadRequest() {
        // Arrange
        when(gitHubIntegrationService.getCommits(eq(OWNER), eq(REPO), eq(USER_ID), isNull(), isNull(), isNull(), isNull()))
                .thenThrow(new RuntimeException("Failed to fetch commits"));

        // Act
        ResponseEntity<Object> response = fetchRepoData.getCommitsByUserId(OWNER, REPO, USER_ID, null, null, null, null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Failed to fetch commits: Failed to fetch commits"), response.getBody());
    }

    @Test
    void getCommitDetails_Success_ReturnsCommitDetails() {
        // Arrange
        String sha = "abc123";
        Object commitDetails = new Object();
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getCommitDetails(OWNER, REPO, sha, USER_ID)).thenReturn(commitDetails);

        // Act
        ResponseEntity<Object> response = fetchRepoData.getCommitDetails(OWNER, REPO, sha, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(commitDetails, response.getBody());
    }

    @Test
    void getCommitDetails_InvalidToken_ReturnsBadRequest() {
        // Arrange
        String sha = "abc123";
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        // Act
        ResponseEntity<Object> response = fetchRepoData.getCommitDetails(OWNER, REPO, sha, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Invalid token"), response.getBody());
    }

    @Test
    void getBranches_Success_ReturnsBranches() {
        // Arrange
        List<Object> branches = List.of(new Object());
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getBranches(OWNER, REPO, USER_ID)).thenReturn(branches);

        // Act
        ResponseEntity<Object> response = fetchRepoData.getBranches(OWNER, REPO, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(branches, response.getBody());
    }

    @Test
    void getBranches_RuntimeException_ReturnsBadRequest() {
        // Arrange
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getBranches(OWNER, REPO, USER_ID))
                .thenThrow(new RuntimeException("Failed to fetch branches"));

        // Act
        ResponseEntity<Object> response = fetchRepoData.getBranches(OWNER, REPO, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Failed to fetch branches: Failed to fetch branches"), response.getBody());
    }

    @Test
    void getBranchDetails_Success_ReturnsBranchDetails() {
        // Arrange
        String branch = "main";
        Object branchDetails = new Object();
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getBranchDetails(OWNER, REPO, branch, USER_ID)).thenReturn(branchDetails);

        // Act
        ResponseEntity<Object> response = fetchRepoData.getBranchDetails(OWNER, REPO, branch, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(branchDetails, response.getBody());
    }

    @Test
    void getBranchDetails_InvalidToken_ReturnsBadRequest() {
        // Arrange
        String branch = "main";
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        // Act
        ResponseEntity<Object> response = fetchRepoData.getBranchDetails(OWNER, REPO, branch, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Invalid token"), response.getBody());
    }

    @Test
    void getPullRequests_Success_ReturnsPullRequests() {
        // Arrange
        List<Object> pullRequests = List.of(new Object());
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getPullRequests(OWNER, REPO, USER_ID, "open")).thenReturn(pullRequests);

        // Act
        ResponseEntity<Object> response = fetchRepoData.getPullRequests(OWNER, REPO, AUTHORIZATION, "open");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pullRequests, response.getBody());
    }

    @Test
    void getPullRequests_InvalidToken_ReturnsBadRequest() {
        // Arrange
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        // Act
        ResponseEntity<Object> response = fetchRepoData.getPullRequests(OWNER, REPO, AUTHORIZATION, "open");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Invalid token"), response.getBody());
    }

    @Test
    void getPullRequestCommits_Success_ReturnsCommits() {
        // Arrange
        String pullNumber = "1";
        List<Object> commits = List.of(new Object());
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getPullRequestCommits(OWNER, REPO, pullNumber, USER_ID)).thenReturn(commits);

        // Act
        ResponseEntity<Object> response = fetchRepoData.getPullRequestCommits(OWNER, REPO, pullNumber, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(commits, response.getBody());
    }

    @Test
    void getPullRequestCommits_RuntimeException_ReturnsBadRequest() {
        // Arrange
        String pullNumber = "1";
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getPullRequestCommits(OWNER, REPO, pullNumber, USER_ID))
                .thenThrow(new RuntimeException("Failed to fetch pull request commits"));

        // Act
        ResponseEntity<Object> response = fetchRepoData.getPullRequestCommits(OWNER, REPO, pullNumber, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Failed to fetch pull request commits: Failed to fetch pull request commits"),
                response.getBody());
    }

    @Test
    void getPullRequestFiles_Success_ReturnsFiles() {
        // Arrange
        String pullNumber = "1";
        List<Object> files = List.of(new Object());
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getPullRequestFiles(OWNER, REPO, pullNumber, USER_ID)).thenReturn(files);

        // Act
        ResponseEntity<Object> response = fetchRepoData.getPullRequestFiles(OWNER, REPO, pullNumber, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(files, response.getBody());
    }

    @Test
    void getPullRequestFiles_InvalidToken_ReturnsBadRequest() {
        // Arrange
        String pullNumber = "1";
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        // Act
        ResponseEntity<Object> response = fetchRepoData.getPullRequestFiles(OWNER, REPO, pullNumber, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Invalid token"), response.getBody());
    }

    @Test
    void getPullRequestReviews_Success_ReturnsReviews() {
        // Arrange
        String pullNumber = "1";
        List<Object> reviews = List.of(new Object());
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getPullRequestReviews(OWNER, REPO, pullNumber, USER_ID)).thenReturn(reviews);

        // Act
        ResponseEntity<Object> response = fetchRepoData.getPullRequestReviews(OWNER, REPO, pullNumber, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(reviews, response.getBody());
    }

    @Test
    void getPullRequestReviews_RuntimeException_ReturnsBadRequest() {
        // Arrange
        String pullNumber = "1";
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getPullRequestReviews(OWNER, REPO, pullNumber, USER_ID))
                .thenThrow(new RuntimeException("Failed to fetch pull request reviews"));

        // Act
        ResponseEntity<Object> response = fetchRepoData.getPullRequestReviews(OWNER, REPO, pullNumber, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Failed to fetch pull request reviews: Failed to fetch pull request reviews"),
                response.getBody());
    }

    @Test
    void getPullRequestEvents_Success_ReturnsEvents() {
        // Arrange
        String pullNumber = "1";
        List<Object> events = List.of(new Object());
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getPullRequestEvents(OWNER, REPO, pullNumber, USER_ID)).thenReturn(events);

        // Act
        ResponseEntity<Object> response = fetchRepoData.getPullRequestEvents(OWNER, REPO, pullNumber, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(events, response.getBody());
    }

    @Test
    void getPullRequestEvents_InvalidToken_ReturnsBadRequest() {
        // Arrange
        String pullNumber = "1";
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        // Act
        ResponseEntity<Object> response = fetchRepoData.getPullRequestEvents(OWNER, REPO, pullNumber, AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Invalid token"), response.getBody());
    }

    @Test
    void getAuthenticatedUser_Success_ReturnsUser() {
        // Arrange
        Object user = new Object();
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getAuthenticatedUser(USER_ID)).thenReturn(user);

        // Act
        ResponseEntity<Object> response = fetchRepoData.getAuthenticatedUser(AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(user, response.getBody());
    }

    @Test
    void getAuthenticatedUser_RuntimeException_ReturnsBadRequest() {
        // Arrange
        when(gitHubIntegrationService.extractUserId(AUTHORIZATION)).thenReturn(USER_ID);
        when(gitHubIntegrationService.getAuthenticatedUser(USER_ID))
                .thenThrow(new RuntimeException("Failed to fetch user"));

        // Act
        ResponseEntity<Object> response = fetchRepoData.getAuthenticatedUser(AUTHORIZATION);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("error", "Failed to fetch user: Failed to fetch user"), response.getBody());
    }
}