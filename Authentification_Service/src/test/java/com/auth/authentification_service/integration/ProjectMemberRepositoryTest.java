package com.auth.authentification_service.integration;

import com.auth.authentification_service.Entity.ProjectMember;
import com.auth.authentification_service.Entity.ProjectMemberId;
import com.auth.authentification_service.Repository.ProjectMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class ProjectMemberRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Test
    public void whenSaveProjectMember_thenReturnSavedProjectMember() {
        // Given
        ProjectMemberId id = new ProjectMemberId(1L, "user1");
        ProjectMember projectMember = new ProjectMember();
        projectMember.setId(id);
        projectMember.setRoleInProject("DEVELOPER");
        projectMember.setJoinedAt(LocalDateTime.now()); // Initialiser joinedAt

        // When
        ProjectMember saved = projectMemberRepository.save(projectMember);
        entityManager.flush();

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId().getProjectId()).isEqualTo(1L);
        assertThat(saved.getId().getUserId()).isEqualTo("user1");
        assertThat(saved.getRoleInProject()).isEqualTo("DEVELOPER");
        assertThat(saved.getJoinedAt()).isNotNull();


    }

    @Test
    public void whenFindById_thenReturnProjectMember() {
        // Given
        ProjectMemberId id = new ProjectMemberId(1L, "user1");
        ProjectMember projectMember = new ProjectMember();
        projectMember.setId(id);
        projectMember.setRoleInProject("DEVELOPER");
        projectMember.setJoinedAt(LocalDateTime.now()); // Initialiser joinedAt
        entityManager.persist(projectMember);
        entityManager.flush();

        // When
        Optional<ProjectMember> found = projectMemberRepository.findById(id);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId().getProjectId()).isEqualTo(1L);
        assertThat(found.get().getId().getUserId()).isEqualTo("user1");
        assertThat(found.get().getRoleInProject()).isEqualTo("DEVELOPER");
        assertThat(found.get().getJoinedAt()).isNotNull();
    }

    @Test
    public void whenFindByIdNonExistent_thenReturnEmpty() {
        // Given
        ProjectMemberId id = new ProjectMemberId(999L, "nonexistent");

        // When
        Optional<ProjectMember> found = projectMemberRepository.findById(id);

        // Then
        assertThat(found).isNotPresent();
    }

    @Test
    public void whenExistsByIdProjectIdAndIdUserId_thenReturnTrue() {
        // Given
        ProjectMemberId id = new ProjectMemberId(1L, "user1");
        ProjectMember projectMember = new ProjectMember();
        projectMember.setId(id);
        projectMember.setRoleInProject("DEVELOPER");
        projectMember.setJoinedAt(LocalDateTime.now()); // Initialiser joinedAt
        entityManager.persist(projectMember);
        entityManager.flush();

        // When
        boolean exists = projectMemberRepository.existsByIdProjectIdAndIdUserId(1L, "user1");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    public void whenExistsByIdProjectIdAndIdUserIdNonExistent_thenReturnFalse() {
        // Given
        // Pas d'enregistrement dans la base

        // When
        boolean exists = projectMemberRepository.existsByIdProjectIdAndIdUserId(999L, "nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    public void whenFindByIdProjectId_thenReturnProjectMembers() {
        // Given
        ProjectMemberId id1 = new ProjectMemberId(1L, "user1");
        ProjectMember member1 = new ProjectMember();
        member1.setId(id1);
        member1.setRoleInProject("DEVELOPER");
        member1.setJoinedAt(LocalDateTime.now()); // Initialiser joinedAt
        entityManager.persist(member1);

        ProjectMemberId id2 = new ProjectMemberId(1L, "user2");
        ProjectMember member2 = new ProjectMember();
        member2.setId(id2);
        member2.setRoleInProject("MANAGER");
        member2.setJoinedAt(LocalDateTime.now()); // Initialiser joinedAt
        entityManager.persist(member2);

        ProjectMemberId id3 = new ProjectMemberId(2L, "user3");
        ProjectMember member3 = new ProjectMember();
        member3.setId(id3);
        member3.setRoleInProject("DEVELOPER");
        member3.setJoinedAt(LocalDateTime.now()); // Initialiser joinedAt
        entityManager.persist(member3);

        entityManager.flush();

        // When
        List<ProjectMember> members = projectMemberRepository.findByIdProjectId(1L);

        // Then
        assertThat(members).hasSize(2);
        assertThat(members).extracting(m -> m.getId().getUserId()).containsExactlyInAnyOrder("user1", "user2");
        assertThat(members).extracting(m -> m.getRoleInProject()).containsExactlyInAnyOrder("DEVELOPER", "MANAGER");
        assertThat(members).extracting(m -> m.getJoinedAt()).doesNotContainNull();
    }

    @Test
    public void whenFindByIdProjectIdNonExistent_thenReturnEmptyList() {
        // Given
        // Pas d'enregistrement dans la base

        // When
        List<ProjectMember> members = projectMemberRepository.findByIdProjectId(999L);

        // Then
        assertThat(members).isEmpty();
    }

    @Test
    public void whenFindByIdUserId_thenReturnProjectMembers() {
        // Given
        ProjectMemberId id1 = new ProjectMemberId(1L, "user1");
        ProjectMember member1 = new ProjectMember();
        member1.setId(id1);
        member1.setRoleInProject("DEVELOPER");
        member1.setJoinedAt(LocalDateTime.now()); // Initialiser joinedAt
        entityManager.persist(member1);

        ProjectMemberId id2 = new ProjectMemberId(2L, "user1");
        ProjectMember member2 = new ProjectMember();
        member2.setId(id2);
        member2.setRoleInProject("MANAGER");
        member2.setJoinedAt(LocalDateTime.now()); // Initialiser joinedAt
        entityManager.persist(member2);

        ProjectMemberId id3 = new ProjectMemberId(3L, "user2");
        ProjectMember member3 = new ProjectMember();
        member3.setId(id3);
        member3.setRoleInProject("DEVELOPER");
        member3.setJoinedAt(LocalDateTime.now()); // Initialiser joinedAt
        entityManager.persist(member3);

        entityManager.flush();

        // When
        List<ProjectMember> members = projectMemberRepository.findByIdUserId("user1");

        // Then
        assertThat(members).hasSize(2);
        assertThat(members).extracting(m -> m.getId().getProjectId()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(members).extracting(m -> m.getRoleInProject()).containsExactlyInAnyOrder("DEVELOPER", "MANAGER");
        assertThat(members).extracting(m -> m.getJoinedAt()).doesNotContainNull();
    }

    @Test
    public void whenFindByIdUserIdNonExistent_thenReturnEmptyList() {
        // Given
        // Pas d'enregistrement dans la base

        // When
        List<ProjectMember> members = projectMemberRepository.findByIdUserId("nonexistent");

        // Then
        assertThat(members).isEmpty();
    }
}
