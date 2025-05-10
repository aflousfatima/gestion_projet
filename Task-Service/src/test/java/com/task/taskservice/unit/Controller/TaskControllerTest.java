package com.task.taskservice.unit.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.task.taskservice.Controller.TaskController;
import com.task.taskservice.DTO.DashboardStatsDTO;
import com.task.taskservice.DTO.TaskCalendarDTO;
import com.task.taskservice.DTO.TaskDTO;
import com.task.taskservice.DTO.TimeEntryDTO;
import com.task.taskservice.DTO.WorkItemHistoryDTO;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Enumeration.WorkItemStatus;
import com.task.taskservice.Mapper.TaskMapper;
import com.task.taskservice.Service.TaskService;
import org.hibernate.jdbc.Work;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Task task;
    private TaskDTO taskDTO;
    private DashboardStatsDTO dashboardStatsDTO;
    private TaskCalendarDTO taskCalendarDTO;
    private TimeEntryDTO timeEntryDTO;
    private WorkItemHistoryDTO historyDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(taskController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // For LocalDate/LocalDateTime

        task = new Task();
        task.setId(1L);
        task.setProjectId(1L);
        task.setId(1L);
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setStatus(WorkItemStatus.TO_DO);
        task.setCreationDate(LocalDate.now());
        task.setLastModifiedDate(LocalDate.now());

        taskDTO = new TaskDTO();
        taskDTO.setId(1L);
        taskDTO.setProjectId(1L);
        taskDTO.setUserStoryId(1L);
        taskDTO.setTitle("Test Task");
        taskDTO.setDescription("Test Description");
        taskDTO.setStatus(WorkItemStatus.TO_DO);
        taskDTO.setCreationDate(LocalDate.now());

        dashboardStatsDTO = new DashboardStatsDTO();
        dashboardStatsDTO.setTotalTasks(10);
        dashboardStatsDTO.setNotCompletedTasks(5);
        dashboardStatsDTO.setCompletedTasks(5);

        taskCalendarDTO = new TaskCalendarDTO();
        taskCalendarDTO.setId(1L);
        taskCalendarDTO.setTitle("Test Task");
        taskCalendarDTO.setStartDate(LocalDate.now());
        taskCalendarDTO.setDueDate(LocalDate.now().plusDays(1));

        timeEntryDTO = new TimeEntryDTO();
        timeEntryDTO.setId(1L);
        timeEntryDTO.setId(1L);
        timeEntryDTO.setDuration(120L);
        timeEntryDTO.setType("DEVELOPMENT");
        timeEntryDTO.setAddedAt(LocalDateTime.now());

        historyDTO = new WorkItemHistoryDTO();
        historyDTO.setId(1L);
        historyDTO.setId(1L);
        historyDTO.setAction("UPDATE");
        historyDTO.setTimestamp(LocalDateTime.now());
        historyDTO.setAuthorName("user123");
    }

    @Test
    void createTask_shouldReturnCreatedTaskDTO() throws Exception {
        String token = "Bearer valid-token";
        when(taskService.createTask(eq(1L), eq(1L), any(TaskDTO.class), eq(token))).thenReturn(taskDTO);

        mockMvc.perform(post("/api/project/tasks/1/1/createTask")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Task"));

        verify(taskService).createTask(eq(1L), eq(1L), any(TaskDTO.class), eq(token));
    }

    @Test
    void createTask_shouldReturnUnauthorized_whenInvalidToken() throws Exception {
        String token = "Bearer invalid-token";
        when(taskService.createTask(eq(1L), eq(1L), any(TaskDTO.class), eq(token)))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        mockMvc.perform(post("/api/project/tasks/1/1/createTask")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDTO)))
                .andExpect(status().isUnauthorized());

        verify(taskService).createTask(eq(1L), eq(1L), any(TaskDTO.class), eq(token));
    }

    @Test
    void updateTask_shouldReturnUpdatedTaskDTO() throws Exception {
        String token = "Bearer valid-token";
        when(taskService.updateTask(eq(1L), any(TaskDTO.class), eq(token))).thenReturn(taskDTO);

        mockMvc.perform(put("/api/project/tasks/1/updateTask")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Task"));

        verify(taskService).updateTask(eq(1L), any(TaskDTO.class), eq(token));
    }

    @Test
    void updateTask_shouldReturnNotFound_whenTaskDoesNotExist() throws Exception {
        String token = "Bearer valid-token";
        when(taskService.updateTask(eq(999L), any(TaskDTO.class), eq(token)))
                .thenThrow(new NoSuchElementException("Task not found"));

        mockMvc.perform(put("/api/project/tasks/999/updateTask")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDTO)))
                .andExpect(status().isNotFound());

        verify(taskService).updateTask(eq(999L), any(TaskDTO.class), eq(token));
    }

    @Test
    void deleteTask_shouldReturnNoContent() throws Exception {
        doNothing().when(taskService).deleteTask(eq(1L));

        mockMvc.perform(delete("/api/project/tasks/1/deleteTask"))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(eq(1L));
    }

    @Test
    void deleteTask_shouldReturnNotFound_whenTaskDoesNotExist() throws Exception {
        doThrow(new NoSuchElementException("Task not found")).when(taskService).deleteTask(eq(999L));

        mockMvc.perform(delete("/api/project/tasks/999/deleteTask"))
                .andExpect(status().isNotFound());

        verify(taskService).deleteTask(eq(999L));
    }

    @Test
    void getTask_shouldReturnTaskDTO() throws Exception {
        String token = "Bearer valid-token";
        when(taskService.getTaskById(eq(1L), eq(1L), eq(1L), eq(token))).thenReturn(taskDTO);

        mockMvc.perform(get("/api/project/tasks/1/1/1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Task"));

        verify(taskService).getTaskById(eq(1L), eq(1L), eq(1L), eq(token));
    }

    @Test
    void getTasksByProjectAndUserStory_shouldReturnTaskDTOList() throws Exception {
        String token = "Bearer valid-token";
        List<TaskDTO> tasks = Arrays.asList(taskDTO);
        when(taskService.getTasksByProjectAndUserStory(eq(1L), eq(1L), eq(token))).thenReturn(tasks);

        mockMvc.perform(get("/api/project/tasks/1/1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(taskService).getTasksByProjectAndUserStory(eq(1L), eq(1L), eq(token));
    }

    @Test
    void getTasksByProject_shouldReturnTaskDTOList() throws Exception {
        String token = "Bearer valid-token";
        List<TaskDTO> tasks = Arrays.asList(taskDTO);
        when(taskService.getTasksByProjectId(eq(1L), eq(token))).thenReturn(tasks);

        mockMvc.perform(get("/api/project/tasks/1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(taskService).getTasksByProjectId(eq(1L), eq(token));
    }

    @Test
    void getTasksOfActiveSprint_shouldReturnTaskDTOList() throws Exception {
        String token = "Bearer valid-token";
        List<TaskDTO> tasks = Arrays.asList(taskDTO);
        when(taskService.getTasksOfActiveSprint(eq(1L), eq(token))).thenReturn(tasks);

        mockMvc.perform(get("/api/project/tasks/active_sprint/1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(taskService).getTasksOfActiveSprint(eq(1L), eq(token));
    }

    @Test
    void getTasksByUserAndActiveSprints_shouldReturnTaskDTOList() throws Exception {
        String token = "Bearer valid-token";
        List<TaskDTO> tasks = Arrays.asList(taskDTO);
        when(taskService.getTasksByUserAndActiveSprints(eq(token))).thenReturn(tasks);

        mockMvc.perform(get("/api/project/tasks/user/active-sprints")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(taskService).getTasksByUserAndActiveSprints(eq(token));
    }

    @Test
    void uploadFileToTask_shouldReturnUpdatedTaskDTO() throws Exception {
        String token = "Bearer valid-token";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Test content".getBytes());
        when(taskService.attachFileToTask(eq(1L), any(MultipartFile.class), eq(token))).thenReturn(task);
        when(taskMapper.toDTO(eq(task))).thenReturn(taskDTO);

        mockMvc.perform(multipart("/api/project/tasks/1/attachments")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(taskService).attachFileToTask(eq(1L), any(MultipartFile.class), eq(token));
        verify(taskMapper).toDTO(eq(task));
    }

    @Test
    void uploadFileToTask_shouldReturnInternalServerError_whenExceptionOccurs() throws Exception {
        String token = "Bearer valid-token";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Test content".getBytes());
        when(taskService.attachFileToTask(eq(1L), any(MultipartFile.class), eq(token)))
                .thenThrow(new IOException("File upload failed"));

        mockMvc.perform(multipart("/api/project/tasks/1/attachments")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isInternalServerError());

        verify(taskService).attachFileToTask(eq(1L), any(MultipartFile.class), eq(token));
    }

    @Test
    void deleteFile_shouldReturnSuccessMessage() throws Exception {
        String token = "Bearer valid-token";
        String publicId = "file123";
        doNothing().when(taskService).deleteFileFromTask(eq(publicId), eq(token));

        mockMvc.perform(delete("/api/project/tasks/delete")
                        .param("publicId", publicId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().string("File deleted successfully."));

        verify(taskService).deleteFileFromTask(eq(publicId), eq(token));
    }

    @Test
    void deleteFile_shouldReturnNotFound_whenFileDoesNotExist() throws Exception {
        String token = "Bearer valid-token";
        String publicId = "file123";
        doThrow(new IllegalArgumentException("File not found")).when(taskService).deleteFileFromTask(eq(publicId), eq(token));

        mockMvc.perform(delete("/api/project/tasks/delete")
                        .param("publicId", publicId)
                        .header("Authorization", token))
                .andExpect(status().isNotFound());

        verify(taskService).deleteFileFromTask(eq(publicId), eq(token));
    }

    @Test
    void getDashboardStats_shouldReturnDashboardStatsDTO() throws Exception {
        String token = "Bearer valid-token";
        when(taskService.getDashboardStats(eq(1L), eq(token))).thenReturn(dashboardStatsDTO);

        mockMvc.perform(get("/api/project/tasks/dashboard/1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(10));

        verify(taskService).getDashboardStats(eq(1L), eq(token));
    }

    @Test
    void getTasksForCalendar_shouldReturnTaskCalendarDTOList() throws Exception {
        String token = "Bearer valid-token";
        List<TaskCalendarDTO> tasks = Arrays.asList(taskCalendarDTO);
        when(taskService.getTasksForCalendar(eq(1L), eq(token))).thenReturn(tasks);

        mockMvc.perform(get("/api/project/tasks/calendar/1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(taskService).getTasksForCalendar(eq(1L), eq(token));
    }

    @Test
    void addManualTimeEntry_shouldReturnOk() throws Exception {
        String token = "Bearer valid-token";
        doNothing().when(taskService).addManualTimeEntry(eq(1L), eq(120L), eq("DEVELOPMENT"), eq(token));

        mockMvc.perform(post("/api/project/tasks/1/time-entry")
                        .param("duration", "120")
                        .param("type", "DEVELOPMENT")
                        .header("Authorization", token))
                .andExpect(status().isOk());

        verify(taskService).addManualTimeEntry(eq(1L), eq(120L), eq("DEVELOPMENT"), eq(token));
    }

    @Test
    void getTimeEntries_shouldReturnTimeEntryDTOList() throws Exception {
        String token = "Bearer valid-token";
        List<TimeEntryDTO> timeEntries = Arrays.asList(timeEntryDTO);
        when(taskService.getTimeEntries(eq(1L), eq(token))).thenReturn(timeEntries);

        mockMvc.perform(get("/api/project/tasks/1/time-entries")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].duration").value(120));

        verify(taskService).getTimeEntries(eq(1L), eq(token));
    }

    @Test
    void addDependency_shouldReturnUpdatedTaskDTO() throws Exception {
        String token = "Bearer valid-token";
        when(taskService.addDependency(eq(1L), eq(2L), eq(token))).thenReturn(taskDTO);

        mockMvc.perform(post("/api/project/tasks/1/dependencies/2/add-dependancy")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(taskService).addDependency(eq(1L), eq(2L), eq(token));
    }

    @Test
    void addDependency_shouldReturnNotFound_whenTaskDoesNotExist() throws Exception {
        String token = "Bearer valid-token";
        when(taskService.addDependency(eq(999L), eq(2L), eq(token)))
                .thenThrow(new NoSuchElementException("Task not found"));

        mockMvc.perform(post("/api/project/tasks/999/dependencies/2/add-dependancy")
                        .header("Authorization", token))
                .andExpect(status().isNotFound());

        verify(taskService).addDependency(eq(999L), eq(2L), eq(token));
    }

    @Test
    void removeDependency_shouldReturnUpdatedTaskDTO() throws Exception {
        String token = "Bearer valid-token";
        when(taskService.removeDependency(eq(1L), eq(2L), eq(token))).thenReturn(taskDTO);

        mockMvc.perform(delete("/api/project/tasks/1/dependencies/2")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(taskService).removeDependency(eq(1L), eq(2L), eq(token));
    }

    @Test
    void getPotentialDependencies_shouldReturnTaskDTOList() throws Exception {
        String token = "Bearer valid-token";
        List<TaskDTO> tasks = Arrays.asList(taskDTO);
        when(taskService.getPotentialDependencies(eq(1L), eq(token))).thenReturn(tasks);

        mockMvc.perform(get("/api/project/tasks/1/potential-dependencies")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(taskService).getPotentialDependencies(eq(1L), eq(token));
    }

    @Test
    void getTaskHistory_shouldReturnHistoryDTOList() throws Exception {
        String token = "Bearer valid-token";
        List<WorkItemHistoryDTO> history = Arrays.asList(historyDTO);
        when(taskService.getTaskHistory(eq(1L), eq(token))).thenReturn(history);

        mockMvc.perform(get("/api/project/tasks/1/history")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(taskService).getTaskHistory(eq(1L), eq(token));
    }

    @Test
    void getTaskHistoryWithAuthorNames_shouldReturnHistoryDTOList() throws Exception {
        String token = "Bearer valid-token";
        historyDTO.setAuthorName("John Doe");
        List<WorkItemHistoryDTO> history = Arrays.asList(historyDTO);
        when(taskService.getTaskHistoryWithAuthorNames(eq(1L), eq(token))).thenReturn(history);

        mockMvc.perform(get("/api/project/tasks/1/history-with-author-names")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].authorName").value("John Doe"));

        verify(taskService).getTaskHistoryWithAuthorNames(eq(1L), eq(token));
    }

    @Test
    void getTasksByProjectId_shouldReturnTaskList() throws Exception {
        List<Task> tasks = Arrays.asList(task);
        when(taskService.getTasksByProjectId(eq(1L))).thenReturn(tasks);

        mockMvc.perform(get("/api/project/tasks/1/fetch_tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(taskService).getTasksByProjectId(eq(1L));
    }

    @Test
    void countTasksByProjectId_shouldReturnCount() throws Exception {
        when(taskService.countTasksByProjectId(eq(1L))).thenReturn(10L);

        mockMvc.perform(get("/api/project/tasks/1/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));

        verify(taskService).countTasksByProjectId(eq(1L));
    }

    @Test
    void getTasksByProjectAndUserStoryInternal_shouldReturnTaskDTOList() throws Exception {
        List<TaskDTO> tasks = Arrays.asList(taskDTO);
        when(taskService.getTasksByProjectAndUserStoryInternal(eq(1L), eq(1L))).thenReturn(tasks);

        mockMvc.perform(get("/api/project/tasks/internal/1/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(taskService).getTasksByProjectAndUserStoryInternal(eq(1L), eq(1L));
    }
}