package com.task.taskservice.Mapper;

import com.task.taskservice.DTO.TaskDTO;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Entity.WorkItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    TaskMapper INSTANCE = Mappers.getMapper(TaskMapper.class);

    // WorkItem/Task → TaskDTO
    @Mapping(target = "dependencyIds", expression = "java(task.getDependencies() != null ? task.getDependencies().stream().map(WorkItem::getId).collect(java.util.stream.Collectors.toList()) : null)")
    @Mapping(target = "type", expression = "java(task instanceof com.task.taskservice.Entity.Bug ? \"BUG\" : \"TASK\")")
    @Mapping(target = "severity", expression = "java(task instanceof com.task.taskservice.Entity.Bug ? ((com.task.taskservice.Entity.Bug) task).getSeverity() != null ? ((com.task.taskservice.Entity.Bug) task).getSeverity().toString() : null : null)")
    TaskDTO toDTO(WorkItem task);

    // TaskDTO → WorkItem/Task (pour création)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dependencies", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "completedDate", ignore = true)
    @Mapping(target = "timeSpent", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    Task toEntity(TaskDTO taskDTO);

    // Mise à jour d'une tâche existante
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dependencies", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "completedDate", ignore = true) // Optionnel : ne pas ignorer si modifiable
    @Mapping(target = "timeSpent", ignore = true) // Optionnel : ne pas ignorer si modifiable
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    void updateEntity(TaskDTO taskDTO, @MappingTarget WorkItem task);
}