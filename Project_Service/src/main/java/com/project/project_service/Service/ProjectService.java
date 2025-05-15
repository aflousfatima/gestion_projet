package com.project.project_service.Service;

import com.project.project_service.Controller.ProjectController;
import com.project.project_service.DTO.*;
import com.project.project_service.Entity.Client;
import com.project.project_service.Entity.Entreprise;
import com.project.project_service.Entity.GitHubLink;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Enumeration.PhaseProjet;
import com.project.project_service.Enumeration.PriorityProjet;
import com.project.project_service.Enumeration.StatusProjet;
import com.project.project_service.Repository.ClientRepository;
import com.project.project_service.Repository.GitHubLinkRepository;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.config.AuthClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.logging.Logger;
@Service
public class ProjectService {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UserStoryService.class);
    @Autowired
    private ClientRepository clientRepository;
    private static final Logger LOGGER = Logger.getLogger(ProjectController.class.getName());
    @Autowired
    private ProjetRepository projectRepository;
    @Autowired
    private AuthClient authClient; // Inject the Feign clien
    @Autowired
    private GitHubLinkRepository gitHubLinkRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Transactional
    @RateLimiter(name = "ProjectServiceLimiter", fallbackMethod = "createProjectRateLimiterFallback")
    @Bulkhead(name = "ProjectServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "createProjectBulkheadFallback")
    @Retry(name = "ProjectServiceRetry", fallbackMethod = "createProjectRetryFallback")
    public void createProject(String authId, String name, String description,
                              LocalDate startDate, LocalDate deadline,
                              String status, String phase, String priority) {
        // Récupérer le manager à partir de l'authId
        Client manager = clientRepository.findByAuthId(authId);
        if (manager == null) {
            throw new RuntimeException("Manager non trouvé");
        }

        // Récupérer l'entreprise associée au manager
        Entreprise company = manager.getCompany();
        if (company == null) {
            throw new RuntimeException("Aucune entreprise associée au manager");
        }

        // Créer le projet
        Projet project = new Projet();
        project.setName(name);
        project.setDescription(description);
        project.setCompany(company); // Lier le projet à l'entreprise
        project.setManager(manager); // Lier le manager au projet
        project.setCreationDate(LocalDate.now()); // Ajouter la date de création
        project.setStartDate(startDate);
        project.setDeadline(deadline);
        project.setStatus(StatusProjet.valueOf(status)); // Convertir le String en Enum
        project.setPhase(PhaseProjet.valueOf(phase));    // Convertir le String en Enum
        project.setPriority(PriorityProjet.valueOf(priority));  // Convertir le String en Enum

        projectRepository.save(project);

        System.out.println("Projet créé avec succès : " + project.getName());
    }


    public void createProjectRateLimiterFallback(String authId, String name, String description,
                                                 LocalDate startDate, LocalDate deadline,
                                                 String status, String phase, String priority, Throwable t) {
        System.out.println("RateLimiter fallback for createProject: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for project creation");
    }

    public void createProjectBulkheadFallback(String authId, String name, String description,
                                              LocalDate startDate, LocalDate deadline,
                                              String status, String phase, String priority, Throwable t) {
        System.out.println("Bulkhead fallback for createProject: " + t.getMessage());
        throw new RuntimeException("Too many concurrent project creation requests");
    }

    public void createProjectRetryFallback(String authId, String name, String description,
                                           LocalDate startDate, LocalDate deadline,
                                           String status, String phase, String priority, Throwable t) {
        System.out.println("Retry fallback for createProject: " + t.getMessage());
        throw new RuntimeException("Failed to create project after retries");
    }

    @RateLimiter(name = "ProjectServiceLimiter", fallbackMethod = "updateProjectRateLimiterFallback")
    @Bulkhead(name = "ProjectServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "updateProjectBulkheadFallback")
    @Retry(name = "ProjectServiceRetry", fallbackMethod = "updateProjectRetryFallback")
    @Transactional
    public void updateProject(String authId, String oldName, String newName,
                              String description, LocalDate startDate,
                              LocalDate deadline, String status,
                              String phase, String priority) {
        // Récupérer le manager à partir de l'authId
        Client manager = clientRepository.findByAuthId(authId);
        if (manager == null) {
            throw new RuntimeException("Manager non trouvé");
        }

        // Récupérer l'entreprise associée au manager
        Entreprise company = manager.getCompany();
        if (company == null) {
            throw new RuntimeException("Aucune entreprise associée au manager");
        }

        // Récupérer le projet à modifier
        Projet project = projectRepository.findByNameAndCompany(oldName, company);
        if (project == null) {
            throw new RuntimeException("Projet non trouvé ou vous n'avez pas les droits pour le modifier");
        }

        // Mettre à jour les champs du projet
        project.setName(newName);
        project.setDescription(description);
        project.setStartDate(startDate);
        project.setDeadline(deadline);
        project.setStatus(StatusProjet.valueOf(status)); // Convertir le String en Enum
        project.setPhase(PhaseProjet.valueOf(phase));    // Convertir le String en Enum
        project.setPriority(PriorityProjet.valueOf(priority));  // Convertir le String en Enum

        // Optionnel : Tu peux ajouter des mises à jour supplémentaires si nécessaire, par exemple, le manager
        project.setManager(manager);

        projectRepository.save(project);

        System.out.println("Projet modifié avec succès : " + project.getName());
    }
    public void updateProjectRateLimiterFallback(String authId, String oldName, String newName,
                                                 String description, LocalDate startDate,
                                                 LocalDate deadline, String status,
                                                 String phase, String priority, Throwable t) {
        System.out.println("RateLimiter fallback for updateProject: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for project update");
    }

    public void updateProjectBulkheadFallback(String authId, String oldName, String newName,
                                              String description, LocalDate startDate,
                                              LocalDate deadline, String status,
                                              String phase, String priority, Throwable t) {
        System.out.println("Bulkhead fallback for updateProject: " + t.getMessage());
        throw new RuntimeException("Too many concurrent project update requests");
    }

    public void updateProjectRetryFallback(String authId, String oldName, String newName,
                                           String description, LocalDate startDate,
                                           LocalDate deadline, String status,
                                           String phase, String priority, Throwable t) {
        System.out.println("Retry fallback for updateProject: " + t.getMessage());
        throw new RuntimeException("Failed to update project after retries");
    }

    @RateLimiter(name = "ProjectServiceLimiter", fallbackMethod = "deleteProjectRateLimiterFallback")
    @Bulkhead(name = "ProjectServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "deleteProjectBulkheadFallback")
    @Retry(name = "ProjectServiceRetry", fallbackMethod = "deleteProjectRetryFallback")
    @Transactional
    public void deleteProject(String authId, String name) {
        // Récupérer le manager à partir de l'authId
        Client manager = clientRepository.findByAuthId(authId);
        if (manager == null) {
            throw new RuntimeException("Manager non trouvé");
        }

        // Récupérer l'entreprise associée au manager
        Entreprise company = manager.getCompany();
        if (company == null) {
            throw new RuntimeException("Aucune entreprise associée au manager");
        }

        // Récupérer le projet à supprimer
        Projet project = projectRepository.findByNameAndCompany(name, company);
        if (project == null) {
            throw new RuntimeException("Projet non trouvé ou vous n'avez pas les droits pour le supprimer");
        }

        // Supprimer le projet
        projectRepository.delete(project);

        System.out.println("Projet supprimé avec succès : " + name);
    }
    public void deleteProjectRateLimiterFallback(String authId, String name, Throwable t) {
        System.out.println("RateLimiter fallback for deleteProject: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for project deletion");
    }

    public void deleteProjectBulkheadFallback(String authId, String name, Throwable t) {
        System.out.println("Bulkhead fallback for deleteProject: " + t.getMessage());
        throw new RuntimeException("Too many concurrent project deletion requests");
    }

    public void deleteProjectRetryFallback(String authId, String name, Throwable t) {
        System.out.println("Retry fallback for deleteProject: " + t.getMessage());
        throw new RuntimeException("Failed to delete project after retries");
    }

    @RateLimiter(name = "ProjectServiceLimiter", fallbackMethod = "getProjectByIdRateLimiterFallback")
    @Bulkhead(name = "ProjectServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "getProjectByIdBulkheadFallback")
    @Retry(name = "ProjectServiceRetry", fallbackMethod = "getProjectByIdRetryFallback")
    // Nouvelle méthode pour récupérer un projet par ID
    public Projet getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID : " + projectId));
    }
    public Projet getProjectByIdRateLimiterFallback(Long projectId, Throwable t) {
        System.out.println("RateLimiter fallback for getProjectById: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for fetching project");
    }

    public Projet getProjectByIdBulkheadFallback(Long projectId, Throwable t) {
        System.out.println("Bulkhead fallback for getProjectById: " + t.getMessage());
        throw new RuntimeException("Too many concurrent project fetch requests");
    }

    public Projet getProjectByIdRetryFallback(Long projectId, Throwable t) {
        System.out.println("Retry fallback for getProjectById: " + t.getMessage());
        throw new RuntimeException("Failed to fetch project after retries");
    }

    @RateLimiter(name = "ProjectServiceLimiter", fallbackMethod = "getManagerByProjectRateLimiterFallback")
    @Bulkhead(name = "ProjectServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "getManagerByProjectBulkheadFallback")
    @Retry(name = "ProjectServiceRetry", fallbackMethod = "getManagerByProjectRetryFallback")
    public Map<String, Object> getManagerByProject(String accessToken, Long projectId) {
        // Récupérer le projet pour obtenir le manager_id
        Projet project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));
        System.out.println("✅ Projet trouvé: " + project.getName());

        Client manager = project.getManager();
        if (manager == null) {
            System.out.println("❌ Aucun manager associé au projet: " + projectId);
            return null; // Return null instead of throwing an exception
        }
        System.out.println("✅ Manager trouvé: " + manager.getId());

        // Récupérer l'authId du manager
        String managerAuthId = manager.getAuthId();
        if (managerAuthId == null) {
            System.out.println("❌ authId manquant pour le manager: " + manager.getId());
            return null; // Return null instead of throwing an exception
        }

        // Appeler le microservice Authentication via Feign pour récupérer les détails du manager
        try {
            Map<String, Object> userInfo = authClient.getUserDetailsByAuthId(
                    managerAuthId,
                    "Bearer " + accessToken
            );

            if (userInfo == null) {
                System.out.println("⚠️ Manager non trouvé via le microservice Authentication : " + managerAuthId);
                return null;
            }

            // Construire un objet JSON pour le manager
            Map<String, Object> managerInfo = new HashMap<>();
            managerInfo.put("id", manager.getId().toString());
            managerInfo.put("authId", managerAuthId); // Include the authId
            managerInfo.put("firstName", userInfo.get("firstName"));
            managerInfo.put("lastName", userInfo.get("lastName"));
            managerInfo.put("role", manager.getRole() != null ? manager.getRole() : "Manager");
            managerInfo.put("avatar", userInfo.get("avatar"));

            return managerInfo;
        } catch (Exception e) {
            System.out.println("❌ Erreur lors de la récupération du manager " +
                    managerAuthId + " depuis le microservice Authentication : " + e.getMessage());
            return null;
        }
    }
    public Map<String, Object> getManagerByProjectRateLimiterFallback(String accessToken, Long projectId, Throwable t) {
        System.out.println("RateLimiter fallback for getManagerByProject: " + t.getMessage());
        return null;
    }

    public Map<String, Object> getManagerByProjectBulkheadFallback(String accessToken, Long projectId, Throwable t) {
        System.out.println("Bulkhead fallback for getManagerByProject: " + t.getMessage());
        return null;
    }

    public Map<String, Object> getManagerByProjectRetryFallback(String accessToken, Long projectId, Throwable t) {
        System.out.println("Retry fallback for getManagerByProject: " + t.getMessage());
        return null;
    }

    public ProjectDTO getProjectDetails(Long projectId, String accessToken) {
        Projet project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));

        ManagerDTO managerDTO = null;
        Map<String, Object> managerInfo = getManagerByProject(accessToken, projectId);
        if (managerInfo != null) {
            managerDTO = new ManagerDTO(
                    Long.valueOf((String) managerInfo.get("id")),
                    (String) managerInfo.get("authId"),
                    (String) managerInfo.get("firstName"),
                    (String) managerInfo.get("lastName"),
                    (String) managerInfo.get("role")
            );
        }

        return new ProjectDTO(
                project.getId(),
                project.getName(),
                project.getDescription(),
                managerDTO,
                project.getCreationDate(),
                project.getStartDate(),
                project.getDeadline(),
                project.getStatus() != null ? project.getStatus().toString() : null,
                project.getPhase() != null ? project.getPhase().toString() : null,
                project.getPriority() != null ? project.getPriority().toString() : null
        );
    }


    public ProjectResponseWithRoleDTO getProjectsByUser(String authId) {
        try {
            System.out.println("🔍 Récupération des projets pour l'utilisateur avec authId: " + authId);

            // Étape 1 : Vérifier si l'utilisateur est un manager
            Client manager = clientRepository.findByAuthId(authId);
            Entreprise company = null;
            List<Projet> managerProjects = new ArrayList<>();

            if (manager != null) {
                company = manager.getCompany();
                if (company != null) {
                    managerProjects = projectRepository.findByCompany(company);
                    System.out.println("✅ Projets du manager trouvés: " + managerProjects.size());
                } else {
                    System.out.println("❌ Aucune entreprise associée au manager: " + authId);
                }
            } else {
                System.out.println("❌ Manager non trouvé pour l'authId: " + authId);
            }

            // Étape 2 : Récupérer les projets où l'utilisateur est membre via Authentification
            List<ProjectMemberDTO> projectMembers = authClient.getProjectMembersByUserId(authId);
            List<Projet> memberProjects = new ArrayList<>();
            Map<Long, String> roleMap = projectMembers.stream()
                    .collect(Collectors.toMap(ProjectMemberDTO::getProjectId, ProjectMemberDTO::getRoleInProject));
            System.out.println("🔍 roleMap créé: " + roleMap);

            if (!projectMembers.isEmpty()) {
                List<Long> projectIds = projectMembers.stream()
                        .map(ProjectMemberDTO::getProjectId)
                        .collect(Collectors.toList());
                memberProjects = projectRepository.findAllById(projectIds);
                System.out.println("✅ Projets du membre trouvés: " + memberProjects.size());
            } else {
                System.out.println("ℹ️ Aucun projet trouvé pour le membre avec authId: " + authId);
            }

            // Étape 3 : Combiner les projets (manager + membre) et éviter les doublons
            Set<Projet> allProjects = new HashSet<>();
            allProjects.addAll(managerProjects);
            allProjects.addAll(memberProjects);
            System.out.println("🔍 Projets combinés (allProjects): " + allProjects.size() +
                    ", IDs: " + allProjects.stream().map(p -> p.getId().toString()).collect(Collectors.joining(", ")));

            // Étape 4 : Convertir les projets en DTO avec rôle
            List<ProjectWithRoleDTO> projectDTOs = allProjects.stream()
                    .map(project -> {
                        String role = roleMap.getOrDefault(project.getId(), "Manager");
                        System.out.println("🔍 Projet ID: " + project.getId() + ", Nom: " + project.getName() +
                                ", Rôle attribué: " + role);
                        return new ProjectWithRoleDTO(
                                project.getId(),
                                project.getName(),
                                project.getDescription(),
                                role,
                                project.getCreationDate().atStartOfDay(),
                                project.getStartDate().atStartOfDay(),
                                project.getDeadline().atStartOfDay(),
                                project.getStatus().name(),
                                project.getPhase().name(),
                                project.getPriority().name()
                        );
                    })
                    .collect(Collectors.toList());

            // Étape 5 : Construire la réponse
            String companyName = (company != null) ? company.getName() : "N/A";
            System.out.println("✅ Projets totaux trouvés: " + projectDTOs.size());
            return new ProjectResponseWithRoleDTO(companyName, projectDTOs);
        } catch (Exception e) {
            System.err.println("❌ Erreur dans getProjectsByUser pour authId: " + authId +
                    ", Message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des projets: " + e.getMessage());
        }
    }


    @Transactional
    public void linkGitHubRepositoryToProject(Long projectId, String repositoryUrl, String authorization) {
        LOGGER.info("Tentative de liaison du dépôt GitHub pour le projet ID: " + projectId + ", URL: " + repositoryUrl);

        // Extraire userId depuis le token
        String userId = authClient.decodeToken(authorization);
        if (userId == null || userId.trim().isEmpty()) {
            LOGGER.warning("userId null ou vide après décodage du token");
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }

        // Valider l'URL du dépôt
        if (!isValidGitHubRepositoryUrl(repositoryUrl)) {
            LOGGER.warning("URL du dépôt GitHub invalide: " + repositoryUrl);
            throw new IllegalArgumentException("L'URL du dépôt GitHub est invalide");
        }

        // Extraire owner et repo de l'URL
        String[] repoDetails = extractOwnerAndRepo(repositoryUrl);
        String owner = repoDetails[0];
        String repo = repoDetails[1];

        // Vérifier si le dépôt existe via githubintegrationservice
        String githubServiceUrl = "http://localhost:8087/fetch_data/repos/" + owner + "/" + repo + "/exists?userId=" + userId;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(githubServiceUrl, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Boolean exists = (Boolean) response.getBody().get("exists");
                if (exists == null || !exists) {
                    LOGGER.warning("Le dépôt " + owner + "/" + repo + " n'existe pas ou n'est pas accessible");
                    throw new IllegalArgumentException("Le dépôt GitHub n'existe pas ou n'est pas accessible. Vérifiez l'URL ou les permissions du token.");
                }
            } else {
                LOGGER.warning("Réponse invalide du service GitHub: " + response.getStatusCode());
                throw new IllegalArgumentException("Erreur lors de la vérification du dépôt: réponse invalide du service GitHub.");
            }
        } catch (HttpClientErrorException e) {
            LOGGER.severe("Erreur HTTP lors de la vérification du dépôt: " + e.getMessage());
            throw new IllegalArgumentException("Erreur lors de la vérification du dépôt: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("Erreur inattendue lors de la vérification du dépôt: " + e.getMessage());
            throw new IllegalArgumentException("Erreur serveur lors de la vérification du dépôt: " + e.getMessage());
        }

        // Vérifier si le projet existe
        Projet project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));

        // Créer ou mettre à jour le lien GitHub
        GitHubLink existingLink = gitHubLinkRepository.findByProjetId(projectId);
        if (existingLink != null) {
            existingLink.setRepositoryUrl(repositoryUrl);
            gitHubLinkRepository.save(existingLink);
            LOGGER.info("🔄 Lien GitHub mis à jour : " + repositoryUrl);
        } else {
            GitHubLink link = new GitHubLink(repositoryUrl, project , userId);
            gitHubLinkRepository.save(link);
            LOGGER.info("🔗 Dépôt GitHub lié au projet : " + repositoryUrl);
        }
    }


    @RateLimiter(name = "ProjectServiceLimiter", fallbackMethod = "getGitHubRepositoryUrlRateLimiterFallback")
    @Bulkhead(name = "ProjectServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "getGitHubRepositoryUrlBulkheadFallback")
    @Retry(name = "ProjectServiceRetry", fallbackMethod = "getGitHubRepositoryUrlRetryFallback")
    public String getGitHubRepositoryUrl(Long projectId) {
        GitHubLink link = gitHubLinkRepository.findByProjetId(projectId);
        return (link != null) ? link.getRepositoryUrl() : null;
    }
    public String getGitHubRepositoryUrlRateLimiterFallback(Long projectId, Throwable t) {
        System.out.println("RateLimiter fallback for getGitHubRepositoryUrl: " + t.getMessage());
        return null;
    }

    public String getGitHubRepositoryUrlBulkheadFallback(Long projectId, Throwable t) {
        System.out.println("Bulkhead fallback for getGitHubRepositoryUrl: " + t.getMessage());
        return null;
    }

    public String getGitHubRepositoryUrlRetryFallback(Long projectId, Throwable t) {
        System.out.println("Retry fallback for getGitHubRepositoryUrl: " + t.getMessage());
        return null;
    }
    private boolean isValidGitHubRepositoryUrl(String url) {
        String regex = "^https://github\\.com/([a-zA-Z0-9-]+)/([a-zA-Z0-9-_]+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url.trim());
        return matcher.matches();
    }

    private String[] extractOwnerAndRepo(String url) {
        String regex = "^https://github\\.com/([a-zA-Z0-9-]+)/([a-zA-Z0-9-_]+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url.trim());
        if (matcher.matches()) {
            return new String[]{matcher.group(1), matcher.group(2)};
        }
        throw new IllegalArgumentException("Impossible d'extraire owner et repo depuis l'URL: " + url);
    }

    public Map<String, String> getGitHubUserId(Long projectId) {
        Optional<GitHubLink> gitHubLink = gitHubLinkRepository.findByProjectId(projectId);
        if (gitHubLink.isPresent()) {
            LOGGER.info("Found GitHub userId for project ID: " + projectId);
            return Map.of("userId", gitHubLink.get().getUserId());
        }
        LOGGER.info("No GitHub link found for project ID: " + projectId);
        return Map.of();
    }

    public List<Long> getActiveProjectIds() {
        return projectRepository.findAll()
                .stream()
                .map(Projet::getId)
                .collect(Collectors.toList());
    }
}
