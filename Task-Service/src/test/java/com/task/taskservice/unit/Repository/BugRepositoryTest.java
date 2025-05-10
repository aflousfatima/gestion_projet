package com.task.taskservice.unit.Repository;

import com.task.taskservice.Entity.Bug;
import com.task.taskservice.Entity.FileAttachment;
import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import com.task.taskservice.Repository.BugRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BugRepositoryTest {

    @Autowired
    private BugRepository bugRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_shouldPersistBug() {
        // Arrange
        Bug bug = new Bug();
        bug.setTitle("Test Bug");
        bug.setProjectId(1L);
        bug.setUserStory(2L);
        bug.setStatus(WorkItemStatus.TO_DO);

        // Act
        Bug savedBug = bugRepository.save(bug);

        // Assert
        assertNotNull(savedBug.getId());
        assertEquals("Test Bug", savedBug.getTitle());
        assertEquals(1L, savedBug.getProjectId());
        assertEquals(2L, savedBug.getUserStory());
        assertEquals(WorkItemStatus.TO_DO, savedBug.getStatus());
    }

    @Test
    void findById_shouldReturnBug_whenBugExists() {
        // Arrange
        Bug bug = new Bug();
        bug.setTitle("Test Bug");
        bug.setProjectId(1L);
        bug.setUserStory(2L);
        Long bugId = entityManager.persistAndGetId(bug, Long.class);

        // Act
        Bug foundBug = bugRepository.findById(bugId).orElse(null);

        // Assert
        assertNotNull(foundBug);
        assertEquals(bugId, foundBug.getId());
        assertEquals("Test Bug", foundBug.getTitle());
    }

    @Test
    void delete_shouldRemoveBug_whenBugExists() {
        // Arrange
        Bug bug = new Bug();
        bug.setTitle("Test Bug");
        Long bugId = entityManager.persistAndGetId(bug, Long.class);

        // Act
        bugRepository.deleteById(bugId);

        // Assert
        Bug deletedBug = bugRepository.findById(bugId).orElse(null);
        assertNull(deletedBug);
    }

    @Test
    void findByProjectIdAndUserStory_shouldReturnBugs_whenMatching() {
        // Arrange
        Bug bug1 = new Bug();
        bug1.setTitle("Bug 1");
        bug1.setProjectId(1L);
        bug1.setUserStory(2L);
        Bug bug2 = new Bug();
        bug2.setTitle("Bug 2");
        bug2.setProjectId(1L);
        bug2.setUserStory(2L);
        Bug bug3 = new Bug();
        bug3.setTitle("Bug 3");
        bug3.setProjectId(1L);
        bug3.setUserStory(3L);

        entityManager.persist(bug1);
        entityManager.persist(bug2);
        entityManager.persist(bug3);
        entityManager.flush();

        // Act
        List<Bug> bugs = bugRepository.findByProjectIdAndUserStory(1L, 2L);

        // Assert
        assertEquals(2, bugs.size());
        assertTrue(bugs.stream().anyMatch(b -> b.getTitle().equals("Bug 1")));
        assertTrue(bugs.stream().anyMatch(b -> b.getTitle().equals("Bug 2")));
    }

    @Test
    void findByProjectId_shouldReturnBugs_whenMatching() {
        // Arrange
        Bug bug1 = new Bug();
        bug1.setTitle("Bug 1");
        bug1.setProjectId(1L);
        Bug bug2 = new Bug();
        bug2.setTitle("Bug 2");
        bug2.setProjectId(1L);
        Bug bug3 = new Bug();
        bug3.setTitle("Bug 3");
        bug3.setProjectId(2L);

        entityManager.persist(bug1);
        entityManager.persist(bug2);
        entityManager.persist(bug3);
        entityManager.flush();

        // Act
        List<Bug> bugs = bugRepository.findByProjectId(1L);

        // Assert
        assertEquals(2, bugs.size());
        assertTrue(bugs.stream().anyMatch(b -> b.getTitle().equals("Bug 1")));
        assertTrue(bugs.stream().anyMatch(b -> b.getTitle().equals("Bug 2")));
    }

    @Test
    void findByUserStoryIn_shouldReturnBugs_whenMatching() {
        // Arrange
        Bug bug1 = new Bug();
        bug1.setTitle("Bug 1");
        bug1.setUserStory(2L);
        Bug bug2 = new Bug();
        bug2.setTitle("Bug 2");
        bug2.setUserStory(3L);
        Bug bug3 = new Bug();
        bug3.setTitle("Bug 3");
        bug3.setUserStory(4L);

        entityManager.persist(bug1);
        entityManager.persist(bug2);
        entityManager.persist(bug3);
        entityManager.flush();

        // Act
        List<Bug> bugs = bugRepository.findByUserStoryIn(Arrays.asList(2L, 3L));

        // Assert
        assertEquals(2, bugs.size());
        assertTrue(bugs.stream().anyMatch(b -> b.getTitle().equals("Bug 1")));
        assertTrue(bugs.stream().anyMatch(b -> b.getTitle().equals("Bug 2")));
    }

    @Test
    void countByUserStoryInAndStatus_shouldReturnCorrectCount() {
        // Arrange
        Bug bug1 = new Bug();
        bug1.setUserStory(2L);
        bug1.setStatus(WorkItemStatus.DONE);
        Bug bug2 = new Bug();
        bug2.setUserStory(2L);
        bug2.setStatus(WorkItemStatus.DONE);
        Bug bug3 = new Bug();
        bug3.setUserStory(2L);
        bug3.setStatus(WorkItemStatus.IN_PROGRESS);

        entityManager.persist(bug1);
        entityManager.persist(bug2);
        entityManager.persist(bug3);
        entityManager.flush();

        // Act
        long count = bugRepository.countByUserStoryInAndStatus(Arrays.asList(2L), WorkItemStatus.DONE);

        // Assert
        assertEquals(2, count);
    }

    @Test
    void countByUserStoryInAndStatusNot_shouldReturnCorrectCount() {
        // Arrange
        Bug bug1 = new Bug();
        bug1.setUserStory(2L);
        bug1.setStatus(WorkItemStatus.DONE);
        Bug bug2 = new Bug();
        bug2.setUserStory(2L);
        bug2.setStatus(WorkItemStatus.IN_PROGRESS);
        Bug bug3 = new Bug();
        bug3.setUserStory(2L);
        bug3.setStatus(WorkItemStatus.TO_DO);

        entityManager.persist(bug1);
        entityManager.persist(bug2);
        entityManager.persist(bug3);
        entityManager.flush();

        // Act
        long count = bugRepository.countByUserStoryInAndStatusNot(Arrays.asList(2L), WorkItemStatus.DONE);

        // Assert
        assertEquals(2, count);
    }

    @Test
    void countOverdueByUserStoryIn_shouldReturnCorrectCount() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        Bug bug1 = new Bug();
        bug1.setUserStory(2L);
        bug1.setStatus(WorkItemStatus.IN_PROGRESS);
        bug1.setDueDate(yesterday);
        Bug bug2 = new Bug();
        bug2.setUserStory(2L);
        bug2.setStatus(WorkItemStatus.TO_DO);
        bug2.setDueDate(yesterday);
        Bug bug3 = new Bug();
        bug3.setUserStory(2L);
        bug3.setStatus(WorkItemStatus.DONE);
        bug3.setDueDate(yesterday);
        Bug bug4 = new Bug();
        bug4.setUserStory(2L);
        bug4.setStatus(WorkItemStatus.IN_PROGRESS);
        bug4.setDueDate(tomorrow);

        entityManager.persist(bug1);
        entityManager.persist(bug2);
        entityManager.persist(bug3);
        entityManager.persist(bug4);
        entityManager.flush();

        // Act
        long count = bugRepository.countOverdueByUserStoryIn(Arrays.asList(2L), WorkItemStatus.DONE, today);

        // Assert
        assertEquals(2, count); // bug1 and bug2 are overdue and not DONE
    }

    @Test
    void countByUserStoryIn_shouldReturnCorrectCount() {
        // Arrange
        Bug bug1 = new Bug();
        bug1.setUserStory(2L);
        Bug bug2 = new Bug();
        bug2.setUserStory(2L);
        Bug bug3 = new Bug();
        bug3.setUserStory(3L);

        entityManager.persist(bug1);
        entityManager.persist(bug2);
        entityManager.persist(bug3);
        entityManager.flush();

        // Act
        long count = bugRepository.countByUserStoryIn(Arrays.asList(2L));

        // Assert
        assertEquals(2, count);
    }

    @Test
    void countBugsByStatus_shouldReturnCorrectCounts() {
        // Arrange
        Bug bug1 = new Bug();
        bug1.setUserStory(2L);
        bug1.setStatus(WorkItemStatus.DONE);
        Bug bug2 = new Bug();
        bug2.setUserStory(2L);
        bug2.setStatus(WorkItemStatus.DONE);
        Bug bug3 = new Bug();
        bug3.setUserStory(2L);
        bug3.setStatus(WorkItemStatus.IN_PROGRESS);

        entityManager.persist(bug1);
        entityManager.persist(bug2);
        entityManager.persist(bug3);
        entityManager.flush();

        // Act
        List<Object[]> results = bugRepository.countBugsByStatus(Arrays.asList(2L));

        // Assert
        assertEquals(2, results.size());
        for (Object[] row : results) {
            WorkItemStatus status = (WorkItemStatus) row[0];
            Long count = (Long) row[1];
            if (status == WorkItemStatus.DONE) {
                assertEquals(2L, count);
            } else if (status == WorkItemStatus.IN_PROGRESS) {
                assertEquals(1L, count);
            }
        }
    }

    @Test
    void countBugsByPriority_shouldReturnCorrectCounts() {
        // Arrange
        Bug bug1 = new Bug();
        bug1.setUserStory(2L);
        bug1.setPriority(WorkItemPriority.HIGH);
        Bug bug2 = new Bug();
        bug2.setUserStory(2L);
        bug2.setPriority(WorkItemPriority.HIGH);
        Bug bug3 = new Bug();
        bug3.setUserStory(2L);
        bug3.setPriority(WorkItemPriority.LOW);

        entityManager.persist(bug1);
        entityManager.persist(bug2);
        entityManager.persist(bug3);
        entityManager.flush();

        // Act
        List<Object[]> results = bugRepository.countBugsByPriority(Arrays.asList(2L));

        // Assert
        assertEquals(2, results.size());
        for (Object[] row : results) {
            WorkItemPriority priority = (WorkItemPriority) row[0];
            Long count = (Long) row[1];
            if (priority == WorkItemPriority.HIGH) {
                assertEquals(2L, count);
            } else if (priority == WorkItemPriority.LOW) {
                assertEquals(1L, count);
            }
        }
    }


}