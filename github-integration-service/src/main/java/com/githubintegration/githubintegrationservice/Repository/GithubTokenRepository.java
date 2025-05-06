package com.githubintegration.githubintegrationservice.Repository;

import com.githubintegration.githubintegrationservice.Entity.GithubToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GithubTokenRepository extends JpaRepository<GithubToken, Long> {
    void deleteByUserId(String userId);
    Optional<GithubToken> findByUserId(String userId);
}
