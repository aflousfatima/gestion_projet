package com.task.taskservice.Service;

import com.task.taskservice.Configuration.AuthClient;
import com.task.taskservice.Configuration.ProjectClient;
import com.task.taskservice.DTO.*;
import com.task.taskservice.Entity.*;
import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import com.task.taskservice.Mapper.BugMapper;
import com.task.taskservice.Repository.BugRepository;
import com.task.taskservice.Repository.FileAttachmentRepository;
import com.task.taskservice.Repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BugService {

    private static final Logger logger = LoggerFactory.getLogger(BugService.class);

    private final BugRepository bugRepository;
    private final TagRepository tagRepository;
    private final BugMapper bugMapper;
    private final AuthClient authClient;
    private final ProjectClient projectClient;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final CloudinaryService cloudinaryService;

    @Autowired
    public BugService(
            BugRepository bugRepository,
            TagRepository tagRepository,
            BugMapper bugMapper,
            AuthClient authClient,
            ProjectClient projectClient,
            CloudinaryService cloudinaryService,
            FileAttachmentRepository fileAttachmentRepository) {
        this.bugRepository = bugRepository;
        this.tagRepository = tagRepository;
        this.bugMapper = bugMapper;
        this.authClient = authClient;
        this.projectClient = projectClient;
        this.cloudinaryService = cloudinaryService;
        this.fileAttachmentRepository = fileAttachmentRepository;
    }

    @Transactional
    public BugDTO createBug(Long projectId, Long userStoryId, BugDTO bugDTO, String token) {
        String createdBy = authClient.decodeToken(token);
        if (bugDTO.getCreationDate() == null) {
            bugDTO.setCreationDate(LocalDate.now());
        }
        bugDTO.setProjectId(projectId);
        bugDTO.setUserStoryId(userStoryId);
        bugDTO.setCreatedBy(createdBy);

        Bug bug = bugMapper.toEntity(bugDTO);

        if (bugDTO.getAssignedUserIds() != null) {
            Set<String> userIdsSet = new HashSet<>(bugDTO.getAssignedUserIds());
            bug.setAssignedUserIds(userIdsSet);
        }

        if (bugDTO.getTags() != null && !bugDTO.getTags().isEmpty()) {
            logger.info("➡️ Tags reçus pour le bug : {}", bugDTO.getTags());
            Set<Tag> tags = new HashSet<>();
            for (String tagName : bugDTO.getTags()) {
                logger.info("🔍 Vérification du tag : {}", tagName);
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> {
                            logger.info("➕ Tag non trouvé, création du tag : {}", tagName);
                            Tag newTag = new Tag();
                            newTag.setName(tagName);
                            newTag.setWorkItems(new HashSet<>());
                            return tagRepository.save(newTag);
                        });
                tags.add(tag);
                logger.info("✅ Tag ajouté à la liste : {}", tag.getName());
            }
            bug.setTags(new HashSet<>());
            for (Tag tag : tags) {
                bug.getTags().add(tag);
                tag.getWorkItems().add(bug);
                logger.info("🔗 Tag associé au bug : {}", tag.getName());
            }
        }

        updateProgress(bug);
        Bug savedBug = bugRepository.save(bug);
        return bugMapper.toDTO(savedBug);
    }

    @Transactional
    public BugDTO updateBug(Long bugId, BugDTO bugDTO, String token) {
        String updatedBy = authClient.decodeToken(token);
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found with ID: " + bugId));

        if (bugDTO.getStatus() != null) {
            WorkItemStatus newStatus = bugDTO.getStatus();
            WorkItemStatus currentStatus = bug.getStatus();
            if (newStatus == WorkItemStatus.IN_PROGRESS && currentStatus != WorkItemStatus.IN_PROGRESS) {
                bug.setStartTime(LocalDateTime.now());
            } else if (currentStatus == WorkItemStatus.IN_PROGRESS && newStatus != WorkItemStatus.IN_PROGRESS) {
                if (bug.getStartTime() != null) {
                    long minutes = ChronoUnit.MINUTES.between(bug.getStartTime(), LocalDateTime.now());
                    if (minutes > 0) {
                        bug.setTotalTimeSpent(bug.getTotalTimeSpent() != null ? bug.getTotalTimeSpent() + minutes : minutes);
                        TimeEntry timeEntry = new TimeEntry();
                        timeEntry.setWorkItem(bug);
                        timeEntry.setDuration(minutes);
                        timeEntry.setAddedBy(updatedBy);
                        timeEntry.setAddedAt(LocalDateTime.now());
                        timeEntry.setType("travail");
                        bug.getTimeEntries().add(timeEntry);
                    }
                    bug.setStartTime(null);
                }
            }
            bug.setStatus(newStatus);
            if (newStatus == WorkItemStatus.DONE) {
                bug.setCompletedDate(LocalDate.now());
            }
        }

        if (bugDTO.getTitle() != null) bug.setTitle(bugDTO.getTitle());
        if (bugDTO.getDescription() != null) bug.setDescription(bugDTO.getDescription());
        if (bugDTO.getDueDate() != null) bug.setDueDate(bugDTO.getDueDate());
        if (bugDTO.getPriority() != null) bug.setPriority(bugDTO.getPriority());
        if (bugDTO.getSeverity() != null) bug.setSeverity(bugDTO.getSeverity());
        if (bugDTO.getStatus() != null) {
            bug.setStatus(bugDTO.getStatus());
            if (bugDTO.getStatus() == WorkItemStatus.DONE) {
                bug.setCompletedDate(LocalDate.now());
            } else {
                bug.setCompletedDate(null);
            }
        }
        if (bugDTO.getEstimationTime() != null) bug.setEstimationTime(bugDTO.getEstimationTime());
        if (bugDTO.getStartDate() != null) bug.setStartDate(bugDTO.getStartDate());
        bug.setUpdatedBy(updatedBy);
        bug.setLastModifiedDate(LocalDate.now());

        if (bugDTO.getAssignedUserIds() != null && !bugDTO.getAssignedUserIds().isEmpty()) {
            Set<String> userIdsSet = new HashSet<>(bugDTO.getAssignedUserIds());
            bug.setAssignedUserIds(userIdsSet);
        } else {
            bug.setAssignedUserIds(bug.getAssignedUserIds() != null ? bug.getAssignedUserIds() : new HashSet<>());
        }

        if (bugDTO.getTags() != null) {
            Set<Tag> tags = bugDTO.getTags().stream().map(tagName ->
                    tagRepository.findByName(tagName)
                            .orElseGet(() -> tagRepository.save(new Tag()))
            ).collect(Collectors.toSet());
            bug.setTags(tags);
        }

        updateProgress(bug);
        Bug updatedBug = bugRepository.save(bug);
        BugDTO responseDTO = bugMapper.toDTO(updatedBug);

        if (updatedBug.getAssignedUserIds() != null && !updatedBug.getAssignedUserIds().isEmpty()) {
            try {
                List<String> userIdList = updatedBug.getAssignedUserIds().stream().collect(Collectors.toList());
                String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
                List<UserDTO> assignedUsers = authClient.getUsersByIds(authHeader, userIdList);
                responseDTO.setAssignedUsers(assignedUsers != null ? assignedUsers : Collections.emptyList());
            } catch (Exception e) {
                logger.error("Failed to fetch user details: {}", e.getMessage());
                responseDTO.setAssignedUsers(Collections.emptyList());
            }
        } else {
            responseDTO.setAssignedUsers(Collections.emptyList());
        }

        return responseDTO;
    }

    @Transactional(readOnly = true)
    public BugDTO getBugById(Long projectId, Long userStoryId, Long bugId, String token) {
        String createdBy = authClient.decodeToken(token);
        if (createdBy == null) {
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }

        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new IllegalArgumentException("Bug not found with ID: " + bugId));

        if (!bug.getProjectId().equals(projectId) || !bug.getUserStory().equals(userStoryId)) {
            throw new IllegalArgumentException("Bug does not belong to the specified project or user story");
        }

        return bugMapper.toDTO(bug);
    }

    @Transactional(readOnly = true)
    public BugDTO getBugByBugId(Long bugId, String token) {
        String userId = authClient.decodeToken(token);
        if (userId == null) {
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found with ID: " + bugId));
        return bugMapper.toDTO(bug);
    }

    @Transactional
    public void deleteBug(Long bugId) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found with ID: " + bugId));
        bugRepository.delete(bug);
    }

    @Transactional(readOnly = true)
    public List<BugDTO> getBugsByProjectAndUserStory(Long projectId, Long userStoryId, String token) {
        String createdBy = authClient.decodeToken(token);
        if (createdBy == null) {
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }

        List<Bug> bugs = bugRepository.findByProjectIdAndUserStory(projectId, userStoryId);
        return bugs.stream()
                .map(bugMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BugDTO> getBugsByProjectId(Long projectId, String token) {
        String createdBy = authClient.decodeToken(token);
        if (createdBy == null) {
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }

        List<Bug> bugs = bugRepository.findByProjectId(projectId);
        return bugs.stream()
                .map(bugMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BugDTO> getBugsOfActiveSprint(Long projectId, String token) {
        logger.info("Fetching bugs for active sprint of project ID: {}", projectId);
        List<Long> activeStoryIds = projectClient.getUserStoriesOfActiveSprint(projectId);
        if (activeStoryIds.isEmpty()) {
            logger.info("No active story IDs found for project ID: {}", projectId);
            return Collections.emptyList();
        }

        List<Bug> bugs = bugRepository.findByUserStoryIn(activeStoryIds);
        if (bugs.isEmpty()) {
            logger.info("No bugs found for the active sprint of project ID: {}", projectId);
            return Collections.emptyList();
        }

        Set<String> allUserIds = bugs.stream()
                .flatMap(bug -> bug.getAssignedUserIds().stream())
                .collect(Collectors.toSet());
        List<UserDTO> users = Collections.emptyList();
        if (!allUserIds.isEmpty()) {
            try {
                users = authClient.getUsersByIds(token, new ArrayList<>(allUserIds));
            } catch (Exception e) {
                logger.error("Error fetching user details: {}", e.getMessage());
            }
        }

        Map<String, UserDTO> userMap = users.stream()
                .collect(Collectors.toMap(UserDTO::getId, user -> user));

        return bugs.stream()
                .map(bug -> {
                    BugDTO dto = bugMapper.toDTO(bug);
                    dto.setAssignedUsers(bug.getAssignedUserIds().stream()
                            .map(userMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Bug attachFileToBug(Long bugId, MultipartFile file, String token) throws IOException {
        logger.info("Attaching file to bug ID: {}, file: {}", bugId, file.getOriginalFilename());
        String uploadedBy = authClient.decodeToken(token);
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new IllegalArgumentException("Bug not found with ID: " + bugId));

        if (file.isEmpty()) {
            logger.error("Uploaded file is empty");
            throw new IllegalArgumentException("File cannot be empty");
        }

        Map uploadResult = cloudinaryService.uploadFile(file);
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        FileAttachment attachment = new FileAttachment(
                file.getOriginalFilename(),
                contentType,
                file.getSize(),
                uploadResult.get("secure_url").toString(),
                uploadResult.get("public_id").toString(),
                uploadedBy
        );
        attachment.setUploadedAt(LocalDateTime.now());

        bug.getAttachments().add(attachment);
        bugRepository.saveAndFlush(bug);
        return bug;
    }

    @Transactional
    public void deleteFileFromBug(String publicId, String token) throws IOException {
        logger.info("Deleting file with publicId: {}", publicId);
        FileAttachment attachment = fileAttachmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("File attachment not found with publicId: " + publicId));

        List<Bug> bugs = bugRepository.findByAttachmentsContaining(attachment);
        Bug bug = bugs.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No bug found with attachment publicId: " + publicId));

        bug.getAttachments().remove(attachment);
        try {
            cloudinaryService.deleteFile(publicId);
        } catch (IOException e) {
            logger.error("Failed to delete file from Cloudinary with publicId: {}", publicId, e);
            throw new IOException("Failed to delete file from Cloudinary: " + e.getMessage(), e);
        }

        bugRepository.save(bug);
        fileAttachmentRepository.delete(attachment);
    }

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(Long projectId, String token) {
        List<Long> activeStoryIds = projectClient.getUserStoriesOfActiveSprint(projectId);
        if (activeStoryIds.isEmpty()) {
            return new DashboardStatsDTO(0, 0, 0, 0, Map.of(), Map.of());
        }

        long completedBugs = bugRepository.countByUserStoryInAndStatus(activeStoryIds, WorkItemStatus.DONE);
        long notCompletedBugs = bugRepository.countByUserStoryInAndStatusNot(activeStoryIds, WorkItemStatus.DONE);
        long overdueBugs = bugRepository.countOverdueByUserStoryIn(activeStoryIds, WorkItemStatus.DONE, LocalDate.now());
        long totalBugs = bugRepository.countByUserStoryIn(activeStoryIds);

        Map<String, Long> bugsByStatus = bugRepository.countBugsByStatus(activeStoryIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((WorkItemStatus) row[0]).name(),
                        row -> (Long) row[1]
                ));

        Map<String, Long> bugsByPriority = bugRepository.countBugsByPriority(activeStoryIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((WorkItemPriority) row[0]).name(),
                        row -> (Long) row[1]
                ));

        return new DashboardStatsDTO(completedBugs, notCompletedBugs, overdueBugs, totalBugs, bugsByStatus, bugsByPriority);
    }

    @Transactional(readOnly = true)
    public List<BugCalendarDTO> getBugsForCalendar(Long projectId, String token) {
        List<Long> activeStoryIds = projectClient.getUserStoriesOfActiveSprint(projectId);
        if (activeStoryIds.isEmpty()) {
            return List.of();
        }

        List<Bug> bugs = bugRepository.findByUserStoryIn(activeStoryIds);
        return bugs.stream()
                .map(bug -> new BugCalendarDTO(
                        bug.getId(),
                        bug.getTitle(),
                        bug.getStartDate(),
                        bug.getDueDate()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimeEntryDTO> getTimeEntries(Long bugId, String token) {
        getBugByBugId(bugId, token);
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found with ID: " + bugId));

        return bug.getTimeEntries().stream()
                .map(te -> {
                    TimeEntryDTO dto = new TimeEntryDTO();
                    dto.setId(te.getId());
                    dto.setDuration(te.getDuration());
                    dto.setAddedBy(te.getAddedBy());
                    dto.setAddedAt(te.getAddedAt());
                    dto.setType(te.getType());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void addManualTimeEntry(Long bugId, Long duration, String type, String token) {
        String addedBy = authClient.decodeToken(token);
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found with ID: " + bugId));
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        bug.setTotalTimeSpent(bug.getTotalTimeSpent() != null ? bug.getTotalTimeSpent() + duration : duration);
        TimeEntry timeEntry = new TimeEntry();
        timeEntry.setWorkItem(bug);
        timeEntry.setDuration(duration);
        timeEntry.setAddedBy(addedBy);
        timeEntry.setAddedAt(LocalDateTime.now());
        timeEntry.setType(type);
        bug.getTimeEntries().add(timeEntry);
        updateProgress(bug);
        bugRepository.save(bug);
    }

    private void updateProgress(Bug bug) {
        if (bug.getStatus() == WorkItemStatus.TO_DO) {
            bug.setProgress(0.0);
        } else if (bug.getStatus() == WorkItemStatus.DONE) {
            bug.setProgress(100.0);
        } else if (bug.getStatus() == WorkItemStatus.IN_PROGRESS || bug.getStatus() == WorkItemStatus.BLOCKED) {
            if (bug.getEstimationTime() != null && bug.getEstimationTime() > 0 && bug.getTotalTimeSpent() != null) {
                double progress = (bug.getTotalTimeSpent().doubleValue() / bug.getEstimationTime()) * 100;
                bug.setProgress(Math.min(progress, 90.0));
            } else {
                bug.setProgress(0.0);
            }
        }
    }

    private BugDTO toBugDTOWithUsers(Bug bug, String token) {
        BugDTO responseDTO = bugMapper.toDTO(bug);
        if (bug.getAssignedUserIds() != null && !bug.getAssignedUserIds().isEmpty()) {
            try {
                List<String> userIdList = bug.getAssignedUserIds().stream().collect(Collectors.toList());
                String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
                List<UserDTO> assignedUsers = authClient.getUsersByIds(authHeader, userIdList);
                responseDTO.setAssignedUsers(assignedUsers != null ? assignedUsers : Collections.emptyList());
            } catch (Exception e) {
                logger.error("Failed to fetch user details: {}", e.getMessage());
                responseDTO.setAssignedUsers(Collections.emptyList());
            }
        } else {
            responseDTO.setAssignedUsers(Collections.emptyList());
        }
        return responseDTO;
    }
}