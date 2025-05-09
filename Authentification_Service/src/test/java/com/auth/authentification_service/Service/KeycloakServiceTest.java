package com.auth.authentification_service.Service;

import com.auth.authentification_service.DTO.ProjectMemberDTO;
import com.auth.authentification_service.DTO.UserDto;
import com.auth.authentification_service.Entity.Invitation;
import com.auth.authentification_service.Entity.ProjectMember;
import com.auth.authentification_service.Entity.ProjectMemberId;
import com.auth.authentification_service.Repository.InvitationRepository;
import com.auth.authentification_service.Repository.ProjectMemberRepository;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class KeycloakServiceTest {

    @Mock
    private VaultService vaultService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private KeycloakService keycloakService;

    @Spy
    @InjectMocks
    private KeycloakService keycloakServiceSpy;

    private static final String DUMMY_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTEyMyJ9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        keycloakService = new KeycloakService(vaultService, restTemplate, invitationRepository, projectMemberRepository);
        keycloakServiceSpy = spy(keycloakService);
        setField(keycloakService, "keycloakUrl", "http://keycloak");
        setField(keycloakService, "keycloakRealm", "my-realm");
        setField(keycloakService, "keycloakClientId", "my-client");
        setField(keycloakService, "projectServiceUrl", "http://project-service");
        setField(keycloakServiceSpy, "keycloakUrl", "http://keycloak");
        setField(keycloakServiceSpy, "keycloakRealm", "my-realm");
        setField(keycloakServiceSpy, "keycloakClientId", "my-client");
        setField(keycloakServiceSpy, "projectServiceUrl", "http://project-service");

        // Ensure vaultService.getClientSecret() never returns null
        when(vaultService.getClientSecret()).thenReturn("secret123");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la configuration du champ : " + fieldName, e);
        }
    }

    private void setupAdminTokenMock(String clientSecret, String accessToken) {
        when(vaultService.getClientSecret()).thenReturn(clientSecret);
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("access_token", accessToken);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(tokenMap, HttpStatus.OK);
        when(restTemplate.postForEntity(
                eq("http://keycloak/realms/my-realm/protocol/openid-connect/token"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);
    }

    private void setupUserCreationMock(String userId, String locationHeader) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.LOCATION, locationHeader);
        when(restTemplate.postForEntity(
                eq("http://keycloak/admin/realms/my-realm/users"),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>("Created", responseHeaders, HttpStatus.CREATED));
    }

    private void setupAssignRoleToUserMock(String userId, String role, String accessToken, String rolesJson, HttpStatus rolesStatus, HttpStatus roleMappingStatus) {
        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(rolesJson, rolesStatus));

        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/role-mappings/realm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>(null, roleMappingStatus));
    }

    private void invokeAssignRoleToUser(String userId, String roleName, String accessToken) {
        try {
            Method method = KeycloakService.class.getDeclaredMethod("assignRoleToUser", String.class, String.class, String.class);
            method.setAccessible(true);
            method.invoke(keycloakServiceSpy, userId, roleName, accessToken);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new RuntimeException("Erreur lors de l'invocation de assignRoleToUser", cause);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'invocation de assignRoleToUser", e);
        }
    }

    @Test
    public void testGetAdminToken_Success() {
        String clientSecret = "secret123";
        String expectedToken = "admin-token-123";

        when(vaultService.getClientSecret()).thenReturn(clientSecret);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access_token", expectedToken);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(
                eq("http://keycloak/realms/my-realm/protocol/openid-connect/token"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        String actualToken = keycloakService.getAdminToken();

        assertEquals(expectedToken, actualToken, "Le token retourné doit correspondre au token attendu");
        verify(vaultService, times(1)).getClientSecret();
        verify(restTemplate, times(1)).postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    public void testGetAdminToken_Failure_Non200Status() {
        String clientSecret = "secret123";

        when(vaultService.getClientSecret()).thenReturn(clientSecret);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        when(restTemplate.postForEntity(
                eq("http://keycloak/realms/my-realm/protocol/openid-connect/token"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keycloakService.getAdminToken(),
                "Une exception doit être levée si la requête échoue"
        );
        assertEquals("Impossible de récupérer le token d'admin Keycloak", exception.getMessage());
        verify(vaultService, times(1)).getClientSecret();
        verify(restTemplate, times(1)).postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    public void testGetAdminToken_Failure_NullResponseBody() {
        String clientSecret = "secret123";

        when(vaultService.getClientSecret()).thenReturn(clientSecret);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.postForEntity(
                eq("http://keycloak/realms/my-realm/protocol/openid-connect/token"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keycloakService.getAdminToken(),
                "Une exception doit être levée si la réponse est vide"
        );
        assertEquals("Impossible de récupérer le token d'admin Keycloak", exception.getMessage());
        verify(vaultService, times(1)).getClientSecret();
        verify(restTemplate, times(1)).postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    void testCreateUser_WithValidDataAndInvitation() {
        String accessToken = "fake-admin-token";
        String clientSecret = "fake-secret";
        String userId = "new-user-id";
        String token = "valid-invitation-token";

        UserDto userDto = new UserDto();
        userDto.setUsername("testuser");
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setEmail("test@example.com");
        userDto.setPassword("password123");
        userDto.setToken(token);

        Invitation invitation = new Invitation();
        invitation.setToken(token);
        invitation.setRole("PROJECT_MANAGER");
        invitation.setProjectId(2L);
        invitation.setUsed(false);
        invitation.setExpiresAt(System.currentTimeMillis() + 10000);

        String locationHeader = "http://localhost:8080/auth/admin/realms/myrealm/users/" + userId;

        setupAdminTokenMock(clientSecret, accessToken);
        setupUserCreationMock(userId, locationHeader);
        setupAssignRoleToUserMock(userId, "PROJECT_MANAGER", accessToken,
                "[{\"id\": \"role-id-123\", \"name\": \"PROJECT_MANAGER\"}]",
                HttpStatus.OK, HttpStatus.NO_CONTENT);

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(projectMemberRepository.existsByIdProjectIdAndIdUserId(invitation.getProjectId(), userId)).thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<String> response = keycloakService.createUser(userDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Utilisateur créé avec succès", response.getBody());
        verify(restTemplate, times(1)).postForEntity(
                eq("http://keycloak/admin/realms/my-realm/users"),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/role-mappings/realm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(projectMemberRepository, times(1)).save(any(ProjectMember.class));
        verify(invitationRepository, times(1)).save(any(Invitation.class));
    }

    @Test
    void testCreateUser_WithInvalidToken() {
        String token = "invalid-token";
        String clientSecret = "secret123";
        String accessToken = "fake-admin-token";
        String userId = "user-id";
        String locationHeader = "http://localhost:8080/users/" + userId;

        UserDto userDto = new UserDto();
        userDto.setUsername("testuser");
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setEmail("test@example.com");
        userDto.setPassword("password123");
        userDto.setToken(token);

        setupAdminTokenMock(clientSecret, accessToken);
        setupUserCreationMock(userId, locationHeader);
        when(invitationRepository.findByToken(token)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keycloakService.createUser(userDto),
                "Une RuntimeException doit être levée pour un jeton invalide"
        );

        assertTrue(exception.getMessage().contains("Jeton d'invitation invalide"), "Le message d'erreur doit indiquer un jeton invalide");
        verify(restTemplate, times(1)).postForEntity(
                eq("http://keycloak/admin/realms/my-realm/users"),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, never()).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void testCreateUser_WithExpiredInvitation() {
        String token = "expired-token";
        String clientSecret = "secret123";
        String accessToken = "fake-admin-token";
        String userId = "user-id";
        String locationHeader = "http://localhost:8080/users/" + userId;

        UserDto userDto = new UserDto();
        userDto.setUsername("testuser");
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setEmail("test@example.com");
        userDto.setPassword("password123");
        userDto.setToken(token);

        Invitation invitation = new Invitation();
        invitation.setToken(token);
        invitation.setExpiresAt(System.currentTimeMillis() - 10000);
        invitation.setUsed(false);

        setupAdminTokenMock(clientSecret, accessToken);
        setupUserCreationMock(userId, locationHeader);
        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keycloakService.createUser(userDto),
                "Une RuntimeException doit être levée pour une invitation expirée"
        );

        assertTrue(exception.getMessage().contains("Lien d'invitation expiré"), "Le message d'erreur doit indiquer une invitation expirée");
        verify(restTemplate, times(1)).postForEntity(
                eq("http://keycloak/admin/realms/my-realm/users"),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, never()).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void testCreateUser_WithAlreadyUsedInvitation() {
        String token = "used-token";
        String clientSecret = "secret123";
        String accessToken = "fake-admin-token";
        String userId = "user-id";
        String locationHeader = "http://localhost:8080/users/" + userId;

        UserDto userDto = new UserDto();
        userDto.setUsername("testuser");
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setEmail("test@example.com");
        userDto.setPassword("password123");
        userDto.setToken(token);

        Invitation invitation = new Invitation();
        invitation.setToken(token);
        invitation.setUsed(true);
        invitation.setExpiresAt(System.currentTimeMillis() + 10000);

        setupAdminTokenMock(clientSecret, accessToken);
        setupUserCreationMock(userId, locationHeader);
        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keycloakService.createUser(userDto),
                "Une RuntimeException doit être levée pour une invitation déjà utilisée"
        );

        assertTrue(exception.getMessage().contains("L'invitation a déjà été utilisée"), "Le message d'erreur doit indiquer une invitation déjà utilisée");
        verify(restTemplate, times(1)).postForEntity(
                eq("http://keycloak/admin/realms/my-realm/users"),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, never()).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void testCreateUser_UserAlreadyProjectMember() {
        String token = "token";
        String userId = "existing-user-id";
        String clientSecret = "secret";
        String accessToken = "fake-admin-token";
        String locationHeader = "http://localhost:8080/users/" + userId;

        UserDto userDto = new UserDto();
        userDto.setUsername("testuser");
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setEmail("test@example.com");
        userDto.setPassword("password123");
        userDto.setToken(token);

        Invitation invitation = new Invitation();
        invitation.setToken(token);
        invitation.setProjectId(1L);
        invitation.setRole("PROJECT_MANAGER");
        invitation.setUsed(false);
        invitation.setExpiresAt(System.currentTimeMillis() + 10000);

        setupAdminTokenMock(clientSecret, accessToken);
        setupUserCreationMock(userId, locationHeader);
        setupAssignRoleToUserMock(userId, "PROJECT_MANAGER", accessToken,
                "[{\"id\": \"role-id-123\", \"name\": \"PROJECT_MANAGER\"}]",
                HttpStatus.OK, HttpStatus.NO_CONTENT);

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(projectMemberRepository.existsByIdProjectIdAndIdUserId(invitation.getProjectId(), userId)).thenReturn(true);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keycloakService.createUser(userDto),
                "Une RuntimeException doit être levée si l'utilisateur est déjà membre du projet"
        );

        assertTrue(exception.getMessage().contains("L'utilisateur est déjà membre de ce projet"), "Le message d'erreur doit indiquer que l'utilisateur est déjà membre");
        verify(restTemplate, times(1)).postForEntity(
                eq("http://keycloak/admin/realms/my-realm/users"),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/role-mappings/realm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
        verify(invitationRepository, times(1)).save(any(Invitation.class));
    }

    @Test
    void testAssignRoleToUser_Success() {
        String userId = "user-123";
        String roleName = "PROJECT_MANAGER";
        String accessToken = "fake-access-token";
        String rolesJson = "[{\"id\": \"role-id-123\", \"name\": \"PROJECT_MANAGER\"}]";

        setupAssignRoleToUserMock(userId, roleName, accessToken, rolesJson, HttpStatus.OK, HttpStatus.NO_CONTENT);

        assertDoesNotThrow(() -> invokeAssignRoleToUser(userId, roleName, accessToken),
                "L'attribution du rôle doit réussir sans exception");

        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/role-mappings/realm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void testAssignRoleToUser_RoleNotFound() {
        String userId = "user-123";
        String roleName = "INVALID_ROLE";
        String accessToken = "fake-access-token";
        String rolesJson = "[{\"id\": \"role-id-123\", \"name\": \"PROJECT_MANAGER\"}]";

        setupAssignRoleToUserMock(userId, roleName, accessToken, rolesJson, HttpStatus.OK, HttpStatus.NO_CONTENT);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokeAssignRoleToUser(userId, roleName, accessToken),
                "Une RuntimeException doit être levée si le rôle n'est pas trouvé"
        );

        assertEquals("Rôle INVALID_ROLE non trouvé.", exception.getMessage(),
                "Le message d'erreur doit indiquer que le rôle n'a pas été trouvé");
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, never()).exchange(
                eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/role-mappings/realm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void testAssignRoleToUser_FailedToRetrieveRoles() {
        String userId = "user-123";
        String roleName = "PROJECT_MANAGER";
        String accessToken = "fake-access-token";
        String rolesJson = "{\"error\": \"Internal Server Error\"}";

        setupAssignRoleToUserMock(userId, roleName, accessToken, rolesJson, HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.NO_CONTENT);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokeAssignRoleToUser(userId, roleName, accessToken),
                "Une RuntimeException doit être levée si la récupération des rôles échoue"
        );

        assertEquals("Erreur lors de la récupération des rôles.", exception.getMessage(),
                "Le message d'erreur doit indiquer une erreur de récupération des rôles");
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, never()).exchange(
                eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/role-mappings/realm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void testAssignRoleToUser_FailedToAssignRole() {
        String userId = "user-123";
        String roleName = "PROJECT_MANAGER";
        String accessToken = "fake-access-token";
        String rolesJson = "[{\"id\": \"role-id-123\", \"name\": \"PROJECT_MANAGER\"}]";

        setupAssignRoleToUserMock(userId, roleName, accessToken, rolesJson, HttpStatus.OK, HttpStatus.BAD_REQUEST);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokeAssignRoleToUser(userId, roleName, accessToken),
                "Une RuntimeException doit être levée si l'attribution du rôle échoue"
        );

        assertEquals("Échec de l'attribution du rôle : PROJECT_MANAGER", exception.getMessage(),
                "Le message d'erreur doit indiquer une erreur d'attribution du rôle");
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/role-mappings/realm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void testAssignRoleToUser_InvalidRolesResponse() {
        String userId = "user-123";
        String roleName = "PROJECT_MANAGER";
        String accessToken = "fake-access-token";
        String rolesJson = "invalid-json";

        setupAssignRoleToUserMock(userId, roleName, accessToken, rolesJson, HttpStatus.OK, HttpStatus.NO_CONTENT);

        doReturn(null).when(keycloakServiceSpy).extractRoleIdFromResponse(anyString(), eq(roleName));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> invokeAssignRoleToUser(userId, roleName, accessToken),
                "Une RuntimeException doit être levée si la réponse des rôles est invalide"
        );

        assertEquals("Rôle PROJECT_MANAGER non trouvé.", exception.getMessage(),
                "Le message d'erreur doit indiquer que le rôle n'a pas été trouvé");
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/roles"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
        verify(restTemplate, never()).exchange(
                eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/role-mappings/realm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void testGetTeamMembers_Success() {
        String adminToken = "admin-token";

        ProjectMember member = new ProjectMember();
        ProjectMemberId memberId = new ProjectMemberId(1L, "user-456");
        member.setId(memberId);
        member.setRoleInProject("DEVELOPER");
        when(projectMemberRepository.findAll()).thenReturn(List.of(member));

        setupAdminTokenMock("secret123", adminToken);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", "user-456");
        userInfo.put("firstName", "Jane");
        userInfo.put("lastName", "Doe");
        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/users"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(List.of(userInfo), HttpStatus.OK));

        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("name", "Project Alpha");
        when(restTemplate.getForEntity(
                eq("http://project-service/api/projects/1"),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(projectInfo, HttpStatus.OK));

        List<Map<String, Object>> result = keycloakService.getTeamMembers(DUMMY_JWT);

        assertEquals(1, result.size());
        Map<String, Object> teamMember = result.get(0);
        assertEquals("user-456", teamMember.get("id"));
        assertEquals("Jane", teamMember.get("firstName"));
        assertEquals("Doe", teamMember.get("lastName"));
        assertEquals("DEVELOPER", teamMember.get("role"));
        assertEquals("Project Alpha", teamMember.get("project"));
        assertEquals("https://ui-avatars.com/api/?name=J+D", teamMember.get("avatar"));
        verify(projectMemberRepository, times(1)).findAll();
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/users"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
        verify(restTemplate, times(1)).getForEntity(
                eq("http://project-service/api/projects/1"),
                eq(Map.class)
        );
    }

    @Test
    void testGetTeamMembers_NullToken() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keycloakService.getTeamMembers(null),
                "Une exception doit être levée si le token est null"
        );

        assertEquals("Token JWT invalide", exception.getMessage());
        verify(projectMemberRepository, never()).findAll();
        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    void testGetTeamMembers_NoUsersFound() {
        String adminToken = "admin-token";

        when(projectMemberRepository.findAll()).thenReturn(List.of());
        setupAdminTokenMock("secret123", adminToken);

        List<Map<String, Object>> result = keycloakService.getTeamMembers(DUMMY_JWT);

        assertTrue(result.isEmpty(), "La liste doit être vide si aucun membre n'est trouvé");
        verify(projectMemberRepository, times(1)).findAll();
        verify(restTemplate, never()).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void testGetTeamMembers_KeycloakError() {
        String adminToken = "admin-token";

        ProjectMember member = new ProjectMember();
        ProjectMemberId memberId = new ProjectMemberId(1L, "user-456");
        member.setId(memberId);
        member.setRoleInProject("DEVELOPER");
        when(projectMemberRepository.findAll()).thenReturn(List.of(member));

        setupAdminTokenMock("secret123", adminToken);

        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/users"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Keycloak error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keycloakService.getTeamMembers(DUMMY_JWT),
                "Une exception doit être levée en cas d'erreur Keycloak"
        );

        assertEquals("Erreur lors de la récupération des utilisateurs depuis Keycloak", exception.getMessage());
        verify(projectMemberRepository, times(1)).findAll();
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/users"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void testGetTeamMembersbyProject_Success() {
        String projectId = "1";
        String adminToken = "admin-token";

        ProjectMember member = new ProjectMember();
        ProjectMemberId memberId = new ProjectMemberId(1L, "user-456");
        member.setId(memberId);
        member.setRoleInProject("DEVELOPER");
        when(projectMemberRepository.findByIdProjectId(1L)).thenReturn(List.of(member));

        setupAdminTokenMock("secret123", adminToken);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("firstName", "Jane");
        userInfo.put("lastName", "Doe");
        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(userInfo, HttpStatus.OK));

        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("name", "Project Alpha");
        when(restTemplate.getForEntity(
                eq("http://project-service/api/projects/1"),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(projectInfo, HttpStatus.OK));

        List<Map<String, Object>> result = keycloakService.getTeamMembersbyProject(DUMMY_JWT, projectId);

        assertEquals(1, result.size());
        Map<String, Object> teamMember = result.get(0);
        assertEquals("user-456", teamMember.get("id"));
        assertEquals("Jane", teamMember.get("firstName"));
        assertEquals("Doe", teamMember.get("lastName"));
        assertEquals("DEVELOPER", teamMember.get("role"));
        assertEquals("Project Alpha", teamMember.get("project"));
        assertEquals("https://ui-avatars.com/api/?name=J+D", teamMember.get("avatar"));
        verify(projectMemberRepository, times(1)).findByIdProjectId(1L);
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
        verify(restTemplate, times(1)).getForEntity(
                eq("http://project-service/api/projects/1"),
                eq(Map.class)
        );
    }

    @Test
    void testGetTeamMembersbyProject_NullToken() {
        String projectId = "1";

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keycloakService.getTeamMembersbyProject(null, projectId),
                "Une exception doit être levée si le token est null"
        );

        assertEquals("Token JWT invalide", exception.getMessage());
        verify(projectMemberRepository, never()).findByIdProjectId(anyLong());
        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    void testGetTeamMembersbyProject_NoMembers() {
        String projectId = "1";
        String adminToken = "admin-token";

        when(projectMemberRepository.findByIdProjectId(1L)).thenReturn(List.of());
        setupAdminTokenMock("secret123", adminToken);

        List<Map<String, Object>> result = keycloakService.getTeamMembersbyProject(DUMMY_JWT, projectId);

        assertTrue(result.isEmpty(), "La liste doit être vide si aucun membre n'est trouvé");
        verify(projectMemberRepository, times(1)).findByIdProjectId(1L);
        verify(restTemplate, never()).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void testGetTeamMembersbyProject_KeycloakError() {
        String projectId = "1";
        String adminToken = "admin-token";

        ProjectMember member = new ProjectMember();
        ProjectMemberId memberId = new ProjectMemberId(1L, "user-456");
        member.setId(memberId);
        member.setRoleInProject("DEVELOPER");
        when(projectMemberRepository.findByIdProjectId(1L)).thenReturn(List.of(member));

        setupAdminTokenMock("secret123", adminToken);

        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Keycloak error"));

        List<Map<String, Object>> result = keycloakService.getTeamMembersbyProject(DUMMY_JWT, projectId);

        assertTrue(result.isEmpty(), "La liste doit être vide en cas d'erreur Keycloak");
        verify(projectMemberRepository, times(1)).findByIdProjectId(1L);
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void testGetUserDetailsByAuthId_Success() {
        String authId = "user-456";
        String userToken = DUMMY_JWT;
        String adminToken = "admin-token";

        setupAdminTokenMock("secret123", adminToken);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("firstName", "Jane");
        userInfo.put("lastName", "Doe");
        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(userInfo, HttpStatus.OK));

        Map<String, Object> result = keycloakService.getUserDetailsByAuthId(authId, userToken);

        assertEquals("Jane", result.get("firstName"));
        assertEquals("Doe", result.get("lastName"));
        assertEquals("https://ui-avatars.com/api/?name=J+D", result.get("avatar"));
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void testGetUserDetailsByAuthId_NullToken() {
        String authId = "user-456";

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keycloakService.getUserDetailsByAuthId(authId, null),
                "Une exception doit être levée si le token est null"
        );

        assertEquals("Token JWT invalide", exception.getMessage());
        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    void testGetUserDetailsByAuthId_UserNotFound() {
        String authId = "user-456";
        String userToken = DUMMY_JWT;
        String adminToken = "admin-token";

        setupAdminTokenMock("secret123", adminToken);

        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keycloakService.getUserDetailsByAuthId(authId, userToken),
                "Une exception doit être levée si l'utilisateur n'est pas trouvé"
        );

        assertEquals("Erreur lors de la récupération de l'utilisateur depuis Keycloak", exception.getMessage());
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void testGetUserDetailsByAuthId_KeycloakError() {
        String authId = "user-456";
        String userToken = DUMMY_JWT;
        String adminToken = "admin-token";

        setupAdminTokenMock("secret123", adminToken);

        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Keycloak error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> keycloakService.getUserDetailsByAuthId(authId, userToken),
                "Une exception doit être levée en cas d'erreur Keycloak"
        );

        assertEquals("Erreur lors de la récupération de l'utilisateur depuis Keycloak", exception.getMessage());
        verify(restTemplate, times(1)).exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void testGetProjectMembersByUserId_Success() {
        String userId = "user-123";

        ProjectMember member = new ProjectMember();
        ProjectMemberId memberId = new ProjectMemberId(1L, userId);
        member.setId(memberId);
        member.setRoleInProject("DEVELOPER");
        when(projectMemberRepository.findByIdUserId(userId)).thenReturn(List.of(member));

        List<ProjectMemberDTO> result = keycloakService.getProjectMembersByUserId(userId);

        assertEquals(1, result.size());
        ProjectMemberDTO dto = result.get(0);
        assertEquals(1L, dto.getProjectId());
        assertEquals("DEVELOPER", dto.getRoleInProject());
        verify(projectMemberRepository, times(1)).findByIdUserId(userId);
    }

    @Test
    void testGetProjectMembersByUserId_NoMembers() {
        String userId = "user-123";

        when(projectMemberRepository.findByIdUserId(userId)).thenReturn(List.of());

        List<ProjectMemberDTO> result = keycloakService.getProjectMembersByUserId(userId);

        assertTrue(result.isEmpty(), "La liste doit être vide si aucun membre n'est trouvé");
        verify(projectMemberRepository, times(1)).findByIdUserId(userId);
    }

    @Test
    void testGetUsersByIds_Success() {
        List<String> userIds = List.of("user-456", "user-789");
        String adminToken = "admin-token";

        setupAdminTokenMock("secret123", adminToken);

        Map<String, Object> userInfo1 = new HashMap<>();
        userInfo1.put("firstName", "Jane");
        userInfo1.put("lastName", "Doe");
        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(userInfo1, HttpStatus.OK));

        Map<String, Object> userInfo2 = new HashMap<>();
        userInfo2.put("firstName", "John");
        userInfo2.put("lastName", "Smith");
        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-789"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(userInfo2, HttpStatus.OK));

        List<UserDto> result = keycloakService.getUsersByIds(userIds);

        assertEquals(2, result.size());
        UserDto user1 = result.get(0);
        assertEquals("user-456", user1.getId());
        assertEquals("Jane", user1.getFirstName());
        assertEquals("Doe", user1.getLastName());
        UserDto user2 = result.get(1);
        assertEquals("user-789", user2.getId());
        assertEquals("John", user2.getFirstName());
        assertEquals("Smith", user2.getLastName());
        verify(restTemplate, times(2)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void testGetUsersByIds_EmptyList() {
        List<String> userIds = List.of();

        List<UserDto> result = keycloakService.getUsersByIds(userIds);

        assertTrue(result.isEmpty(), "La liste doit être vide pour une entrée vide");
        verify(restTemplate, never()).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void testGetUsersByIds_NullList() {
        List<UserDto> result = keycloakService.getUsersByIds(null);

        assertTrue(result.isEmpty(), "La liste doit être vide pour une entrée null");
        verify(restTemplate, never()).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    void testGetUsersByIds_SomeUsersNotFound() {
        List<String> userIds = List.of("user-456", "user-789");
        String adminToken = "admin-token";

        setupAdminTokenMock("secret123", adminToken);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("firstName", "Jane");
        userInfo.put("lastName", "Doe");
        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(userInfo, HttpStatus.OK));

        when(restTemplate.exchange(
                eq("http://keycloak/admin/realms/my-realm/users/user-789"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        List<UserDto> result = keycloakService.getUsersByIds(userIds);

        assertEquals(1, result.size());
        UserDto user = result.get(0);
        assertEquals("user-456", user.getId());
        assertEquals("Jane", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        verify(restTemplate, times(2)).exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        );
    }
}