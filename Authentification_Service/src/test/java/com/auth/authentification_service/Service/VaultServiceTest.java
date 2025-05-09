package com.auth.authentification_service.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultServiceTest {

    @Mock
    private VaultTemplate vaultTemplate;

    @InjectMocks
    private VaultService vaultService;

    private static final String VAULT_PATH = "secret/keycloak";
    private static final String SECRET_KEY = "credentials.secret";
    private static final String EXPECTED_SECRET = "my-client-secret";
    private static final String NO_SECRET_MESSAGE = "Aucun secret trouvé dans Vault.";

    @BeforeEach
    void setUp() {
        // VaultService is instantiated with the mocked VaultTemplate via @InjectMocks
    }

    @Test
    @DisplayName("Should return client secret when Vault response is valid")
    void getClientSecret_success() {
        // Arrange
        VaultResponse vaultResponse = new VaultResponse();
        Map<String, Object> data = new HashMap<>();
        data.put(SECRET_KEY, EXPECTED_SECRET);
        vaultResponse.setData(data);
        when(vaultTemplate.read(VAULT_PATH)).thenReturn(vaultResponse);

        // Act
        String result = vaultService.getClientSecret();

        // Assert
        assertEquals(EXPECTED_SECRET, result);
        verify(vaultTemplate).read(VAULT_PATH);
    }

    @Test
    @DisplayName("Should return no secret message when Vault response is null")
    void getClientSecret_nullResponse() {
        // Arrange
        when(vaultTemplate.read(VAULT_PATH)).thenReturn(null);

        // Act
        String result = vaultService.getClientSecret();

        // Assert
        assertEquals(NO_SECRET_MESSAGE, result);
        verify(vaultTemplate).read(VAULT_PATH);
    }

    @Test
    @DisplayName("Should return no secret message when Vault response data is null")
    void getClientSecret_nullData() {
        // Arrange
        VaultResponse vaultResponse = new VaultResponse();
        vaultResponse.setData(null);
        when(vaultTemplate.read(VAULT_PATH)).thenReturn(vaultResponse);

        // Act
        String result = vaultService.getClientSecret();

        // Assert
        assertEquals(NO_SECRET_MESSAGE, result);
        verify(vaultTemplate).read(VAULT_PATH);
    }

}