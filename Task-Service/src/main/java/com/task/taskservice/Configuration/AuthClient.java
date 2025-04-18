package com.task.taskservice.Configuration;


import com.task.taskservice.DTO.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "auth-service", url = "http://localhost:8083")  // Utilisez un URL dynamique ou un nom de service si vous utilisez Eureka
public interface AuthClient {




    @PostMapping("/api/tasks_reponsibles/by-ids")
    List<UserDTO> getUsersByIds(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody List<String> userIds
    );

    // Nouvelle méthode pour décoder le token
    @GetMapping("/api/auth/decode-token")
    String decodeToken(@RequestHeader("Authorization") String authorization);

}

