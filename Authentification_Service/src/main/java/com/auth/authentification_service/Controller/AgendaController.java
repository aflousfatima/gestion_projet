package com.auth.authentification_service.Controller;

import com.auth.authentification_service.Entity.Agenda;
import com.auth.authentification_service.Service.AgendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agenda")
public class AgendaController {

    @Autowired
    private AgendaService agendaService;

    @GetMapping
    public ResponseEntity<Agenda> getAgenda(JwtAuthenticationToken authToken) {
        String userId = agendaService.extractUserIdFromToken(authToken);
        Agenda agenda = agendaService.getAgendaByUserId(userId);
        return ResponseEntity.ok(agenda);
    }

    @PutMapping
    public ResponseEntity<Agenda> updateAgenda(
            @RequestBody Agenda agenda,
            JwtAuthenticationToken authToken) {
        String userId = agendaService.extractUserIdFromToken(authToken);
        Agenda updatedAgenda = agendaService.updateAgenda(userId, agenda);
        return ResponseEntity.ok(updatedAgenda);
    }
}