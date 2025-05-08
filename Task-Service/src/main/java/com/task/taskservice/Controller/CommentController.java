package com.task.taskservice.Controller;

import com.task.taskservice.DTO.CommentDTO;
import com.task.taskservice.Entity.Comment;
import com.task.taskservice.Service.CommentService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project/task/comments")
@OpenAPIDefinition(info = @Info(
        title = "API de Gestion des Commentaires",
        version = "1.0",
        description = "Cette API permet de gérer les commentaires associés aux tâches des projets."
),
        servers = @Server(
                url = "http://localhost:8086/"
        ))
public class CommentController {
    @Autowired
    private CommentService commentService;

    @Operation(summary = "Créer un commentaire",
            description = "Cette méthode permet de créer un nouveau commentaire pour une tâche spécifique dans un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Commentaire créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide")
    })
    @PostMapping("/createComment")
    public ResponseEntity<CommentDTO> createComment(
            @RequestBody Comment comment,
            @RequestHeader("Authorization") String token) {
        CommentDTO commentDTO = commentService.createComment(comment, token);
        return ResponseEntity.ok(commentDTO);
    }

    @Operation(summary = "Récupérer les commentaires d'une tâche",
            description = "Cette méthode permet de récupérer la liste des commentaires associés à une tâche spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des commentaires récupérée avec succès"),
            @ApiResponse(responseCode = "404", description = "Tâche non trouvée")
    })
    @GetMapping("/getComment/{workItemId}")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Long workItemId) {
        List<CommentDTO> commentDTOs = commentService.getCommentsByWorkItemId(workItemId);
        return ResponseEntity.ok(commentDTOs);
    }
}