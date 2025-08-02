package com.auth.authentification_service.Repository;


import com.auth.authentification_service.Entity.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgendaRepository extends JpaRepository<Agenda, Long> {
    Optional<Agenda> findByUserId(String userId);
}