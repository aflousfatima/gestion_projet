package com.project.project_service.Service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProjetRepository projectRepository;
    @Autowired
    private AuthClient authClient; // Inject the Feign clien
    @Autowired
    private GitHubLinkRepository gitHubLinkRepository;
    @Transactional
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

    // Nouvelle méthode pour récupérer un projet par ID
    public Projet getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID : " + projectId));
    }

    // In ProjectService (Project Microservice)
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
    public void linkGitHubRepositoryToProject(Long projectId, String repositoryUrl) {
        Projet project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));

        GitHubLink existingLink = gitHubLinkRepository.findByProjetId(projectId);
        if (existingLink != null) {
            existingLink.setRepositoryUrl(repositoryUrl);
            gitHubLinkRepository.save(existingLink);
            System.out.println("🔄 Lien GitHub mis à jour : " + repositoryUrl);
        } else {
            GitHubLink link = new GitHubLink(repositoryUrl, project);
            gitHubLinkRepository.save(link);
            System.out.println("🔗 Dépôt GitHub lié au projet : " + repositoryUrl);
        }
    }
    public String getGitHubRepositoryUrl(Long projectId) {
        GitHubLink link = gitHubLinkRepository.findByProjetId(projectId);
        return (link != null) ? link.getRepositoryUrl() : null;
    }

}
