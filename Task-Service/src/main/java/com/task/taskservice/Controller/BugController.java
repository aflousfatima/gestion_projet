package com.task.taskservice.Controller;

import com.task.taskservice.DTO.BugDTO;
import com.task.taskservice.DTO.DashboardStatsDTO;
import com.task.taskservice.DTO.BugCalendarDTO;
import com.task.taskservice.DTO.TimeEntryDTO;
import com.task.taskservice.Entity.Bug;
import com.task.taskservice.Mapper.BugMapper;
import com.task.taskservice.Service.BugService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/project/bugs")
public class BugController {

    private static final Logger logger = LoggerFactory.getLogger(BugService.class);

    @Autowired
    private BugService bugService;
    @Autowired
    private BugMapper bugMapper;

    @PostMapping("/{projectId}/{userStoryId}/createBug")
    public ResponseEntity<BugDTO> createBug(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestBody BugDTO bugDTO,
            @RequestHeader("Authorization") String token) {
        BugDTO createdBug = bugService.createBug(projectId, userStoryId, bugDTO, token);
        return new ResponseEntity<>(createdBug, HttpStatus.CREATED);
    }

    @PutMapping("/{bugId}/updateBug")
    public ResponseEntity<BugDTO> updateBug(
            @PathVariable Long bugId,
            @RequestBody BugDTO bugDTO,
            @RequestHeader("Authorization") String token) {
        BugDTO updatedBug = bugService.updateBug(bugId, bugDTO, token);
        return new ResponseEntity<>(updatedBug, HttpStatus.OK);
    }

    @DeleteMapping("/{bugId}/deleteBug")
    public ResponseEntity<Void> deleteBug(@PathVariable Long bugId) {
        bugService.deleteBug(bugId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{projectId}/{userStoryId}/{bugId}")
    public ResponseEntity<BugDTO> getBug(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @PathVariable Long bugId,
            @RequestHeader("Authorization") String token) {
        BugDTO bugDTO = bugService.getBugById(projectId, userStoryId, bugId, token);
        return new ResponseEntity<>(bugDTO, HttpStatus.OK);
    }

    @GetMapping("/{projectId}/{userStoryId}")
    public ResponseEntity<List<BugDTO>> getBugsByProjectAndUserStory(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestHeader("Authorization") String token) {
        List<BugDTO> bugs = bugService.getBugsByProjectAndUserStory(projectId, userStoryId, token);
        return new ResponseEntity<>(bugs, HttpStatus.OK);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<List<BugDTO>> getBugsByProject(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {
        List<BugDTO> bugs = bugService.getBugsByProjectId(projectId, token);
        return new ResponseEntity<>(bugs, HttpStatus.OK);
    }

    @GetMapping("/active_sprint/{projectId}")
    public ResponseEntity<List<BugDTO>> getBugsOfActiveSprint(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {
        List<BugDTO> bugs = bugService.getBugsOfActiveSprint(projectId, token);
        return new ResponseEntity<>(bugs, HttpStatus.OK);
    }

    @PostMapping("/{bugId}/attachments")
    public ResponseEntity<BugDTO> uploadFileToBug(
            @PathVariable Long bugId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {
        try {
            Bug updatedBug = bugService.attachFileToBug(bugId, file, token);
            BugDTO bugDTO = bugMapper.toDTO(updatedBug);
            return new ResponseEntity<>(bugDTO, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(
            @RequestParam String publicId,
            @RequestHeader("Authorization") String token) {
        logger.info("Received DELETE request for publicId: {}", publicId);
        try {
            bugService.deleteFileFromBug(publicId, token);
            return new ResponseEntity<>("File deleted successfully.", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.warn("File not found for publicId: {}", publicId);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            logger.error("Failed to delete file from Cloudinary for publicId: {}", publicId, e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            logger.error("Failed to delete file for publicId: {}", publicId, e);
            return new ResponseEntity<>("Failed to delete file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/dashboard/{projectId}")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {
        DashboardStatsDTO stats = bugService.getDashboardStats(projectId, token);
        return new ResponseEntity<>(stats, HttpStatus.OK);
    }

    @GetMapping("/calendar/{projectId}")
    public ResponseEntity<List<BugCalendarDTO>> getBugsForCalendar(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {
        List<BugCalendarDTO> bugs = bugService.getBugsForCalendar(projectId, token);
        return new ResponseEntity<>(bugs, HttpStatus.OK);
    }

    @PostMapping("/{bugId}/time-entry")
    public ResponseEntity<Void> addManualTimeEntry(
            @PathVariable Long bugId,
            @RequestParam Long duration,
            @RequestParam String type,
            @RequestHeader("Authorization") String token) {
        bugService.addManualTimeEntry(bugId, duration, type, token);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/{bugId}/time-entries")
    public ResponseEntity<List<TimeEntryDTO>> getTimeEntries(
            @PathVariable Long bugId,
            @RequestHeader("Authorization") String token) {
        List<TimeEntryDTO> timeEntryDTOs = bugService.getTimeEntries(bugId, token);
        return new ResponseEntity<>(timeEntryDTOs, HttpStatus.OK);
    }
}