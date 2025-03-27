package com.project.project_service.Service;

import com.project.project_service.DTO.InitialProjectManagementDTO;
import com.project.project_service.DTO.ProjectDTO;
import com.project.project_service.DTO.ProjectResponseDTO;
import com.project.project_service.Entity.Client;
import com.project.project_service.Entity.Entreprise;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Team;
import com.project.project_service.Repository.ClientRepository;
import com.project.project_service.Repository.EntrepriseRepository;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InitialProjectManagementService {

    @Autowired
    private EntrepriseRepository companyRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ProjetRepository projectRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    public void createCompanyTeamProject(InitialProjectManagementDTO dto, String authId) {

        // Récupérer le client à partir de l'auth_Id
        System.out.println("Récupération du client avec authId: " + authId);
        Client manager = clientRepository.findByAuthId(authId);  // Méthode personnalisée dans ClientRepository

        if (manager != null) {
            System.out.println(" Client trouvé : " + manager.getId());
        } else {
            System.out.println(" Client non trouvé pour l'authId: " + authId);
            System.out.println("Création du client...");
            manager = new Client();
            manager.setRole(dto.getRole());
            manager.setDepartment(dto.getDepartment());
            manager.setAuthId(authId);
            manager = clientRepository.saveAndFlush(manager);  // 💡 On force l'insertion immédiate
            System.out.println(" Client créé avec succès, ID: " + manager.getId());
        }

// Maintenant, on crée l'entreprise en associant le manager
        System.out.println("Création de l'entreprise...");
        Entreprise company = new Entreprise();
        company.setName(dto.getCompanyName());
        company.setIndustry(dto.getIndustry());
        company.setManager(manager);  //  Ici, le manager est maintenant en base !
        company = companyRepository.saveAndFlush(company); // 💡 On force l'insertion immédiate
        System.out.println("Entreprise créée avec succès, ID: " + company.getId());

// Mettre à jour le client pour lui associer l'entreprise
        manager.setCompany(company);
        clientRepository.save(manager);  //  Mise à jour finale du client
        System.out.println(" Association Client-Entreprise enregistrée !");

        // Créer l'équipe et définir l'ID du créateur
        System.out.println("Création de l'équipe...");
        Team team = new Team();
        team.setName(dto.getTeamName());
        team.setSize(dto.getNumEmployees());
        team.setCompany(company);  // Lier l'équipe à l'entreprise
        teamRepository.save(team);
        System.out.println("Équipe créée avec succès : " + team.getName());

        // Créer le projet et définir l'ID du créateur
        System.out.println("Création du projet...");
        Projet project = new Projet();
        project.setName(dto.getProjectName());
        project.setDescription(dto.getProjectDescription());
        project.setCompany(company);  // Lier le projet à l'entreprise
        projectRepository.save(project);
        System.out.println("Projet créé avec succès : " + project.getName());
    }


    public ProjectResponseDTO getProjectsByManager(String authId) {
        System.out.println("🔍 Récupération des projets pour le manager avec authId: " + authId);

        // Récupérer le client (manager) à partir de l'authId
        Client manager = clientRepository.findByAuthId(authId);
        if (manager == null) {
            System.out.println("❌ Manager non trouvé pour l'authId: " + authId);
            throw new RuntimeException("Manager non trouvé");
        }
        System.out.println("✅ Manager trouvé: " + manager.getId());

        // Récupérer l'entreprise associée au manager
        Entreprise company = manager.getCompany();
        if (company == null) {
            System.out.println("❌ Aucune entreprise associée au manager: " + authId);
            throw new RuntimeException("Aucune entreprise associée");
        }
        System.out.println("✅ Entreprise trouvée: " + company.getName());

        // Récupérer les projets associés à l'entreprise
        List<Projet> projects = projectRepository.findByCompany(company);
        List<ProjectDTO> projectDTOs = projects.stream()
                .map(project -> new ProjectDTO(project.getName(), project.getDescription()))
                .collect(Collectors.toList());
        System.out.println("✅ Projets trouvés: " + projectDTOs);

        // Construire la réponse
        return new ProjectResponseDTO(company.getName(), projectDTOs);
    }
}
