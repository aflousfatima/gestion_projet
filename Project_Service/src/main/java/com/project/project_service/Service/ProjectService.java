package com.project.project_service.Service;

import com.project.project_service.Entity.Client;
import com.project.project_service.Entity.Entreprise;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Repository.ClientRepository;
import com.project.project_service.Repository.ProjetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ProjetRepository projectRepository;

    @Transactional
    public void createProject(String authId, String name, String description) {
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
        projectRepository.save(project);

        System.out.println("Projet créé avec succès : " + project.getName());
    }

    @Transactional
    public void updateProject(String authId, String oldName, String newName, String description) {
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
}
