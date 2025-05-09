package com.auth.authentification_service.Service;

import com.auth.authentification_service.DTO.KeycloakTokenResponse;
import com.auth.authentification_service.DTO.TokenDto;
import com.auth.authentification_service.DTO.UserDto;
import com.auth.authentification_service.Entity.Invitation;
import com.auth.authentification_service.Entity.ProjectMember;
import com.auth.authentification_service.Entity.ProjectMemberId;
import com.auth.authentification_service.Repository.InvitationRepository;
import com.auth.authentification_service.Repository.ProjectMemberRepository;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @BeforeEach
    public void setUp() {
        keycloakService = new KeycloakService(vaultService, restTemplate, invitationRepository, projectMemberRepository);
        keycloakServiceSpy = spy(keycloakService);
        setField(keycloakService, "keycloakUrl", "http://keycloak");
        setField(keycloakService, "keycloakRealm", "my-realm");
        setField(keycloakService, "keycloakClientId", "my-client");
        setField(keycloakServiceSpy, "keycloakUrl", "http://keycloak");
        setField(keycloakServiceSpy, "keycloakRealm", "my-realm");
        setField(keycloakServiceSpy, "keycloakClientId", "my-client");
        setupAdminTokenMock("secret123", "fake-admin-token");
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
        when(restTemplate.postForEntity(
                eq("http://keycloak/realms/my-realm/protocol/openid-connect/token"),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(tokenMap, HttpStatus.OK));
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
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
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
    void testUpdateUser_Success() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        String adminToken = "fake-admin-token";
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", "UpdatedFirstName");
        userData.put("lastName", "UpdatedLastName");

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            doReturn(adminToken).when(keycloakServiceSpy).getAdminToken();

            when(restTemplate.exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(null, HttpStatus.NO_CONTENT));

            ResponseEntity<String> response = keycloakServiceSpy.updateUser(accessToken, userData);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Profil mis à jour avec succès", response.getBody());
            verify(restTemplate, times(1)).exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    @Test
    void testUpdateUser_Failure_Non204Status() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        String adminToken = "fake-admin-token";
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", "UpdatedFirstName");

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            doReturn(adminToken).when(keycloakServiceSpy).getAdminToken();

            when(restTemplate.exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>("Erreur", HttpStatus.BAD_REQUEST));

            ResponseEntity<String> response = keycloakServiceSpy.updateUser(accessToken, userData);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Échec de la mise à jour du profil", response.getBody());
            verify(restTemplate, times(1)).exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    @Test
    void testUpdateUser_HttpClientErrorException() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        String adminToken = "fake-admin-token";
        Map<String, Object> userData = new HashMap<>();
        userData.put("lastName", "UpdatedLastName");

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            doReturn(adminToken).when(keycloakServiceSpy).getAdminToken();

            when(restTemplate.exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Erreur Keycloak"));

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> keycloakServiceSpy.updateUser(accessToken, userData),
                    "Une RuntimeException doit être levée en cas d'erreur HTTP"
            );

            assertTrue(exception.getMessage().contains("Erreur lors de la mise à jour de l'utilisateur"));
            verify(restTemplate, times(1)).exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    @Test
    void testUpdateUser_EmptyUserData() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        String adminToken = "fake-admin-token";
        Map<String, Object> userData = new HashMap<>();

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            doReturn(adminToken).when(keycloakServiceSpy).getAdminToken();

            when(restTemplate.exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(null, HttpStatus.NO_CONTENT));

            ResponseEntity<String> response = keycloakServiceSpy.updateUser(accessToken, userData);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Profil mis à jour avec succès", response.getBody());
            verify(restTemplate, times(1)).exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

    @Test
    void testChangePassword_Success() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        String email = "test@example.com";
        String adminToken = "fake-admin-token";
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("currentPassword", "oldPassword");
        passwordData.put("newPassword", "newPassword");

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            Claim emailClaim = mock(Claim.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            when(decodedJWT.getClaim("email")).thenReturn(emailClaim);
            when(emailClaim.asString()).thenReturn(email);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            doReturn(new TokenDto("access-token", "refresh-token")).when(keycloakServiceSpy).authenticateUser(email, "oldPassword");

            doReturn(adminToken).when(keycloakServiceSpy).getAdminToken();

            when(restTemplate.exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/reset-password"),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>(null, HttpStatus.NO_CONTENT));

            ResponseEntity<String> response = keycloakServiceSpy.changePassword(accessToken, passwordData);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Mot de passe changé avec succès", response.getBody());
            verify(restTemplate, times(1)).exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/reset-password"),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testChangePassword_Failure_NullEmailInToken() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("currentPassword", "oldPassword");
        passwordData.put("newPassword", "newPassword");

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            Claim emailClaim = mock(Claim.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            when(decodedJWT.getClaim("email")).thenReturn(emailClaim);
            when(emailClaim.asString()).thenReturn(null);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            ResponseEntity<String> response = keycloakServiceSpy.changePassword(accessToken, passwordData);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Email non trouvé dans le token", response.getBody());
            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
        }
    }

    @Test
    void testChangePassword_Failure_IncorrectCurrentPassword() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        String email = "test@example.com";
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("currentPassword", "wrongPassword");
        passwordData.put("newPassword", "newPassword");

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            Claim emailClaim = mock(Claim.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            when(decodedJWT.getClaim("email")).thenReturn(emailClaim);
            when(emailClaim.asString()).thenReturn(email);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            doThrow(new RuntimeException("Invalid credentials")).when(keycloakServiceSpy).authenticateUser(email, "wrongPassword");

            ResponseEntity<String> response = keycloakServiceSpy.changePassword(accessToken, passwordData);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Mot de passe actuel incorrect", response.getBody());
            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testChangePassword_Failure_Non204Status() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        String email = "test@example.com";
        String adminToken = "fake-admin-token";
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("currentPassword", "oldPassword");
        passwordData.put("newPassword", "newPassword");

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            Claim emailClaim = mock(Claim.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            when(decodedJWT.getClaim("email")).thenReturn(emailClaim);
            when(emailClaim.asString()).thenReturn(email);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            doReturn(new TokenDto("access-token", "refresh-token")).when(keycloakServiceSpy).authenticateUser(email, "oldPassword");

            doReturn(adminToken).when(keycloakServiceSpy).getAdminToken();

            when(restTemplate.exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/reset-password"),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(new ResponseEntity<>("Erreur", HttpStatus.BAD_REQUEST));

            ResponseEntity<String> response = keycloakServiceSpy.changePassword(accessToken, passwordData);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Échec du changement de mot de passe", response.getBody());
            verify(restTemplate, times(1)).exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/reset-password"),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testChangePassword_HttpClientErrorException() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        String email = "test@example.com";
        String adminToken = "fake-admin-token";
        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("currentPassword", "oldPassword");
        passwordData.put("newPassword", "newPassword");

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            Claim emailClaim = mock(Claim.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            when(decodedJWT.getClaim("email")).thenReturn(emailClaim);
            when(emailClaim.asString()).thenReturn(email);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            doReturn(new TokenDto("access-token", "refresh-token")).when(keycloakServiceSpy).authenticateUser(email, "oldPassword");

            doReturn(adminToken).when(keycloakServiceSpy).getAdminToken();

            when(restTemplate.exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/reset-password"),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Erreur Keycloak"));

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> keycloakServiceSpy.changePassword(accessToken, passwordData),
                    "Une RuntimeException doit être levée en cas d'erreur HTTP"
            );

            assertTrue(exception.getMessage().contains("Erreur lors du changement de mot de passe"));
            verify(restTemplate, times(1)).exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/" + userId + "/reset-password"),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testAuthenticateUser_Success() throws Exception {
        String email = "test@example.com";
        String password = "password123";
        String clientSecret = "secret123";
        String tokenUrl = "http://keycloak/realms/my-realm/protocol/openid-connect/token";
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse();
        tokenResponse.setAccessToken("access-token");
        tokenResponse.setRefreshToken("refresh-token");

        when(vaultService.getClientSecret()).thenReturn(clientSecret);

        when(restTemplate.exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(KeycloakTokenResponse.class)
        )).thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        TokenDto result = keycloakService.authenticateUser(email, password);

        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        verify(restTemplate, times(1)).exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(KeycloakTokenResponse.class)
        );
    }

    @Test
    void testAuthenticateUser_Failure_Non200Status() {
        String email = "test@example.com";
        String password = "password123";
        String clientSecret = "secret123";
        String tokenUrl = "http://keycloak/realms/my-realm/protocol/openid-connect/token";

        when(vaultService.getClientSecret()).thenReturn(clientSecret);

        when(restTemplate.exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(KeycloakTokenResponse.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));

        Exception exception = assertThrows(
                Exception.class,
                () -> keycloakService.authenticateUser(email, password),
                "Une exception doit être levée si l'authentification échoue"
        );

        assertEquals("Échec de l'authentification avec Keycloak", exception.getMessage());
        verify(restTemplate, times(1)).exchange(
                eq(tokenUrl),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(KeycloakTokenResponse.class)
        );
    }


    @Test
    void testGetTeamMembers_KeycloakError() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        String adminToken = "fake-admin-token";

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            doReturn(adminToken).when(keycloakServiceSpy).getAdminToken();

            ProjectMemberId memberId1 = new ProjectMemberId(1L,"user-456");
            ProjectMember member1 = new ProjectMember();
            member1.setId(memberId1);
            member1.setRoleInProject("DEVELOPER");

            List<ProjectMember> members = List.of(member1);
            when(projectMemberRepository.findAll()).thenReturn(members);

            when(restTemplate.exchange(
                    eq("http://keycloak/admin/realms/my-realm/users"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Keycloak error"));

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> keycloakServiceSpy.getTeamMembers(accessToken),
                    "Une RuntimeException doit être levée si Keycloak échoue"
            );

            assertEquals("Erreur lors de la récupération des utilisateurs depuis Keycloak", exception.getMessage());
            verify(projectMemberRepository, times(1)).findAll();
            verify(restTemplate, times(1)).exchange(
                    eq("http://keycloak/admin/realms/my-realm/users"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)
            );
            verify(restTemplate, never()).getForEntity(anyString(), any());
        }
    }
    @Test
    void testGetTeamMembersbyProject_EmptyProjectMembers() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        String projectId = "1";

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            when(projectMemberRepository.findByIdProjectId(1L)).thenReturn(Collections.emptyList());

            List<Map<String, Object>> result = keycloakServiceSpy.getTeamMembersbyProject(accessToken, projectId);

            assertTrue(result.isEmpty(), "La liste des membres doit être vide");
            verify(projectMemberRepository, times(1)).findByIdProjectId(1L);
            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(ParameterizedTypeReference.class));
            verify(restTemplate, never()).getForEntity(anyString(), any());
        }
    }

    @Test
    void testGetTeamMembersbyProject_NullUserInfo() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        String projectId = "1";
        String adminToken = "fake-admin-token";

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            doReturn(adminToken).when(keycloakServiceSpy).getAdminToken();

            ProjectMemberId memberId1 = new ProjectMemberId(1L,"user-456");
            ProjectMember member1 = new ProjectMember();
            member1.setId(memberId1);
            member1.setRoleInProject("DEVELOPER");

            List<ProjectMember> members = List.of(member1);
            when(projectMemberRepository.findByIdProjectId(1L)).thenReturn(members);

            when(restTemplate.exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)
            )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

            List<Map<String, Object>> result = keycloakServiceSpy.getTeamMembersbyProject(accessToken, projectId);

            assertTrue(result.isEmpty(), "La liste des membres doit être vide si les infos utilisateurs sont null");
            verify(projectMemberRepository, times(1)).findByIdProjectId(1L);
            verify(restTemplate, times(1)).exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)
            );
            verify(restTemplate, never()).getForEntity(anyString(), any());
        }
    }
    @Test
    void testGetTeamMembersbyProject_KeycloakUserError() {
        String accessToken = "fake-access-token";
        String userId = "user-123";
        String projectId = "1";
        String adminToken = "fake-admin-token";

        try (MockedStatic<JWT> jwtMock = Mockito.mockStatic(JWT.class)) {
            DecodedJWT decodedJWT = mock(DecodedJWT.class);
            when(decodedJWT.getSubject()).thenReturn(userId);
            jwtMock.when(() -> JWT.decode(accessToken)).thenReturn(decodedJWT);

            doReturn(adminToken).when(keycloakServiceSpy).getAdminToken();

            ProjectMemberId memberId1 = new ProjectMemberId(1L,"user-456");
            ProjectMember member1 = new ProjectMember();
            member1.setId(memberId1);
            member1.setRoleInProject("DEVELOPER");

            List<ProjectMember> members = List.of(member1);
            when(projectMemberRepository.findByIdProjectId(1L)).thenReturn(members);

            when(restTemplate.exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)
            )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "User not found"));

            List<Map<String, Object>> result = keycloakServiceSpy.getTeamMembersbyProject(accessToken, projectId);

            assertTrue(result.isEmpty(), "La liste des membres doit être vide si la récupération de l'utilisateur échoue");
            verify(projectMemberRepository, times(1)).findByIdProjectId(1L);
            verify(restTemplate, times(1)).exchange(
                    eq("http://keycloak/admin/realms/my-realm/users/user-456"),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)
            );
            verify(restTemplate, never()).getForEntity(anyString(), any());
        }
    }

}