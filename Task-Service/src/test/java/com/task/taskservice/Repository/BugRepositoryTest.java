package com.task.taskservice.Repository;
import com.task.taskservice.Entity.Bug;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest  // Cette annotation configure une base de données en mémoire pour les tests
public class BugRepositoryTest {
    @Autowired
    private BugRepository bugRepository;
    @Test
    public void testSaveTask() {
        Bug bug = new Bug();
        bug.setTitle("Test Task");
        bug.setDescription("This is a test task");
        bug.setEstimationTime(120L);
        bug.setPriority(WorkItemPriority.HIGH);
        bug.setStatus(WorkItemStatus.TO_DO);

        Bug savedBug = bugRepository.save(bug);

        assertNotNull(savedBug.getId());  // Vérifier que l'ID est généré après l'enregistrement
        assertEquals("Test Bug", savedBug.getTitle());  // Vérifier que le titre est correct
    }

    @Test
    public void testFindById() {
        Bug bug = new Bug();
        bug.setTitle("Test Task");
        bug.setDescription("This is a test task");
        bugRepository.save(bug);

        Bug foundBug = bugRepository.findById(bug.getId()).orElse(null);

        assertNotNull(foundBug);  // Vérifier que la tâche est trouvée
        assertEquals(bug.getTitle(), foundBug.getTitle());  // Vérifier que le titre correspond
    }
}
