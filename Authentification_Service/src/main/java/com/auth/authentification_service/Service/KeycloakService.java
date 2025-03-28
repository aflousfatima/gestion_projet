package com.auth.authentification_service.Service;

import com.auth.authentification_service.DTO.UserDto;
import com.auth.authentification_service.Entity.Invitation;
import com.auth.authentification_service.Entity.ProjectMember;
import com.auth.authentification_service.Repository.InvitationRepository;
import com.auth.authentification_service.Repository.ProjectMemberRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class KeycloakService {
    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.resource}")
    private String keycloakClientId;

    private final VaultService vaultService;
    private final RestTemplate restTemplate;
    private final InvitationRepository invitationRepository;
    private final ProjectMemberRepository projectMemberRepository;


    public KeycloakService(VaultService vaultService, RestTemplate restTemplate , InvitationRepository invitationRepository,ProjectMemberRepository projectMemberRepository) {
        this.vaultService = vaultService;
        this.restTemplate = restTemplate;
        this.invitationRepository = invitationRepository;
        this.projectMemberRepository=projectMemberRepository;
    }

    public String getAdminToken() {
        String clientSecret = vaultService.getClientSecret();
        System.out.println("Client Secret utilisé : " + clientSecret);
        String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";

        System.out.println("URL pour récupérer le token : " + tokenUrl);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("client_id", keycloakClientId);
        params.add("client_secret", clientSecret);
        System.out.println("Paramètres envoyés : " + params);

        HttpHeaders headers = new HttpHeaders();
        headers.forEach((key, value) -> System.out.println("Header : " + key + " = " + value));

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        System.out.println("Headers envoyés après ajout : " + headers);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        System.out.println("Envoi de la requête pour récupérer le token...");

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        System.out.println("Statut de la réponse : " + response.getStatusCode());

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            System.out.println("Token récupéré avec succès !");
            return response.getBody().get("access_token").toString();
        }

        System.out.println("Erreur lors de la récupération du token !");
        throw new RuntimeException("Impossible de récupérer le token d'admin Keycloak");
    }

    public ResponseEntity<String> createUser(UserDto userDto) {
        System.out.println("🔄 Début de la création de l'utilisateur : " + userDto.getUsername());

        String accessToken = getAdminToken();
        String createUserUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users";

        // Créer le payload pour l'utilisateur
        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("username", userDto.getUsername());
        userPayload.put("firstName", userDto.getFirstName());
        userPayload.put("lastName", userDto.getLastName());
        userPayload.put("email", userDto.getEmail());
        userPayload.put("enabled", true);

        Map<String, String> credentials = new HashMap<>();
        credentials.put("type", "password");
        credentials.put("value", userDto.getPassword());
        credentials.put("temporary", "false");

        userPayload.put("credentials", new Map[]{credentials});

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userPayload, headers);

        // Créer l'utilisateur dans Keycloak
        System.out.println("📤 Envoi de la requête pour créer l'utilisateur à : " + createUserUrl);
        ResponseEntity<String> response = restTemplate.postForEntity(createUserUrl, request, String.class);

        System.out.println("📥 Réponse de Keycloak pour la création : " + response.getStatusCode() + " - " + response.getBody());

        if (response.getStatusCode() != HttpStatus.CREATED) {
            throw new RuntimeException("Échec de la création de l'utilisateur : " + response.getBody());
        }

        // Récupérer l'ID de l'utilisateur créé
        String locationHeader = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        System.out.println("📍 En-tête Location : " + locationHeader);
        String userId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
        System.out.println("🆔 ID de l'utilisateur créé : " + userId);

        // Attribuer un rôle à l'utilisateur
        String roleToAssign;
        if (userDto.getToken() != null) {
            System.out.println("🔑 Jeton d'invitation détecté : " + userDto.getToken());
            Invitation invitation = invitationRepository.findByToken(userDto.getToken())
                    .orElseThrow(() -> new RuntimeException("Jeton d'invitation invalide"));

            if (invitation.getExpiresAt() < System.currentTimeMillis()) {
                throw new RuntimeException("Lien d'invitation expiré");
            }

            if (invitation.isUsed()) {
                throw new RuntimeException("L'invitation a déjà été utilisée");
            }

            roleToAssign = invitation.getRole();
            System.out.println("🎭 Rôle à attribuer (depuis l'invitation) : " + roleToAssign);
        } else {
            roleToAssign = "USER";
            System.out.println("🎭 Rôle par défaut à attribuer : " + roleToAssign);
        }

        // Attribuer le rôle dans Keycloak
        assignRoleToUser(userId, roleToAssign, accessToken);

        // Si un jeton est présent, marquer l'invitation comme utilisée
        if (userDto.getToken() != null) {
            System.out.println("✅ Marquage de l'invitation comme utilisée...");
            Invitation invitation = invitationRepository.findByToken(userDto.getToken()).get();
            invitation.setUsed(true);
            invitationRepository.save(invitation);
            System.out.println("✅ Invitation marquée comme utilisée avec succès");
            // Vérifier si l'utilisateur est déjà membre du projet
            if (projectMemberRepository.existsByIdProjectIdAndIdUserId(invitation.getProjectId(), userId)) {
                throw new RuntimeException("L'utilisateur est déjà membre de ce projet");
            }

            // Ajouter l'utilisateur à project_members
            ProjectMember projectMember = new ProjectMember(
                    invitation.getProjectId(),
                    userId,
                    invitation.getRole()
            );
            projectMemberRepository.save(projectMember);
            System.out.println("✅ Utilisateur ajouté à project_members avec project_id=" + invitation.getProjectId() + ", user_id=" + userId);
        }

        System.out.println("✅ Utilisateur créé avec succès !");
        return ResponseEntity.status(HttpStatus.CREATED).body("Utilisateur créé avec succès");
    }

    private void assignRoleToUser(String userId, String roleName, String accessToken) {
        System.out.println("🔄 Attribution du rôle " + roleName + " à l'utilisateur : " + userId);

        // URL pour récupérer la liste des rôles dans le realm
        String rolesUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/roles";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Requête pour récupérer la liste des rôles
        HttpEntity<String> entity = new HttpEntity<>(headers);
        System.out.println("📤 Récupération des rôles depuis : " + rolesUrl);
        ResponseEntity<String> rolesResponse = restTemplate.exchange(rolesUrl, HttpMethod.GET, entity, String.class);

        System.out.println("📥 Réponse de Keycloak pour les rôles : " + rolesResponse.getStatusCode() + " - " + rolesResponse.getBody());

        if (rolesResponse.getStatusCode() == HttpStatus.OK) {
            // Extraire l'ID du rôle
            String roleId = extractRoleIdFromResponse(rolesResponse.getBody(), roleName);

            if (roleId != null) {
                System.out.println("🆔 ID du rôle " + roleName + " : " + roleId);

                // URL de l'API Keycloak pour affecter un rôle à l'utilisateur
                String roleMappingUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId + "/role-mappings/realm";

                // Définir le rôle en utilisant l'ID obtenu dynamiquement
                String roleJson = "[{\"id\": \"" + roleId + "\", \"name\": \"" + roleName + "\"}]";
                System.out.println("📤 Requête d'attribution du rôle : " + roleJson);
                HttpEntity<String> roleMappingEntity = new HttpEntity<>(roleJson, headers);

                // Effectuer la requête pour attribuer le rôle
                ResponseEntity<String> response = restTemplate.exchange(
                        roleMappingUrl,
                        HttpMethod.POST,
                        roleMappingEntity,
                        String.class
                );

                System.out.println("📥 Réponse de Keycloak pour l'attribution du rôle : " + response.getStatusCode() + " - " + response.getBody());

                if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                    System.out.println("✅ Rôle " + roleName + " attribué avec succès !");
                } else {
                    System.out.println("❌ Échec de l'attribution du rôle " + roleName + " : " + response.getBody());
                    throw new RuntimeException("Échec de l'attribution du rôle : " + roleName);
                }
            } else {
                System.out.println("❌ Le rôle " + roleName + " n'a pas été trouvé.");
                throw new RuntimeException("Rôle " + roleName + " non trouvé.");
            }
        } else {
            System.out.println("❌ Erreur lors de la récupération des rôles : " + rolesResponse.getBody());
            throw new RuntimeException("Erreur lors de la récupération des rôles.");
        }
    }

    private String extractRoleIdFromResponse(String responseBody, String roleName) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode roles = mapper.readTree(responseBody);
            for (JsonNode role : roles) {
                if (role.get("name").asText().equals(roleName)) {
                    return role.get("id").asText();
                }
            }
            return null;
        } catch (Exception e) {
            System.out.println("❌ Erreur lors de l'extraction de l'ID du rôle : " + e.getMessage());
            return null;
        }
    }




}
