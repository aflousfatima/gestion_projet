package com.githubintegration.githubintegrationservice.unit.Repository;
import com.githubintegration.githubintegrationservice.Entity.GithubToken;
import com.githubintegration.githubintegrationservice.Repository.GithubTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class GithubTokenRepositoryTest {

    @Autowired
    private GithubTokenRepository githubTokenRepository;

    @Test
    void findByUserId_WhenTokenExists_ReturnsToken() {
        // Arrange
        String userId = "user123";
        GithubToken token = new GithubToken();
        token.setUserId(userId);
        token.setAccessToken("test-access-token");
        githubTokenRepository.save(token);

        // Act
        Optional<GithubToken> result = githubTokenRepository.findByUserId(userId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
        assertEquals("test-access-token", result.get().getAccessToken());
    }

    @Test
    void findByUserId_WhenTokenDoesNotExist_ReturnsEmpty() {
        // Arrange
        String userId = "nonexistent";

        // Act
        Optional<GithubToken> result = githubTokenRepository.findByUserId(userId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void deleteByUserId_WhenTokenExists_DeletesToken() {
        // Arrange
        String userId = "user123";
        GithubToken token = new GithubToken();
        token.setUserId(userId);
        token.setAccessToken("test-access-token");
        githubTokenRepository.save(token);

        // Act
        githubTokenRepository.deleteByUserId(userId);

        // Assert
        Optional<GithubToken> result = githubTokenRepository.findByUserId(userId);
        assertFalse(result.isPresent());
    }

    @Test
    void deleteByUserId_WhenTokenDoesNotExist_NoExceptionThrown() {
        // Arrange
        String userId = "nonexistent";

        // Act & Assert
        assertDoesNotThrow(() -> githubTokenRepository.deleteByUserId(userId));
    }

    @Test
    void save_WhenTokenIsValid_SavesToken() {
        // Arrange
        String userId = "user123";
        GithubToken token = new GithubToken();
        token.setUserId(userId);
        token.setAccessToken("test-access-token");

        // Act
        GithubToken savedToken = githubTokenRepository.save(token);

        // Assert
        Optional<GithubToken> result = githubTokenRepository.findByUserId(userId);
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
        assertEquals("test-access-token", result.get().getAccessToken());
        assertEquals(savedToken.getId(), result.get().getId());
    }
}