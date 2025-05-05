package com.githubintegration.githubintegrationservice.Service;

import com.githubintegration.githubintegrationservice.Entity.GithubToken;
import com.githubintegration.githubintegrationservice.Repository.GithubTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GithubTokenService {

    private final GithubTokenRepository tokenRepository;

    public String getAccessTokenByUserId(String userId) {
        return tokenRepository.findByUserId(userId)
                .map(GithubToken::getAccessToken)
                .orElseThrow(() -> new RuntimeException("Token not found for user"));
    }

    public void saveToken(String userId, String accessToken) {
        // Vérifier si un token existe déjà pour cet utilisateur
        tokenRepository.findByUserId(userId).ifPresentOrElse(
                existingToken -> {
                    existingToken.setAccessToken(accessToken);
                    tokenRepository.save(existingToken);
                },
                () -> {
                    GithubToken newToken = new GithubToken();
                    newToken.setUserId(userId);
                    newToken.setAccessToken(accessToken);
                    tokenRepository.save(newToken);
                }
        );
    }
}