package com.task.taskservice.Controller;

import com.task.taskservice.DTO.CommentDTO;
import com.task.taskservice.Entity.Comment;
import com.task.taskservice.Service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project/task/comments")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @PostMapping("/createComment")
    public ResponseEntity<CommentDTO> createComment(
            @RequestBody Comment comment,
            @RequestHeader("Authorization") String token) {
        CommentDTO commentDTO = commentService.createComment(comment, token);
        return ResponseEntity.ok(commentDTO);
    }

    @GetMapping("/getComment/{workItemId}")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Long workItemId) {
        List<CommentDTO> commentDTOs = commentService.getCommentsByWorkItemId(workItemId);
        return ResponseEntity.ok(commentDTOs);
    }
}