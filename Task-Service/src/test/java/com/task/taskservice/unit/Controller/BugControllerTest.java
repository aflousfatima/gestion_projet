package com.task.taskservice.unit.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.task.taskservice.Controller.BugController;
import com.task.taskservice.DTO.BugDTO;
import com.task.taskservice.DTO.TimeEntryDTO;
import com.task.taskservice.Entity.Bug;
import com.task.taskservice.Enumeration.WorkItemStatus;
import com.task.taskservice.Mapper.BugMapper;
import com.task.taskservice.Service.BugService;
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

class BugControllerTest {

    @Mock
    private BugService bugService;

    @Mock
    private BugMapper bugMapper;

    @InjectMocks
    private BugController bugController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Bug bug;
    private BugDTO bugDTO;

    private TimeEntryDTO timeEntryDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(bugController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Register JavaTimeModule for LocalDate support

        bug = new Bug();
        bug.setId(1L);
        bug.setProjectId(1L);
        bug.setId(1L);
        bug.setTitle("Test Bug");
        bug.setDescription("Test Description");
        bug.setStatus(WorkItemStatus.TO_DO);
        bug.setCreationDate(LocalDate.now());
        bug.setLastModifiedDate(LocalDate.now());

        bugDTO = new BugDTO();
        bugDTO.setId(1L);
        bugDTO.setProjectId(1L);
        bugDTO.setUserStoryId(1L);
        bugDTO.setTitle("Test Bug");
        bugDTO.setDescription("Test Description");
        bugDTO.setStatus(WorkItemStatus.TO_DO);
        bugDTO.setCreationDate(LocalDate.now()); // Updated to LocalDate
        // Assuming updatedAt is LocalDateTime; adjust if it's LocalDate

        timeEntryDTO = new TimeEntryDTO();
        timeEntryDTO.setId(1L);
        timeEntryDTO.setId(1L);
        timeEntryDTO.setDuration(120L);
        timeEntryDTO.setType("DEVELOPMENT");
        timeEntryDTO.setAddedAt(LocalDateTime.now());
    }

    @Test
    void createBug_shouldReturnCreatedBugDTO() throws Exception {
        String token = "Bearer valid-token";
        when(bugService.createBug(eq(1L), eq(1L), any(BugDTO.class), eq(token))).thenReturn(bugDTO);

        mockMvc.perform(post("/api/project/bugs/1/1/createBug")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bugDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Bug"));

        verify(bugService).createBug(eq(1L), eq(1L), any(BugDTO.class), eq(token));
    }

    @Test
    void createBug_shouldReturnUnauthorized_whenInvalidToken() throws Exception {
        String token = "Bearer invalid-token";
        when(bugService.createBug(eq(1L), eq(1L), any(BugDTO.class), eq(token)))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        mockMvc.perform(post("/api/project/bugs/1/1/createBug")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bugDTO)))
                .andExpect(status().isUnauthorized());

        verify(bugService).createBug(eq(1L), eq(1L), any(BugDTO.class), eq(token));
    }

    @Test
    void updateBug_shouldReturnUpdatedBugDTO() throws Exception {
        String token = "Bearer valid-token";
        when(bugService.updateBug(eq(1L), any(BugDTO.class), eq(token))).thenReturn(bugDTO);

        mockMvc.perform(put("/api/project/bugs/1/updateBug")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bugDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Bug"));

        verify(bugService).updateBug(eq(1L), any(BugDTO.class), eq(token));
    }

    @Test
    void updateBug_shouldReturnNotFound_whenBugDoesNotExist() throws Exception {
        String token = "Bearer valid-token";
        when(bugService.updateBug(eq(999L), any(BugDTO.class), eq(token)))
                .thenThrow(new NoSuchElementException("Bug not found"));

        mockMvc.perform(put("/api/project/bugs/999/updateBug")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bugDTO)))
                .andExpect(status().isNotFound());

        verify(bugService).updateBug(eq(999L), any(BugDTO.class), eq(token));
    }

    @Test
    void deleteBug_shouldReturnNoContent() throws Exception {
        doNothing().when(bugService).deleteBug(eq(1L));

        mockMvc.perform(delete("/api/project/bugs/1/deleteBug"))
                .andExpect(status().isNoContent());

        verify(bugService).deleteBug(eq(1L));
    }

    @Test
    void deleteBug_shouldReturnNotFound_whenBugDoesNotExist() throws Exception {
        doThrow(new NoSuchElementException("Bug not found")).when(bugService).deleteBug(eq(999L));

        mockMvc.perform(delete("/api/project/bugs/999/deleteBug"))
                .andExpect(status().isNotFound());

        verify(bugService).deleteBug(eq(999L));
    }

    @Test
    void getBug_shouldReturnBugDTO() throws Exception {
        String token = "Bearer valid-token";
        when(bugService.getBugById(eq(1L), eq(1L), eq(1L), eq(token))).thenReturn(bugDTO);

        mockMvc.perform(get("/api/project/bugs/1/1/1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Bug"));

        verify(bugService).getBugById(eq(1L), eq(1L), eq(1L), eq(token));
    }

    @Test
    void getBugsByProjectAndUserStory_shouldReturnBugDTOList() throws Exception {
        String token = "Bearer valid-token";
        List<BugDTO> bugs = Arrays.asList(bugDTO);
        when(bugService.getBugsByProjectAndUserStory(eq(1L), eq(1L), eq(token))).thenReturn(bugs);

        mockMvc.perform(get("/api/project/bugs/1/1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(bugService).getBugsByProjectAndUserStory(eq(1L), eq(1L), eq(token));
    }

    @Test
    void getBugsByProject_shouldReturnBugDTOList() throws Exception {
        String token = "Bearer valid-token";
        List<BugDTO> bugs = Arrays.asList(bugDTO);
        when(bugService.getBugsByProjectId(eq(1L), eq(token))).thenReturn(bugs);

        mockMvc.perform(get("/api/project/bugs/1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(bugService).getBugsByProjectId(eq(1L), eq(token));
    }

    @Test
    void getBugsOfActiveSprint_shouldReturnBugDTOList() throws Exception {
        String token = "Bearer valid-token";
        List<BugDTO> bugs = Arrays.asList(bugDTO);
        when(bugService.getBugsOfActiveSprint(eq(1L), eq(token))).thenReturn(bugs);

        mockMvc.perform(get("/api/project/bugs/active_sprint/1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(bugService).getBugsOfActiveSprint(eq(1L), eq(token));
    }

    @Test
    void uploadFileToBug_shouldReturnUpdatedBugDTO() throws Exception {
        String token = "Bearer valid-token";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Test content".getBytes());
        when(bugService.attachFileToBug(eq(1L), any(MultipartFile.class), eq(token))).thenReturn(bug);
        when(bugMapper.toDTO(eq(bug))).thenReturn(bugDTO);

        mockMvc.perform(multipart("/api/project/bugs/1/attachments")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(bugService).attachFileToBug(eq(1L), any(MultipartFile.class), eq(token));
        verify(bugMapper).toDTO(eq(bug));
    }

    @Test
    void uploadFileToBug_shouldReturnInternalServerError_whenExceptionOccurs() throws Exception {
        String token = "Bearer valid-token";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Test content".getBytes());
        when(bugService.attachFileToBug(eq(1L), any(MultipartFile.class), eq(token)))
                .thenThrow(new IOException("File upload failed"));

        mockMvc.perform(multipart("/api/project/bugs/1/attachments")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isInternalServerError());

        verify(bugService).attachFileToBug(eq(1L), any(MultipartFile.class), eq(token));
    }

    @Test
    void deleteFile_shouldReturnSuccessMessage() throws Exception {
        String token = "Bearer valid-token";
        String publicId = "file123";
        doNothing().when(bugService).deleteFileFromBug(eq(publicId), eq(token));

        mockMvc.perform(delete("/api/project/bugs/delete")
                        .param("publicId", publicId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().string("File deleted successfully."));

        verify(bugService).deleteFileFromBug(eq(publicId), eq(token));
    }

    @Test
    void deleteFile_shouldReturnNotFound_whenFileDoesNotExist() throws Exception {
        String token = "Bearer valid-token";
        String publicId = "file123";
        doThrow(new IllegalArgumentException("File not found")).when(bugService).deleteFileFromBug(eq(publicId), eq(token));

        mockMvc.perform(delete("/api/project/bugs/delete")
                        .param("publicId", publicId)
                        .header("Authorization", token))
                .andExpect(status().isNotFound());

        verify(bugService).deleteFileFromBug(eq(publicId), eq(token));
    }


    @Test
    void addManualTimeEntry_shouldReturnOk() throws Exception {
        String token = "Bearer valid-token";
        doNothing().when(bugService).addManualTimeEntry(eq(1L), eq(120L), eq("DEVELOPMENT"), eq(token));

        mockMvc.perform(post("/api/project/bugs/1/time-entry")
                        .param("duration", "120")
                        .param("type", "DEVELOPMENT")
                        .header("Authorization", token))
                .andExpect(status().isOk());

        verify(bugService).addManualTimeEntry(eq(1L), eq(120L), eq("DEVELOPMENT"), eq(token));
    }

    @Test
    void getTimeEntries_shouldReturnTimeEntryDTOList() throws Exception {
        String token = "Bearer valid-token";
        List<TimeEntryDTO> timeEntries = Arrays.asList(timeEntryDTO);
        when(bugService.getTimeEntries(eq(1L), eq(token))).thenReturn(timeEntries);

        mockMvc.perform(get("/api/project/bugs/1/time-entries")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].duration").value(120));

        verify(bugService).getTimeEntries(eq(1L), eq(token));
    }
}