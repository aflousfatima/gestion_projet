package com.project.project_service.Repository;

import com.project.project_service.Entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
     Client findByAuthId(String authId);

}
