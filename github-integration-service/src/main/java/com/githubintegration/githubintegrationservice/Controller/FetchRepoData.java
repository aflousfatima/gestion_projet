package com.githubintegration.githubintegrationservice.Controller;

import com.githubintegration.githubintegrationservice.Service.GitHubIntegrationService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/fetch_data")
public class FetchRepoData {
    private static final Logger LOGGER = Logger.getLogger(FetchRepoData.class.getName());
    private final GitHubIntegrationService gitHubIntegrationService;

    public FetchRepoData(GitHubIntegrationService gitHubIntegrationService) {
        this.gitHubIntegrationService = gitHubIntegrationService;
    }

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