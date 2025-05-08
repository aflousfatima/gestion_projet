package com.githubintegration.githubintegrationservice.Controller;

import com.githubintegration.githubintegrationservice.Service.GitHubIntegrationService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/fetch_data")
@OpenAPIDefinition(info = @Info(
        title = "API d'Intégration GitHub",
        version = "1.0",
        description = "Cette API permet de récupérer des données depuis GitHub, telles que les dépôts, commits, branches et pull requests."
),
        servers = @Server(
                url = "http://localhost:8087/"
        ))
public class FetchRepoData {
    private static final Logger LOGGER = Logger.getLogger(FetchRepoData.class.getName());
    private final GitHubIntegrationService gitHubIntegrationService;

    public FetchRepoData(GitHubIntegrationService gitHubIntegrationService) {
        this.gitHubIntegrationService = gitHubIntegrationService;
    }
    @Operation(summary = "Vérifier l'existence d'un dépôt",
            description = "Cette méthode permet de vérifier si un dépôt GitHub existe pour un propriétaire et un nom de dépôt donnés.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vérification effectuée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la vérification du dépôt")
    })
    @GetMapping("/repos/{owner}/{repo}/exists")
    public ResponseEntity<Map<String, Boolean>> checkRepositoryExists(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestHeader("Authorization") String authorization) {
        LOGGER.info("Checking repository: " + owner + "/" + repo);
        try {
            String userId = gitHubIntegrationService.extractUserId(authorization);
            boolean exists = gitHubIntegrationService.checkRepositoryExists(owner, repo, userId);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("exists", false));
        } catch (Exception e) {
            LOGGER.severe("Unexpected error checking repository: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("exists", false));
        }
    }

    @Operation(summary = "Récupérer les commits d'un dépôt",
            description = "Cette méthode permet de récupérer la liste des commits d'un dépôt GitHub, avec des filtres optionnels pour la branche, l'auteur, et les dates.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Commits récupérés avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des commits")
    })
    @GetMapping("/repos/{owner}/{repo}/commits")
    public ResponseEntity<Object> getCommits(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        LOGGER.info("Fetching commits for: " + owner + "/" + repo);
        try {
            String userId = gitHubIntegrationService.extractUserId(authorization);
            Object commits = gitHubIntegrationService.getCommits(owner, repo, userId, branch, author, since, until);
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

    @Operation(summary = "Récupérer les commits d'un dépôt par utilisateur",
            description = "Cette méthode permet de récupérer la liste des commits d'un dépôt GitHub pour un utilisateur spécifique, avec des filtres optionnels.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Commits récupérés avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des commits")
    })
    @GetMapping("/repos/{owner}/{repo}/commits-by-user")
    public ResponseEntity<Object> getCommitsByUserId(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam String userId,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        LOGGER.info("Fetching commits for: " + owner + "/" + repo + " with userId: " + userId);
        try {
            Object commits = gitHubIntegrationService.getCommits(owner, repo, userId, branch, author, since, until);
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
    @Operation(summary = "Récupérer les détails d'un commit",
            description = "Cette méthode permet de récupérer les détails d'un commit spécifique dans un dépôt GitHub en utilisant son SHA.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Détails du commit récupérés avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des détails du commit")
    })
    @GetMapping("/repos/{owner}/{repo}/commits/{sha}")
    public ResponseEntity<Object> getCommitDetails(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String sha,
            @RequestHeader("Authorization") String authorization) {
        LOGGER.info("Fetching commit details for SHA: " + sha + " in: " + owner + "/" + repo);
        try {
            String userId = gitHubIntegrationService.extractUserId(authorization);
            Object commitDetails = gitHubIntegrationService.getCommitDetails(owner, repo, sha, userId);
            LOGGER.info("Commit details fetched successfully: " + commitDetails);
            return ResponseEntity.ok(commitDetails);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.warning("Error fetching commit details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to fetch commit details: " + e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching commit details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error fetching commit details: " + e.getMessage()));
        }
    }

    @Operation(summary = "Récupérer les branches d'un dépôt",
            description = "Cette méthode permet de récupérer la liste des branches d'un dépôt GitHub.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Branches récupérées avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des branches")
    })
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
    @Operation(summary = "Récupérer les détails d'une branche",
            description = "Cette méthode permet de récupérer les détails d'une branche spécifique dans un dépôt GitHub.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Détails de la branche récupérés avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des détails de la branche")
    })
    @GetMapping("/repos/{owner}/{repo}/branches/{branch}")
    public ResponseEntity<Object> getBranchDetails(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String branch,
            @RequestHeader("Authorization") String authorization) {
        LOGGER.info("Fetching branch details for: " + branch + " in: " + owner + "/" + repo);
        try {
            String userId = gitHubIntegrationService.extractUserId(authorization);
            Object branchDetails = gitHubIntegrationService.getBranchDetails(owner, repo, branch, userId);
            LOGGER.info("Branch details fetched successfully: " + branchDetails);
            return ResponseEntity.ok(branchDetails);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.warning("Error fetching branch details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to fetch branch details: " + e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching branch details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error fetching branch details: " + e.getMessage()));
        }
    }
    @Operation(summary = "Récupérer les pull requests d'un dépôt",
            description = "Cette méthode permet de récupérer la liste des pull requests d'un dépôt GitHub, avec un état spécifié (ouvert, fermé, tous).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pull requests récupérées avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des pull requests")
    })
    @GetMapping("/repos/{owner}/{repo}/pulls")
    public ResponseEntity<Object> getPullRequests(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "open") String state) {
        LOGGER.info("Fetching pull requests for: " + owner + "/" + repo);
        try {
            String userId = gitHubIntegrationService.extractUserId(authorization);
            Object pullRequests = gitHubIntegrationService.getPullRequests(owner, repo, userId, state);
            LOGGER.info("Pull requests fetched successfully: " + pullRequests);
            return ResponseEntity.ok(pullRequests);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.warning("Error fetching pull requests: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to fetch pull requests: " + e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching pull requests: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error fetching pull requests: " + e.getMessage()));
        }
    }
    @Operation(summary = "Récupérer les commits d'une pull request",
            description = "Cette méthode permet de récupérer la liste des commits associés à une pull request spécifique dans un dépôt GitHub.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Commits de la pull request récupérés avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des commits de la pull request")
    })
    @GetMapping("/repos/{owner}/{repo}/pulls/{pullNumber}/commits")
    public ResponseEntity<Object> getPullRequestCommits(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String pullNumber,
            @RequestHeader("Authorization") String authorization) {
        LOGGER.info("Fetching commits for pull request #" + pullNumber + " in: " + owner + "/" + repo);
        try {
            String userId = gitHubIntegrationService.extractUserId(authorization);
            Object commits = gitHubIntegrationService.getPullRequestCommits(owner, repo, pullNumber, userId);
            LOGGER.info("Pull request commits fetched successfully: " + commits);
            return ResponseEntity.ok(commits);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.warning("Error fetching pull request commits: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to fetch pull request commits: " + e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching pull request commits: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error fetching pull request commits: " + e.getMessage()));
        }
    }
    @Operation(summary = "Récupérer les fichiers d'une pull request",
            description = "Cette méthode permet de récupérer la liste des fichiers modifiés dans une pull request spécifique dans un dépôt GitHub.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fichiers de la pull request récupérés avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des fichiers de la pull request")
    })
    @GetMapping("/repos/{owner}/{repo}/pulls/{pullNumber}/files")
    public ResponseEntity<Object> getPullRequestFiles(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String pullNumber,
            @RequestHeader("Authorization") String authorization) {
        LOGGER.info("Fetching files for pull request #" + pullNumber + " in: " + owner + "/" + repo);
        try {
            String userId = gitHubIntegrationService.extractUserId(authorization);
            Object files = gitHubIntegrationService.getPullRequestFiles(owner, repo, pullNumber, userId);
            LOGGER.info("Pull request files fetched successfully: " + files);
            return ResponseEntity.ok(files);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.warning("Error fetching pull request files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to fetch pull request files: " + e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching pull request files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error fetching pull request files: " + e.getMessage()));
        }
    }
    @Operation(summary = "Récupérer les revues d'une pull request",
            description = "Cette méthode permet de récupérer la liste des revues associées à une pull request spécifique dans un dépôt GitHub.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Revues de la pull request récupérées avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des revues de la pull request")
    })
    @GetMapping("/repos/{owner}/{repo}/pulls/{pullNumber}/reviews")
    public ResponseEntity<Object> getPullRequestReviews(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String pullNumber,
            @RequestHeader("Authorization") String authorization) {
        LOGGER.info("Fetching reviews for pull request #" + pullNumber + " in: " + owner + "/" + repo);
        try {
            String userId = gitHubIntegrationService.extractUserId(authorization);
            Object reviews = gitHubIntegrationService.getPullRequestReviews(owner, repo, pullNumber, userId);
            LOGGER.info("Pull request reviews fetched successfully: " + reviews);
            return ResponseEntity.ok(reviews);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.warning("Error fetching pull request reviews: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to fetch pull request reviews: " + e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching pull request reviews: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error fetching pull request reviews: " + e.getMessage()));
        }
    }
    @Operation(summary = "Récupérer les événements d'une pull request",
            description = "Cette méthode permet de récupérer la liste des événements associés à une pull request spécifique dans un dépôt GitHub.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Événements de la pull request récupérés avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des événements de la pull request")
    })
    @GetMapping("/repos/{owner}/{repo}/pulls/{pullNumber}/events")
    public ResponseEntity<Object> getPullRequestEvents(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String pullNumber,
            @RequestHeader("Authorization") String authorization) {
        LOGGER.info("Fetching events for pull request #" + pullNumber + " in: " + owner + "/" + repo);
        try {
            String userId = gitHubIntegrationService.extractUserId(authorization);
            Object events = gitHubIntegrationService.getPullRequestEvents(owner, repo, pullNumber, userId);
            LOGGER.info("Pull request events fetched successfully: " + events);
            return ResponseEntity.ok(events);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.warning("Error fetching pull request events: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to fetch pull request events: " + e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching pull request events: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error fetching pull request events: " + e.getMessage()));
        }
    }
    @Operation(summary = "Récupérer les informations de l'utilisateur authentifié",
            description = "Cette méthode permet de récupérer les informations de l'utilisateur authentifié via GitHub.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Informations de l'utilisateur récupérées avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des informations de l'utilisateur")
    })
    @GetMapping("/user")
    public ResponseEntity<Object> getAuthenticatedUser(
            @RequestHeader("Authorization") String authorization) {
        LOGGER.info("Fetching authenticated user");
        try {
            String userId = gitHubIntegrationService.extractUserId(authorization);
            Object user = gitHubIntegrationService.getAuthenticatedUser(userId);
            LOGGER.info("User fetched successfully: " + user);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.warning("Error fetching user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to fetch user: " + e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Unexpected error fetching user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error fetching user: " + e.getMessage()));
        }
    }
}