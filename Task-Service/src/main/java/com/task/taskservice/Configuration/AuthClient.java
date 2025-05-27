package com.task.taskservice.Configuration;


import com.task.taskservice.DTO.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "auth-service", url = "http://localhost:8083",fallback = AuthClientFallback.class)  // Utilisez un URL dynamique ou un nom de service si vous utilisez Eureka
public interface AuthClient {

    @GetMapping("/api/users/search")
    Map<String, Object> searchUserByName(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestHeader("Authorization") String authorization);


    @GetMapping("/api/users/details")
    Map<String, Map<String, Object>> getUserDetailsByIds(@RequestParam("ids") String ids);


    @GetMapping("/api/auth/users/{authId}")
    Map<String, Object> getUserDetailsByAuthId(
            @PathVariable("authId") String authId,
            @RequestHeader("Authorization") String authorization
    );


    @GetMapping("/api/auth/decode-token")
    String decodeToken(@RequestHeader("Authorization") String authorization);


    @PostMapping("/api/tasks_reponsibles/by-ids")
    List<UserDTO> getUsersByIds(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody List<String> userIds
    );

    // Nouvelle méthode pour décoder le token

}

