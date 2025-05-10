package com.task.taskservice.unit.Repository;

import com.task.taskservice.Entity.FileAttachment;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import com.task.taskservice.Repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_shouldPersistTask() {
        // Arrange
        Task task = new Task();
        task.setTitle("Test Task");
        task.setProjectId(1L);
        task.setUserStory(2L);
        task.setStatus(WorkItemStatus.TO_DO);

        // Act
        Task savedTask = taskRepository.save(task);

        // Assert
        assertNotNull(savedTask.getId());
        assertEquals("Test Task", savedTask.getTitle());
        assertEquals(1L, savedTask.getProjectId());
        assertEquals(2L, savedTask.getUserStory());
        assertEquals(WorkItemStatus.TO_DO, savedTask.getStatus());
    }

    @Test
    void findById_shouldReturnTask_whenTaskExists() {
        // Arrange
        Task task = new Task();
        task.setTitle("Test Task");
        task.setProjectId(1L);
        task.setUserStory(2L);
        Long taskId = entityManager.persistAndGetId(task, Long.class);

        // Act
        Task foundTask = taskRepository.findById(taskId).orElse(null);

        // Assert
        assertNotNull(foundTask);
        assertEquals(taskId, foundTask.getId());
        assertEquals("Test Task", foundTask.getTitle());
    }

    @Test
    void delete_shouldRemoveTask_whenTaskExists() {
        // Arrange
        Task task = new Task();
        task.setTitle("Test Task");
        Long taskId = entityManager.persistAndGetId(task, Long.class);

        // Act
        taskRepository.deleteById(taskId);

        // Assert
        Task deletedTask = taskRepository.findById(taskId).orElse(null);
        assertNull(deletedTask);
    }

    @Test
    void findByProjectIdAndUserStory_shouldReturnTasks_whenMatching() {
        // Arrange
        Task task1 = new Task();
        task1.setTitle("Task 1");
        task1.setProjectId(1L);
        task1.setUserStory(2L);
        Task task2 = new Task();
        task2.setTitle("Task 2");
        task2.setProjectId(1L);
        task2.setUserStory(2L);
        Task task3 = new Task();
        task3.setTitle("Task 3");
        task3.setProjectId(1L);
        task3.setUserStory(3L);

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        // Act
        List<Task> tasks = taskRepository.findByProjectIdAndUserStory(1L, 2L);

        // Assert
        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Task 1")));
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Task 2")));
    }

    @Test
    void findByProjectId_shouldReturnTasks_whenMatching() {
        // Arrange
        Task task1 = new Task();
        task1.setTitle("Task 1");
        task1.setProjectId(1L);
        Task task2 = new Task();
        task2.setTitle("Task 2");
        task2.setProjectId(1L);
        Task task3 = new Task();
        task3.setTitle("Task 3");
        task3.setProjectId(2L);

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        // Act
        List<Task> tasks = taskRepository.findByProjectId(1L);

        // Assert
        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Task 1")));
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Task 2")));
    }

    @Test
    void findByUserStoryIn_shouldReturnTasks_whenMatching() {
        // Arrange
        Task task1 = new Task();
        task1.setTitle("Task 1");
        task1.setUserStory(2L);
        Task task2 = new Task();
        task2.setTitle("Task 2");
        task2.setUserStory(3L);
        Task task3 = new Task();
        task3.setTitle("Task 3");
        task3.setUserStory(4L);

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        // Act
        List<Task> tasks = taskRepository.findByUserStoryIn(Arrays.asList(2L, 3L));

        // Assert
        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Task 1")));
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Task 2")));
    }

    @Test
    void findByAttachmentsContaining_shouldReturnTask_whenMatching() {
        // Arrange
        FileAttachment attachment = new FileAttachment();
        attachment.setFileName("test.pdf");
        attachment.setPublicId("file123");
        Long attachmentId = entityManager.persistAndGetId(attachment, Long.class);

        Task task = new Task();
        task.setTitle("Task 1");
        task.setAttachments(Arrays.asList(attachment));
        entityManager.persist(task);
        entityManager.flush();

        FileAttachment fetchedAttachment = entityManager.find(FileAttachment.class, attachmentId);

        // Act
        Optional<Task> foundTask = taskRepository.findByAttachmentsContaining(fetchedAttachment);

        // Assert
        assertTrue(foundTask.isPresent());
        assertEquals("Task 1", foundTask.get().getTitle());
    }

    @Test
    void findByDependenciesId_shouldReturnTasks_whenMatching() {
        // Arrange
        Task task1 = new Task();
        task1.setTitle("Task 1");
        Long task1Id = entityManager.persistAndGetId(task1, Long.class);

        Task task2 = new Task();
        task2.setTitle("Task 2");
        task2.setDependencies(Arrays.asList(task1));
        Task task3 = new Task();
        task3.setTitle("Task 3");
        task3.setDependencies(Arrays.asList(task1));
        Task task4 = new Task();
        task4.setTitle("Task 4");
        task4.setDependencies(Arrays.asList());

        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.persist(task4);
        entityManager.flush();

        // Act
        List<Task> tasks = taskRepository.findByDependenciesId(task1Id);

        // Assert
        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Task 2")));
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Task 3")));
    }

    @Test
    void countByUserStoryInAndStatus_shouldReturnCorrectCount() {
        // Arrange
        Task task1 = new Task();
        task1.setUserStory(2L);
        task1.setStatus(WorkItemStatus.DONE);
        Task task2 = new Task();
        task2.setUserStory(2L);
        task2.setStatus(WorkItemStatus.DONE);
        Task task3 = new Task();
        task3.setUserStory(2L);
        task3.setStatus(WorkItemStatus.IN_PROGRESS);

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        // Act
        long count = taskRepository.countByUserStoryInAndStatus(Arrays.asList(2L), WorkItemStatus.DONE);

        // Assert
        assertEquals(2, count);
    }

    @Test
    void countByUserStoryInAndStatusNot_shouldReturnCorrectCount() {
        // Arrange
        Task task1 = new Task();
        task1.setUserStory(2L);
        task1.setStatus(WorkItemStatus.DONE);
        Task task2 = new Task();
        task2.setUserStory(2L);
        task2.setStatus(WorkItemStatus.IN_PROGRESS);
        Task task3 = new Task();
        task3.setUserStory(2L);
        task3.setStatus(WorkItemStatus.TO_DO);

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        // Act
        long count = taskRepository.countByUserStoryInAndStatusNot(Arrays.asList(2L), WorkItemStatus.DONE);

        // Assert
        assertEquals(2, count);
    }

    @Test
    void countOverdueByUserStoryIn_shouldReturnCorrectCount() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        Task task1 = new Task();
        task1.setUserStory(2L);
        task1.setStatus(WorkItemStatus.IN_PROGRESS);
        task1.setDueDate(yesterday);
        Task task2 = new Task();
        task2.setUserStory(2L);
        task2.setStatus(WorkItemStatus.TO_DO);
        task2.setDueDate(yesterday);
        Task task3 = new Task();
        task3.setUserStory(2L);
        task3.setStatus(WorkItemStatus.DONE);
        task3.setDueDate(yesterday);
        Task task4 = new Task();
        task4.setUserStory(2L);
        task4.setStatus(WorkItemStatus.IN_PROGRESS);
        task4.setDueDate(tomorrow);

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.persist(task4);
        entityManager.flush();

        // Act
        long count = taskRepository.countOverdueByUserStoryIn(Arrays.asList(2L), WorkItemStatus.DONE, today);

        // Assert
        assertEquals(2, count); // task1 and task2 are overdue and not DONE
    }

    @Test
    void countByUserStoryIn_shouldReturnCorrectCount() {
        // Arrange
        Task task1 = new Task();
        task1.setUserStory(2L);
        Task task2 = new Task();
        task2.setUserStory(2L);
        Task task3 = new Task();
        task3.setUserStory(3L);

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        // Act
        long count = taskRepository.countByUserStoryIn(Arrays.asList(2L));

        // Assert
        assertEquals(2, count);
    }

    @Test
    void countTasksByStatus_shouldReturnCorrectCounts() {
        // Arrange
        Task task1 = new Task();
        task1.setUserStory(2L);
        task1.setStatus(WorkItemStatus.DONE);
        Task task2 = new Task();
        task2.setUserStory(2L);
        task2.setStatus(WorkItemStatus.DONE);
        Task task3 = new Task();
        task3.setUserStory(2L);
        task3.setStatus(WorkItemStatus.IN_PROGRESS);

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        // Act
        List<Object[]> results = taskRepository.countTasksByStatus(Arrays.asList(2L));

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
    void countTasksByPriority_shouldReturnCorrectCounts() {
        // Arrange
        Task task1 = new Task();
        task1.setUserStory(2L);
        task1.setPriority(WorkItemPriority.HIGH);
        Task task2 = new Task();
        task2.setUserStory(2L);
        task2.setPriority(WorkItemPriority.HIGH);
        Task task3 = new Task();
        task3.setUserStory(2L);
        task3.setPriority(WorkItemPriority.LOW);

        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        // Act
        List<Object[]> results = taskRepository.countTasksByPriority(Arrays.asList(2L));

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