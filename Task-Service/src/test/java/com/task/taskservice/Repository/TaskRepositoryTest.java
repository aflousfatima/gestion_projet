package com.task.taskservice.Repository;

import com.task.taskservice.Entity.Task;
import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest  // Cette annotation configure une base de données en mémoire pour les tests
public class TaskRepositoryTest {
    @Autowired
    private TaskRepository taskRepository;
    @Test
    public void testSaveTask() {
        Task task = new Task();
        task.setTitle("Test Task");
        task.setDescription("This is a test task");
        task.setEstimationTime(120L);
        task.setPriority(WorkItemPriority.HIGH);
        task.setStatus(WorkItemStatus.TO_DO);

        Task savedTask = taskRepository.save(task);

        assertNotNull(savedTask.getId());  // Vérifier que l'ID est généré après l'enregistrement
        assertEquals("Test Task", savedTask.getTitle());  // Vérifier que le titre est correct
    }

    @Test
    public void testFindById() {
        Task task = new Task();
        task.setTitle("Test Task");
        task.setDescription("This is a test task");
        taskRepository.save(task);

        Task foundTask = taskRepository.findById(task.getId()).orElse(null);

        assertNotNull(foundTask);  // Vérifier que la tâche est trouvée
        assertEquals(task.getTitle(), foundTask.getTitle());  // Vérifier que le titre correspond
    }
}
