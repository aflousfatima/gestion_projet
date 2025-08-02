package com.auth.authentification_service.Service;

import com.auth.authentification_service.Entity.Agenda;
import com.auth.authentification_service.Repository.AgendaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AgendaService {

    private static final Logger logger = LoggerFactory.getLogger(AgendaService.class);

    @Autowired
    private AgendaRepository agendaRepository;

    @Transactional
    public Agenda updateAgenda(String userId, Agenda agenda) {
        logger.info("📥 Réception de la requête pour mettre à jour l'agenda pour userId: {}", userId);
        logger.info("📋 Contenu de l'agenda reçu: {}", agenda);

        Agenda existingAgenda = agendaRepository.findByUserId(userId)
                .orElse(new Agenda());

        existingAgenda.setUserId(userId);
        // S'assurer que les listes ne sont pas null
        List<Agenda.Slot> availableSlots = agenda.getAvailableSlots() != null
                ? agenda.getAvailableSlots()
                : new ArrayList<>();
        List<Agenda.Slot> blockedSlots = agenda.getBlockedSlots() != null
                ? agenda.getBlockedSlots()
                : new ArrayList<>();

        logger.info("📋 availableSlots: {}", availableSlots);
        logger.info("📋 blockedSlots: {}", blockedSlots);

        existingAgenda.setAvailableSlots(availableSlots);
        existingAgenda.setBlockedSlots(blockedSlots);

        // Validation des créneaux
        validateSlots(existingAgenda.getAvailableSlots());
        validateSlots(existingAgenda.getBlockedSlots());

        Agenda savedAgenda = agendaRepository.save(existingAgenda);
        logger.info("✅ Agenda sauvegardé: {}", savedAgenda);
        return savedAgenda;
    }

    public Agenda getAgendaByUserId(String userId) {
        logger.info("🔍 Récupération de l'agenda pour userId: {}", userId);
        Agenda agenda = agendaRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    logger.error("❌ Agenda non trouvé pour userId: {}", userId);
                    return new RuntimeException("Agenda not found for user: " + userId);
                });
        logger.info("✅ Agenda récupéré: {}", agenda);
        return agenda;
    }

    private void validateSlots(List<Agenda.Slot> slots) {
        if (slots == null || slots.isEmpty()) {
            logger.debug("ℹ️ Liste de slots null ou vide, aucune validation nécessaire");
            return;
        }
        logger.debug("🔍 Validation de {} slots", slots.size());
        for (int i = 0; i < slots.size(); i++) {
            Agenda.Slot slot1 = slots.get(i);
            for (int j = i + 1; j < slots.size(); j++) {
                Agenda.Slot slot2 = slots.get(j);
                if (slot1.getDay().equals(slot2.getDay()) &&
                        isOverlapping(slot1.getStartTime(), slot1.getEndTime(),
                                slot2.getStartTime(), slot2.getEndTime())) {
                    logger.error("❌ Chevauchement détecté pour le jour {}: {} - {} et {} - {}",
                            slot1.getDay(), slot1.getStartTime(), slot1.getEndTime(),
                            slot2.getStartTime(), slot2.getEndTime());
                    throw new IllegalArgumentException("Overlapping slots detected on " + slot1.getDay());
                }
            }
        }
    }

    private boolean isOverlapping(String start1, String end1, String start2, String end2) {
        return start1.compareTo(end2) <= 0 && start2.compareTo(end1) <= 0;
    }

    public String extractUserIdFromToken(JwtAuthenticationToken authToken) {
        String userId = authToken.getToken().getSubject();
        logger.info("🔑 UserId extrait du token JWT: {}", userId);
        return userId;
    }
}