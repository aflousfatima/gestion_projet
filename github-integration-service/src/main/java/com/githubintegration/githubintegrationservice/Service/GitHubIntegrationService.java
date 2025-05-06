package com.githubintegration.githubintegrationservice.Service;

import com.githubintegration.githubintegrationservice.Config.AuthClient;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class GitHubIntegrationService {
    private static final Logger LOGGER = Logger.getLogger(GitHubIntegrationService.class.getName());

    private final AuthClient authClient;
    private final GithubTokenService githubTokenService;
    private final RestTemplate restTemplate;

    public GitHubIntegrationService(
            AuthClient authClient,
            GithubTokenService githubTokenService,
            RestTemplate restTemplate) {
        this.authClient = authClient;
        this.githubTokenService = githubTokenService;
        this.restTemplate = restTemplate;
    }

    public String extractUserId(String authorization) {
        LOGGER.info("Extracting userId from authorization header");
        try {
            String userId = authClient.decodeToken(authorization);
            if (userId == null || userId.trim().isEmpty()) {
                LOGGER.warning("userId null or empty after decoding token");
                throw new IllegalArgumentException("Invalid or missing userId in token");
            }
            LOGGER.info("Successfully extracted userId: " + userId);
            return userId;
        } catch (Exception e) {
            LOGGER.severe("Error decoding token: " + e.getMessage() + ", Cause: " + e.getCause());
            throw new IllegalArgumentException("Failed to decode token: " + e.getMessage(), e);
        }
    }

    public boolean checkRepositoryExists(String owner, String repo, String userId) {
        LOGGER.info("Checking GitHub repository: " + owner + "/" + repo + " for user ID: " + userId);
        try {
            String accessToken = githubTokenService.getAccessTokenByUserId(userId);
            if (accessToken == null || accessToken.trim().isEmpty()) {
                LOGGER.warning("No access token found for user ID: " + userId);
                throw new IllegalArgumentException("No access token found for user");
            }
            LOGGER.info("Access token retrieved for user ID: " + userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.github.com/repos/" + owner + "/" + repo;
            LOGGER.info("Sending request to GitHub: " + url);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Object.class);

            LOGGER.info("GitHub response for " + owner + "/" + repo + ": HTTP " + response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                LOGGER.warning("Repository " + owner + "/" + repo + " does not exist on GitHub (HTTP 404)");
            } else if (e.getStatusCode().value() == 403) {
                LOGGER.warning("Access denied to repository " + owner + "/" + repo + " (HTTP 403) - Check token permissions");
            } else {
                LOGGER.warning("HTTP error checking repository " + owner + "/" + repo + ": " + e.getStatusCode() + " - " + e.getMessage());
            }
            return false;
        } catch (Exception e) {
            LOGGER.severe("Unexpected error checking repository " + owner + "/" + repo + ": " + e.getMessage());
            throw new RuntimeException("Unexpected error checking repository: " + e.getMessage());
        }
    }

    public Object getCommits(String owner, String repo, String userId) {
        LOGGER.info("Fetching commits for repository: " + owner + "/" + repo + " for user ID: " + userId);
        try {
            String accessToken = githubTokenService.getAccessTokenByUserId(userId);
            if (accessToken == null || accessToken.trim().isEmpty()) {
                LOGGER.warning("No access token found for user ID: " + userId);
                throw new IllegalArgumentException("No access token found for user");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.github.com/repos/" + owner + "/" + repo + "/commits";
            LOGGER.info("Sending request to GitHub: " + url);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Object.class);

            LOGGER.info("GitHub response for commits: HTTP " + response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 409 && e.getResponseBodyAsString().contains("Git Repository is empty")) {
                LOGGER.info("Repository " + owner + "/" + repo + " is empty (HTTP 409)");
                return Collections.emptyList();
            }
            LOGGER.warning("HTTP error fetching commits for " + owner + "/" + repo + ": " + e.getStatusCode() + " - " + e.getMessage());
            throw new RuntimeException("Failed to fetch commits: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching commits for " + owner + "/" + repo + ": " + e.getMessage());
            throw new RuntimeException("Unexpected error fetching commits: " + e.getMessage());
        }
    }

    public Object getBranches(String owner, String repo, String userId) {
        LOGGER.info("Fetching branches for repository: " + owner + "/" + repo + " for user ID: " + userId);
        try {
            String accessToken = githubTokenService.getAccessTokenByUserId(userId);
            if (accessToken == null || accessToken.trim().isEmpty()) {
                LOGGER.warning("No access token found for user ID: " + userId);
                throw new IllegalArgumentException("No access token found for user");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.github.com/repos/" + owner + "/" + repo + "/branches";
            LOGGER.info("Sending request to GitHub: " + url);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Object.class);

            LOGGER.info("GitHub response for branches: HTTP " + response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 409 && e.getResponseBodyAsString().contains("Git Repository is empty")) {
                LOGGER.info("Repository " + owner + "/" + repo + " is empty (HTTP 409)");
                return Collections.emptyList();
            }
            LOGGER.warning("HTTP error fetching branches for " + owner + "/" + repo + ": " + e.getStatusCode() + " - " + e.getMessage());
            throw new RuntimeException("Failed to fetch branches: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching branches for " + owner + "/" + repo + ": " + e.getMessage());
            throw new RuntimeException("Unexpected error fetching branches: " + e.getMessage());
        }
    }

    public Object getPullRequests(String owner, String repo, String userId) {
        LOGGER.info("Fetching pull requests for repository: " + owner + "/" + repo + " for user ID: " + userId);
        try {
            String accessToken = githubTokenService.getAccessTokenByUserId(userId);
            if (accessToken == null || accessToken.trim().isEmpty()) {
                LOGGER.warning("No access token found for user ID: " + userId);
                throw new IllegalArgumentException("No access token found for user");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.github.com/repos/" + owner + "/" + repo + "/pulls";
            LOGGER.info("Sending request to GitHub: " + url);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Object.class);

            LOGGER.info("GitHub response for pull requests: HTTP " + response.getStatusCode());
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                LOGGER.warning("Repository " + owner + "/" + repo + " not found (HTTP 404)");
                throw new IllegalArgumentException("Repository not found");
            } else if (e.getStatusCode().value() == 403) {
                LOGGER.warning("Access denied to repository " + owner + "/" + repo + " (HTTP 403) - Check token permissions");
                throw new IllegalArgumentException("Access denied to repository");
            }
            LOGGER.warning("HTTP error fetching pull requests for " + owner + "/" + repo + ": " + e.getStatusCode() + " - " + e.getMessage());
            throw new RuntimeException("Failed to fetch pull requests: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching pull requests for " + owner + "/" + repo + ": " + e.getMessage());
            throw new RuntimeException("Unexpected error fetching pull requests: " + e.getMessage());
        }
    }

    public Object getAuthenticatedUser(String userId) {
        LOGGER.info("Fetching authenticated user for user ID: " + userId);
        try {
            String accessToken = githubTokenService.getAccessTokenByUserId(userId);
            if (accessToken == null || accessToken.trim().isEmpty()) {
                LOGGER.warning("No access token found for user ID: " + userId);
                throw new IllegalArgumentException("No access token found for user");
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Object> response = restTemplate.exchange(
                    "https://api.github.com/user", HttpMethod.GET, entity, Object.class);
            LOGGER.info("GitHub response for user: HTTP " + response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            LOGGER.warning("HTTP error fetching user: " + e.getStatusCode() + " - " + e.getMessage());
            throw new RuntimeException("Failed to fetch user: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching user: " + e.getMessage());
            throw new RuntimeException("Unexpected error fetching user: " + e.getMessage());
        }
    }
}