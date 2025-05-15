package com.auth.authentification_service.Service;

import com.auth.authentification_service.DTO.KeycloakTokenResponse;
import com.auth.authentification_service.DTO.ProjectMemberDTO;
import com.auth.authentification_service.DTO.TokenDto;
import com.auth.authentification_service.DTO.UserDto;
import com.auth.authentification_service.Entity.Invitation;
import com.auth.authentification_service.Entity.ProjectMember;
import com.auth.authentification_service.Repository.InvitationRepository;
import com.auth.authentification_service.Repository.ProjectMemberRepository;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final String projectServiceUrl = "http://localhost:8085"; // Ajoute cette ligne pour l'URL de project-service

    public KeycloakService(VaultService vaultService, RestTemplate restTemplate, InvitationRepository invitationRepository, ProjectMemberRepository projectMemberRepository) {
        this.vaultService = vaultService;
        this.restTemplate = restTemplate;
        this.invitationRepository = invitationRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @RateLimiter(name = "KeycloakServiceLimiter", fallbackMethod = "getAdminTokenRateLimiterFallback")
    @Bulkhead(name = "KeycloakServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "getAdminTokenBulkheadFallback")
    @Retry(name = "KeycloakServiceRetry", fallbackMethod = "getAdminTokenRetryFallback")
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

    public String getAdminTokenRateLimiterFallback(Throwable t) {
        System.out.println("RateLimiter fallback for getAdminToken: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for admin token retrieval");
    }

    public String getAdminTokenBulkheadFallback(Throwable t) {
        System.out.println("Bulkhead fallback for getAdminToken: " + t.getMessage());
        throw new RuntimeException("Too many concurrent admin token requests");
    }

    public String getAdminTokenRetryFallback(Throwable t) {
        System.out.println("Retry fallback for getAdminToken: " + t.getMessage());
        throw new RuntimeException("Failed to retrieve admin token after retries");
    }


    @RateLimiter(name = "KeycloakServiceLimiter", fallbackMethod = "createUserRateLimiterFallback")
    @Bulkhead(name = "KeycloakServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "createUserBulkheadFallback")
    @Retry(name = "KeycloakServiceRetry", fallbackMethod = "createUserRetryFallback")
    public ResponseEntity<String> createUser(UserDto userDto) {
        System.out.println("Début de la création de l'utilisateur : " + userDto.getUsername());

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
        System.out.println("Envoi de la requête pour créer l'utilisateur à : " + createUserUrl);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(createUserUrl, request, String.class);

            System.out.println("Réponse de Keycloak pour la création : " + response.getStatusCode() + " - " + response.getBody());

            if (response.getStatusCode() != HttpStatus.CREATED) {
                throw new RuntimeException("Échec de la création de l'utilisateur : " + response.getBody());
            }

            // Récupérer l'ID de l'utilisateur créé
            String locationHeader = response.getHeaders().getFirst(HttpHeaders.LOCATION);
            System.out.println("En-tête Location : " + locationHeader);
            String userId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
            System.out.println("ID de l'utilisateur créé : " + userId);

            // Attribuer un rôle à l'utilisateur
            String roleToAssign;
            if (userDto.getToken() != null) {
                System.out.println("Jeton d'invitation détecté : " + userDto.getToken());
                Invitation invitation = invitationRepository.findByToken(userDto.getToken())
                        .orElseThrow(() -> new RuntimeException("Jeton d'invitation invalide"));

                if (invitation.getExpiresAt() < System.currentTimeMillis()) {
                    throw new RuntimeException("Lien d'invitation expiré");
                }

                if (invitation.isUsed()) {
                    throw new RuntimeException("L'invitation a déjà été utilisée");
                }

                roleToAssign = invitation.getRole();
                System.out.println("Rôle à attribuer (depuis l'invitation) : " + roleToAssign);
            } else {
                roleToAssign = "USER";
                System.out.println("Rôle par défaut à attribuer : " + roleToAssign);
            }

            // Attribuer le rôle dans Keycloak
            assignRoleToUser(userId, roleToAssign, accessToken);

            // Si un jeton est présent, marquer l'invitation comme utilisée
            if (userDto.getToken() != null) {
                System.out.println("Marquage de l'invitation comme utilisée...");
                Invitation invitation = invitationRepository.findByToken(userDto.getToken()).get();
                invitation.setUsed(true);
                invitationRepository.save(invitation);
                System.out.println("Invitation marquée comme utilisée avec succès");
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
                System.out.println("Utilisateur ajouté à project_members avec project_id=" + invitation.getProjectId() + ", user_id=" + userId);
            }

            System.out.println("Utilisateur créé avec succès !");
            return ResponseEntity.status(HttpStatus.CREATED).body("Utilisateur créé avec succès");
        } catch (HttpClientErrorException e) {
            // Gérer les erreurs de Keycloak
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                String responseBody = e.getResponseBodyAsString();
                System.out.println("Erreur de Keycloak : " + responseBody);

                // Vérifier si l'erreur est due à un username ou email déjà pris
                if (responseBody.contains("userName") && responseBody.contains("already exists")) {
                    throw new RuntimeException("Le nom d'utilisateur est déjà pris. Veuillez en choisir un autre.");
                } else if (responseBody.contains("email") && responseBody.contains("already exists")) {
                    throw new RuntimeException("L'email est déjà utilisé. Veuillez utiliser un autre email.");
                } else {
                    throw new RuntimeException("Erreur lors de la création de l'utilisateur dans Keycloak : " + responseBody);
                }
            }
            throw e; // Relancer l'exception si ce n'est pas une erreur 400
        }
    }


    public ResponseEntity<String> createUserRateLimiterFallback(UserDto userDto, Throwable t) {
        System.out.println("RateLimiter fallback for createUser: " + t.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded for user creation");
    }

    public ResponseEntity<String> createUserBulkheadFallback(UserDto userDto, Throwable t) {
        System.out.println("Bulkhead fallback for createUser: " + t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Too many concurrent user creation requests");
    }

    public ResponseEntity<String> createUserRetryFallback(UserDto userDto, Throwable t) {
        System.out.println("Retry fallback for createUser: " + t.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create user after retries");
    }

    @RateLimiter(name = "KeycloakServiceLimiter", fallbackMethod = "assignRoleToUserRateLimiterFallback")
    @Bulkhead(name = "KeycloakServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "assignRoleToUserBulkheadFallback")
    @Retry(name = "KeycloakServiceRetry", fallbackMethod = "assignRoleToUserRetryFallback")
    private void assignRoleToUser(String userId, String roleName, String accessToken) {
        System.out.println("Attribution du rôle " + roleName + " à l'utilisateur : " + userId);

        // URL pour récupérer la liste des rôles dans le realm
        String rolesUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/roles";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Requête pour récupérer la liste des rôles
        HttpEntity<String> entity = new HttpEntity<>(headers);
        System.out.println("Récupération des rôles depuis : " + rolesUrl);
        ResponseEntity<String> rolesResponse = restTemplate.exchange(rolesUrl, HttpMethod.GET, entity, String.class);

        System.out.println("Réponse de Keycloak pour les rôles : " + rolesResponse.getStatusCode() + " - " + rolesResponse.getBody());

        if (rolesResponse.getStatusCode() == HttpStatus.OK) {
            // Extraire l'ID du rôle
            String roleId = extractRoleIdFromResponse(rolesResponse.getBody(), roleName);

            if (roleId != null) {
                System.out.println("ID du rôle " + roleName + " : " + roleId);

                // URL de l'API Keycloak pour affecter un rôle à l'utilisateur
                String roleMappingUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId + "/role-mappings/realm";

                // Définir le rôle en utilisant l'ID obtenu dynamiquement
                String roleJson = "[{\"id\": \"" + roleId + "\", \"name\": \"" + roleName + "\"}]";
                System.out.println("Requête d'attribution du rôle : " + roleJson);
                HttpEntity<String> roleMappingEntity = new HttpEntity<>(roleJson, headers);

                // Effectuer la requête pour attribuer le rôle
                ResponseEntity<String> response = restTemplate.exchange(
                        roleMappingUrl,
                        HttpMethod.POST,
                        roleMappingEntity,
                        String.class
                );

                System.out.println("Réponse de Keycloak pour l'attribution du rôle : " + response.getStatusCode() + " - " + response.getBody());

                if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                    System.out.println("Rôle " + roleName + " attribué avec succès !");
                } else {
                    System.out.println("Échec de l'attribution du rôle " + roleName + " : " + response.getBody());
                    throw new RuntimeException("Échec de l'attribution du rôle : " + roleName);
                }
            } else {
                System.out.println("Le rôle " + roleName + " n'a pas été trouvé.");
                throw new RuntimeException("Rôle " + roleName + " non trouvé.");
            }
        } else {
            System.out.println("Erreur lors de la récupération des rôles : " + rolesResponse.getBody());
            throw new RuntimeException("Erreur lors de la récupération des rôles.");
        }
    }
    public void assignRoleToUserRateLimiterFallback(String userId, String roleName, String accessToken, Throwable t) {
        System.out.println("RateLimiter fallback for assignRoleToUser: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for role assignment");
    }

    public void assignRoleToUserBulkheadFallback(String userId, String roleName, String accessToken, Throwable t) {
        System.out.println("Bulkhead fallback for assignRoleToUser: " + t.getMessage());
        throw new RuntimeException("Too many concurrent role assignment requests");
    }

    public void assignRoleToUserRetryFallback(String userId, String roleName, String accessToken, Throwable t) {
        System.out.println("Retry fallback for assignRoleToUser: " + t.getMessage());
        throw new RuntimeException("Failed to assign role after retries");
    }

    public String extractRoleIdFromResponse(String responseBody, String roleName) {
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
            System.out.println("Erreur lors de l'extraction de l'ID du rôle : " + e.getMessage());
            return null;
        }
    }

    @RateLimiter(name = "KeycloakServiceLimiter", fallbackMethod = "getTeamMembersRateLimiterFallback")
    @Bulkhead(name = "KeycloakServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "getTeamMembersBulkheadFallback")
    @Retry(name = "KeycloakServiceRetry", fallbackMethod = "getTeamMembersRetryFallback")
    public List<Map<String, Object>> getTeamMembers(String accessToken) {
        // Décoder le token pour obtenir l'ID de l'utilisateur connecté
        DecodedJWT decodedJWT = JWT.decode(accessToken);
        String userId = decodedJWT.getSubject(); // ID de l'utilisateur connecté

        // Récupérer tous les membres des projets
        List<ProjectMember> projectMembers = projectMemberRepository.findAll();
        List<Map<String, Object>> teamMembers = new ArrayList<>();

        // Obtenir un token admin pour interroger Keycloak
        String adminToken = getAdminToken();

        // Récupérer tous les utilisateurs de Keycloak en une seule requête
        String usersUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    usersUrl, HttpMethod.GET, request, new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    }
            );
            List<Map<String, Object>> allUsers = response.getBody();

            if (allUsers == null) {
                System.out.println("Aucune information d'utilisateur trouvée dans Keycloak");
                return teamMembers;
            }

            // Créer une map pour un accès rapide aux informations des utilisateurs
            Map<String, Map<String, Object>> userInfoMap = new HashMap<>();
            for (Map<String, Object> user : allUsers) {
                String userIdFromKeycloak = (String) user.get("id");
                userInfoMap.put(userIdFromKeycloak, user);
            }

            // Traiter chaque membre de ProjectMember
            for (ProjectMember member : projectMembers) {
                // Ignorer l'utilisateur connecté (optionnel)
                if (member.getId().getUserId().equals(userId)) {
                    continue;
                }

                // Récupérer les informations de l'utilisateur depuis la map
                Map<String, Object> userInfo = userInfoMap.get(member.getId().getUserId());
                if (userInfo == null) {
                    System.out.println("Utilisateur non trouvé dans Keycloak : " + member.getId().getUserId());
                    continue;
                }

                String firstName = (String) userInfo.get("firstName");
                String lastName = (String) userInfo.get("lastName");

                // Vérifier que firstName et lastName ne sont pas null
                if (firstName == null || lastName == null) {
                    System.out.println("firstName ou lastName manquant pour l'utilisateur : " + member.getId().getUserId());
                    firstName = firstName != null ? firstName : "Inconnu";
                    lastName = lastName != null ? lastName : "Inconnu";
                }

                // Récupérer le nom du projet via une requête HTTP au project-service
                String projectName = "Projet Inconnu"; // Valeur par défaut
                try {
                    ResponseEntity<Map> projectResponse = restTemplate.getForEntity(
                            projectServiceUrl + "/api/projects/" + member.getId().getProjectId(),
                            Map.class
                    );
                    if (projectResponse.getStatusCode() == HttpStatus.OK && projectResponse.getBody() != null) {
                        projectName = (String) projectResponse.getBody().get("name");
                    } else {
                        System.out.println(" Projet non trouvé pour l'ID : " + member.getId().getProjectId());
                    }
                } catch (Exception e) {
                    System.out.println("Erreur lors de la récupération du projet " +
                            member.getId().getProjectId() + " : " + e.getMessage());
                }

                // Construire un objet JSON pour le membre
                Map<String, Object> teamMember = new HashMap<>();
                teamMember.put("id", member.getId().getUserId());
                teamMember.put("firstName", firstName);
                teamMember.put("lastName", lastName);
                teamMember.put("role", member.getRoleInProject());
                teamMember.put("project", projectName);
                teamMember.put("avatar", "https://ui-avatars.com/api/?name=" +
                        firstName.charAt(0) + "+" + lastName.charAt(0));

                teamMembers.add(teamMember);
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération des utilisateurs depuis Keycloak : " + e.getMessage());
            throw new RuntimeException("Erreur lors de la récupération des utilisateurs depuis Keycloak", e);
        }

        return teamMembers;
    }


    public List<Map<String, Object>> getTeamMembersRateLimiterFallback(String accessToken, Throwable t) {
        System.out.println("RateLimiter fallback for getTeamMembers: " + t.getMessage());
        return new ArrayList<>();
    }

    public List<Map<String, Object>> getTeamMembersBulkheadFallback(String accessToken, Throwable t) {
        System.out.println("Bulkhead fallback for getTeamMembers: " + t.getMessage());
        return new ArrayList<>();
    }

    public List<Map<String, Object>> getTeamMembersRetryFallback(String accessToken, Throwable t) {
        System.out.println("Retry fallback for getTeamMembers: " + t.getMessage());
        return new ArrayList<>();
    }

    @RateLimiter(name = "KeycloakServiceLimiter", fallbackMethod = "getTeamMembersByProjectRateLimiterFallback")
    @Bulkhead(name = "KeycloakServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "getTeamMembersByProjectBulkheadFallback")
    @Retry(name = "KeycloakServiceRetry", fallbackMethod = "getTeamMembersByProjectRetryFallback")

    public List<Map<String, Object>> getTeamMembersbyProject(String accessToken, String projectId) {
        // Décoder le token pour obtenir l'ID de l'utilisateur connecté
        DecodedJWT decodedJWT = JWT.decode(accessToken);
        String userId = decodedJWT.getSubject(); // ID de l'utilisateur connecté

        // Récupérer les membres du projet spécifié depuis la table project_members
        List<ProjectMember> projectMembers = projectMemberRepository.findByIdProjectId(Long.valueOf(projectId));
        List<Map<String, Object>> teamMembers = new ArrayList<>();

        if (projectMembers.isEmpty()) {
            System.out.println("⚠️ Aucun membre trouvé pour le projet ID : " + projectId);
            return teamMembers;
        }

        // Obtenir un token admin pour interroger Keycloak
        String adminToken = getAdminToken();

        // Traiter chaque membre de ProjectMember
        for (ProjectMember member : projectMembers) {
            // Ignorer l'utilisateur connecté (optionnel)
            if (member.getId().getUserId().equals(userId)) {
                continue;
            }

            // Récupérer les informations de l'utilisateur depuis Keycloak
            String userUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + member.getId().getUserId();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<String> request = new HttpEntity<>(headers);

            try {
                ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                        userUrl, HttpMethod.GET, request, new ParameterizedTypeReference<Map<String, Object>>() {
                        }
                );
                Map<String, Object> userInfo = userResponse.getBody();

                if (userInfo == null) {
                    System.out.println("⚠️ Utilisateur non trouvé dans Keycloak : " + member.getId().getUserId());
                    continue;
                }

                String firstName = (String) userInfo.get("firstName");
                String lastName = (String) userInfo.get("lastName");

                // Vérifier que firstName et lastName ne sont pas null
                if (firstName == null || lastName == null) {
                    System.out.println("⚠️ firstName ou lastName manquant pour l'utilisateur : " + member.getId().getUserId());
                    firstName = firstName != null ? firstName : "Inconnu";
                    lastName = lastName != null ? lastName : "Inconnu";
                }

                // Récupérer le nom du projet via une requête HTTP au project-service
                String projectName = "Projet Inconnu"; // Valeur par défaut
                try {
                    ResponseEntity<Map> projectResponse = restTemplate.getForEntity(
                            projectServiceUrl + "/api/projects/" + member.getId().getProjectId(),
                            Map.class
                    );
                    if (projectResponse.getStatusCode() == HttpStatus.OK && projectResponse.getBody() != null) {
                        projectName = (String) projectResponse.getBody().get("name");
                    } else {
                        System.out.println("⚠️ Projet non trouvé pour l'ID : " + member.getId().getProjectId());
                    }
                } catch (Exception e) {
                    System.out.println("❌ Erreur lors de la récupération du projet " +
                            member.getId().getProjectId() + " : " + e.getMessage());
                }

                // Construire un objet JSON pour le membre
                Map<String, Object> teamMember = new HashMap<>();
                teamMember.put("id", member.getId().getUserId());
                teamMember.put("firstName", firstName);
                teamMember.put("lastName", lastName);
                teamMember.put("role", member.getRoleInProject());
                teamMember.put("project", projectName);
                teamMember.put("avatar", "https://ui-avatars.com/api/?name=" +
                        firstName.charAt(0) + "+" + lastName.charAt(0));

                teamMembers.add(teamMember);
            } catch (Exception e) {
                System.out.println("❌ Erreur lors de la récupération de l'utilisateur " +
                        member.getId().getUserId() + " depuis Keycloak : " + e.getMessage());
            }
        }

        return teamMembers;
    }
    // In AuthenticationService (Authentication Microservice)

    public List<Map<String, Object>> getTeamMembersByProjectRateLimiterFallback(String accessToken, String projectId, Throwable t) {
        System.out.println("RateLimiter fallback for getTeamMembersbyProject: " + t.getMessage());
        return new ArrayList<>();
    }

    public List<Map<String, Object>> getTeamMembersByProjectBulkheadFallback(String accessToken, String projectId, Throwable t) {
        System.out.println("Bulkhead fallback for getTeamMembersbyProject: " + t.getMessage());
        return new ArrayList<>();
    }

    public List<Map<String, Object>> getTeamMembersByProjectRetryFallback(String accessToken, String projectId, Throwable t) {
        System.out.println("Retry fallback for getTeamMembersbyProject: " + t.getMessage());
        return new ArrayList<>();
    }

    @RateLimiter(name = "KeycloakServiceLimiter", fallbackMethod = "getUserDetailsByAuthIdRateLimiterFallback")
    @Bulkhead(name = "KeycloakServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "getUserDetailsByAuthIdBulkheadFallback")
    @Retry(name = "KeycloakServiceRetry", fallbackMethod = "getUserDetailsByAuthIdRetryFallback")
    public Map<String, Object> getUserDetailsByAuthId(String authId, String userToken) {
        String adminToken = getAdminToken(); // Remplace le token utilisateur par un token admin
        String userUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + authId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                    userUrl, HttpMethod.GET, request, new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> userInfo = userResponse.getBody();

            if (userInfo == null) {
                System.out.println("⚠️ Utilisateur non trouvé dans Keycloak : " + authId);
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            String firstName = (String) userInfo.get("firstName");
            String lastName = (String) userInfo.get("lastName");

            if (firstName == null || lastName == null) {
                System.out.println("⚠️ firstName ou lastName manquant pour l'utilisateur : " + authId);
                firstName = firstName != null ? firstName : "Inconnu";
                lastName = lastName != null ? lastName : "Inconnu";
            }

            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("firstName", firstName);
            userDetails.put("lastName", lastName);
            userDetails.put("avatar", "https://ui-avatars.com/api/?name=" +
                    firstName.charAt(0) + "+" + lastName.charAt(0));

            return userDetails;
        } catch (Exception e) {
            System.out.println("❌ Erreur lors de la récupération de l'utilisateur " +
                    authId + " depuis Keycloak : " + e.getMessage());
            throw new RuntimeException("Erreur lors de la récupération de l'utilisateur depuis Keycloak", e);
        }
    }

    public Map<String, Object> getUserDetailsByAuthIdRateLimiterFallback(String authId, String userToken, Throwable t) {
        System.out.println("RateLimiter fallback for getUserDetailsByAuthId: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for user details retrieval");
    }

    public Map<String, Object> getUserDetailsByAuthIdBulkheadFallback(String authId, String userToken, Throwable t) {
        System.out.println("Bulkhead fallback for getUserDetailsByAuthId: " + t.getMessage());
        throw new RuntimeException("Too many concurrent user details requests");
    }

    public Map<String, Object> getUserDetailsByAuthIdRetryFallback(String authId, String userToken, Throwable t) {
        System.out.println("Retry fallback for getUserDetailsByAuthId: " + t.getMessage());
        throw new RuntimeException("Failed to retrieve user details after retries");
    }

    public List<ProjectMemberDTO> getProjectMembersByUserId(String userId) {
        List<ProjectMember> projectMembers = projectMemberRepository.findByIdUserId(userId);
        return projectMembers.stream()
                .map(pm -> new ProjectMemberDTO(
                        pm.getId().getProjectId(),
                        pm.getRoleInProject()
                ))
                .collect(Collectors.toList());
    }

    @RateLimiter(name = "KeycloakServiceLimiter", fallbackMethod = "getUsersByIdsRateLimiterFallback")
    @Bulkhead(name = "KeycloakServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "getUsersByIdsBulkheadFallback")
    @Retry(name = "KeycloakServiceRetry", fallbackMethod = "getUsersByIdsRetryFallback")
    public List<UserDto> getUsersByIds(List<String> userIds) {
        String adminToken = getAdminToken();
        List<UserDto> users = new ArrayList<>();

        if (userIds == null || userIds.isEmpty()) {
            System.out.println("⚠️ Liste d'IDs vide ou null");
            return users;
        }

        for (String userId : userIds) {
            String userUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<String> request = new HttpEntity<>(headers);

            try {
                ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                        userUrl,
                        HttpMethod.GET,
                        request,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                Map<String, Object> userInfo = userResponse.getBody();

                if (userInfo == null) {
                    System.out.println("⚠️ Utilisateur non trouvé dans Keycloak : " + userId);
                    continue;
                }

                String firstName = (String) userInfo.get("firstName");
                String lastName = (String) userInfo.get("lastName");

                // Gérer les cas où firstName ou lastName sont null
                firstName = firstName != null ? firstName : "Inconnu";
                lastName = lastName != null ? lastName : "Inconnu";

                UserDto userDTO = new UserDto();
                userDTO.setId(userId);
                userDTO.setFirstName(firstName);
                userDTO.setLastName(lastName);

                users.add(userDTO);
            } catch (Exception e) {
                System.out.println("❌ Erreur lors de la récupération de l'utilisateur " + userId +
                        " depuis Keycloak : " + e.getMessage());
            }
        }

        return users;
    }
    public List<UserDto> getUsersByIdsRateLimiterFallback(List<String> userIds, Throwable t) {
        System.out.println("RateLimiter fallback for getUsersByIds: " + t.getMessage());
        return new ArrayList<>();
    }

    public List<UserDto> getUsersByIdsBulkheadFallback(List<String> userIds, Throwable t) {
        System.out.println("Bulkhead fallback for getUsersByIds: " + t.getMessage());
        return new ArrayList<>();
    }

    public List<UserDto> getUsersByIdsRetryFallback(List<String> userIds, Throwable t) {
        System.out.println("Retry fallback for getUsersByIds: " + t.getMessage());
        return new ArrayList<>();
    }

    @RateLimiter(name = "KeycloakServiceLimiter", fallbackMethod = "updateUserRateLimiterFallback")
    @Bulkhead(name = "KeycloakServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "updateUserBulkheadFallback")
    @Retry(name = "KeycloakServiceRetry", fallbackMethod = "updateUserRetryFallback")
    public ResponseEntity<String> updateUser(String accessToken, Map<String, Object> userData) {
        System.out.println("🔄 Mise à jour de l'utilisateur avec les données : " + userData);

        // Décoder le token pour obtenir l'ID de l'utilisateur
        DecodedJWT decodedJWT = JWT.decode(accessToken);
        String userId = decodedJWT.getSubject();
        String updateUserUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId;

        // Préparer le payload pour Keycloak
        Map<String, Object> userPayload = new HashMap<>();
        if (userData.containsKey("firstName")) {
            userPayload.put("firstName", userData.get("firstName"));
        }
        if (userData.containsKey("lastName")) {
            userPayload.put("lastName", userData.get("lastName"));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAdminToken());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userPayload, headers);

        // Mettre à jour l'utilisateur dans Keycloak
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    updateUserUrl, HttpMethod.PUT, request, String.class);

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                System.out.println("✅ Utilisateur mis à jour dans Kernel : " + userId);
                return ResponseEntity.ok("Profil mis à jour avec succès");
            } else {
                System.out.println("❌ Échec de la mise à jour de l'utilisateur : " + response.getBody());
                return ResponseEntity.status(response.getStatusCode())
                        .body("Échec de la mise à jour du profil");
            }
        } catch (HttpClientErrorException e) {
            System.out.println("❌ Erreur Keycloak : " + e.getResponseBodyAsString());
            throw new RuntimeException("Erreur lors de la mise à jour de l'utilisateur : " + e.getMessage());
        }
    }

    public ResponseEntity<String> updateUserRateLimiterFallback(String accessToken, Map<String, Object> userData, Throwable t) {
        System.out.println("RateLimiter fallback for updateUser: " + t.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded for user update");
    }

    public ResponseEntity<String> updateUserBulkheadFallback(String accessToken, Map<String, Object> userData, Throwable t) {
        System.out.println("Bulkhead fallback for updateUser: " + t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Too many concurrent user update requests");
    }

    public ResponseEntity<String> updateUserRetryFallback(String accessToken, Map<String, Object> userData, Throwable t) {
        System.out.println("Retry fallback for updateUser: " + t.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update user after retries");
    }

    @RateLimiter(name = "KeycloakServiceLimiter", fallbackMethod = "changePasswordRateLimiterFallback")
    @Bulkhead(name = "KeycloakServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "changePasswordBulkheadFallback")
    @Retry(name = "KeycloakServiceRetry", fallbackMethod = "changePasswordRetryFallback")
    public ResponseEntity<String> changePassword(String accessToken, Map<String, String> passwordData) {
        System.out.println("🔄 Changement de mot de passe pour l'utilisateur");

        // Décoder le token pour obtenir l'ID et l'email de l'utilisateur
        DecodedJWT decodedJWT = JWT.decode(accessToken);
        String userId = decodedJWT.getSubject();
        String email = decodedJWT.getClaim("email").asString();

        if (email == null) {
            System.out.println("❌ Email non trouvé dans le token");
            return ResponseEntity.badRequest().body("Email non trouvé dans le token");
        }

        // Valider le mot de passe actuel
        String currentPassword = passwordData.get("currentPassword");
        String newPassword = passwordData.get("newPassword");

        try {
            authenticateUser(email, currentPassword);
            System.out.println("✅ Mot de passe actuel validé");
        } catch (Exception e) {
            System.out.println("❌ Mot de passe actuel invalide : " + e.getMessage());
            return ResponseEntity.badRequest().body("Mot de passe actuel incorrect");
        }

        // Préparer le payload pour changer le mot de passe
        String resetPasswordUrl = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId + "/reset-password";
        Map<String, Object> passwordPayload = new HashMap<>();
        passwordPayload.put("type", "password");
        passwordPayload.put("value", newPassword);
        passwordPayload.put("temporary", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAdminToken());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(passwordPayload, headers);

        // Changer le mot de passe dans Keycloak
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    resetPasswordUrl, HttpMethod.PUT, request, String.class);

            if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
                System.out.println("✅ Mot de passe changé pour l'utilisateur : " + userId);
                return ResponseEntity.ok("Mot de passe changé avec succès");
            } else {
                System.out.println("❌ Échec du changement de mot de passe : " + response.getBody());
                return ResponseEntity.status(response.getStatusCode())
                        .body("Échec du changement de mot de passe");
            }
        } catch (HttpClientErrorException e) {
            System.out.println("❌ Erreur Keycloak : " + e.getResponseBodyAsString());
            throw new RuntimeException("Erreur lors du changement de mot de passe : " + e.getMessage());
        }
    }
    public ResponseEntity<String> changePasswordRateLimiterFallback(String accessToken, Map<String, String> passwordData, Throwable t) {
        System.out.println("RateLimiter fallback for changePassword: " + t.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded for password change");
    }

    public ResponseEntity<String> changePasswordBulkheadFallback(String accessToken, Map<String, String> passwordData, Throwable t) {
        System.out.println("Bulkhead fallback for changePassword: " + t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Too many concurrent password change requests");
    }

    public ResponseEntity<String> changePasswordRetryFallback(String accessToken, Map<String, String> passwordData, Throwable t) {
        System.out.println("Retry fallback for changePassword: " + t.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to change password after retries");
    }

    @RateLimiter(name = "KeycloakServiceLimiter", fallbackMethod = "authenticateUserRateLimiterFallback")
    @Bulkhead(name = "KeycloakServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "authenticateUserBulkheadFallback")
    @Retry(name = "KeycloakServiceRetry", fallbackMethod = "authenticateUserRetryFallback")
    public TokenDto authenticateUser(String email, String password) throws Exception {
        System.out.println("Récupération du client secret depuis Vault...");
        String keycloakClientSecret = vaultService.getClientSecret();
        System.out.println("Client Secret from Vault: " + keycloakClientSecret);
        String tokenUrl = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";
        System.out.println("URL de token : " + tokenUrl);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", keycloakClientId);
        params.add("client_secret", keycloakClientSecret);
        params.add("username", email);
        params.add("password", password);
        params.add("grant_type", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        System.out.println("Envoi de la requête POST à Keycloak...");
        ResponseEntity<KeycloakTokenResponse> responseEntity = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, entity, KeycloakTokenResponse.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            System.out.println("Tokens récupérés avec succès !");
            KeycloakTokenResponse tokenResponse = responseEntity.getBody();

            return new TokenDto(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
        } else {
            System.out.println("Échec de l'authentification avec Keycloak !");
            throw new Exception("Échec de l'authentification avec Keycloak");
        }
    }

    public TokenDto authenticateUserRateLimiterFallback(String email, String password, Throwable t) {
        System.out.println("RateLimiter fallback for authenticateUser: " + t.getMessage());
        return new TokenDto("", "");
    }

    public TokenDto authenticateUserBulkheadFallback(String email, String password, Throwable t) {
        System.out.println("Bulkhead fallback for authenticateUser: " + t.getMessage());
        return new TokenDto("", "");
    }

    public TokenDto authenticateUserRetryFallback(String email, String password, Throwable t) {
        System.out.println("Retry fallback for authenticateUser: " + t.getMessage());
        return new TokenDto("", "");
    }

}
