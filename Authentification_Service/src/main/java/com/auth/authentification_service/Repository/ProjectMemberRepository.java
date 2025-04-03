package com.auth.authentification_service.Repository;

import com.auth.authentification_service.Entity.ProjectMember;
import com.auth.authentification_service.Entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    // Méthodes personnalisées si besoin, par exemple :
    boolean existsByIdProjectIdAndIdUserId(Long projectId, String userId);
    List<ProjectMember> findByIdProjectId(Long projectId);
}