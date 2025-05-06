package com.githubintegration.githubintegrationservice.Controller;

import com.githubintegration.githubintegrationservice.Service.GitHubIntegrationService;
import com.githubintegration.githubintegrationservice.Service.GithubTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/fetch_data")
@RequiredArgsConstructor
public class FetchRepoData {
    private static final Logger LOGGER = Logger.getLogger(OAuthController.class.getName());
    @Autowired
   public GithubTokenService githubTokenService;

    @Autowired
    public RestTemplate restTemplate;

    @Autowired
    public GitHubIntegrationService gitHubIntegrationService;
    @GetMapping("/repos/{owner}/{repo}/exists")
    public ResponseEntity<Map<String, Boolean>> checkRepositoryExists(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam String userId) {
        LOGGER.info("Vérification du dépôt GitHub : " + owner + "/" + repo + " pour l'utilisateur ID : " + userId);

        try {
            String accessToken = githubTokenService.getAccessTokenByUserId(userId);
            if (accessToken == null || accessToken.trim().isEmpty()) {
                LOGGER.warning("Aucun token d'accès trouvé pour l'utilisateur ID : " + userId);
                return ResponseEntity.badRequest().body(Map.of("exists", false));
            }
            LOGGER.info("Token d'accès récupéré pour l'utilisateur ID : " + userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://api.github.com/repos/" + owner + "/" + repo;
            LOGGER.info("Envoi de la requête à GitHub : " + url);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Object.class);

            LOGGER.info("Réponse de GitHub pour " + owner + "/" + repo + " : Code HTTP " + response.getStatusCode());
            return ResponseEntity.ok(Map.of("exists", response.getStatusCode().is2xxSuccessful()));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                LOGGER.warning("Le dépôt " + owner + "/" + repo + " n'existe pas sur GitHub (HTTP 404)");
            } else if (e.getStatusCode().value() == 403) {
                LOGGER.warning("Accès refusé au dépôt " + owner + "/" + repo + " (HTTP 403) - Vérifiez les permissions du token");
            } else {
                LOGGER.warning("Erreur HTTP lors de la vérification du dépôt " + owner + "/" + repo + " : " + e.getStatusCode() + " - " + e.getMessage());
            }
            return ResponseEntity.ok(Map.of("exists", false));
        } catch (Exception e) {
            LOGGER.severe("Erreur inattendue lors de la vérification du dépôt " + owner + "/" + repo + " : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("exists", false));
        }
    }
    @GetMapping("/repos/{owner}/{repo}/commits")
    public ResponseEntity<Object> getCommits(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestHeader("Authorization") String authorization) {
        LOGGER.info("Fetching commits for: " + owner + "/" + repo);
        try {
            if (gitHubIntegrationService == null) {
                LOGGER.severe("GitHubIntegrationService is null in getCommits");
                throw new IllegalStateException("GitHubIntegrationService is not initialized");
            }
            String userId = gitHubIntegrationService.extractUserId(authorization);
            Object commits = gitHubIntegrationService.getCommits(owner, repo, userId);
            LOGGER.info("Commits fetched successfully: " + commits);
            return ResponseEntity.ok(commits);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.warning("Error fetching commits: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to fetch commits: " + e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching commits: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error fetching commits: " + e.getMessage()));
        }
    }

    @GetMapping("/repos/{owner}/{repo}/branches")
    public ResponseEntity<Object> getBranches(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestHeader("Authorization") String authorization) {
        LOGGER.info("Fetching branches for: " + owner + "/" + repo);
        try {
            String userId = gitHubIntegrationService.extractUserId(authorization);
            Object branches = gitHubIntegrationService.getBranches(owner, repo, userId);
            LOGGER.info("Branches fetched successfully: " + branches);
            return ResponseEntity.ok(branches);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.warning("Error fetching branches: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to fetch branches: " + e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching branches: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error fetching branches: " + e.getMessage()));
        }
    }
    @GetMapping("/user")
    public ResponseEntity<Object> getAuthenticatedUser(@RequestParam String userId) {
        String accessToken = githubTokenService.getAccessTokenByUserId(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                "https://api.github.com/user", HttpMethod.GET, entity, Object.class);

        return ResponseEntity.ok(response.getBody());
    }

}
