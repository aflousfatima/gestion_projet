package com.githubintegration.githubintegrationservice.Repository;


import com.githubintegration.githubintegrationservice.Entity.OAuthState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthStateRepository extends JpaRepository<OAuthState, String> {
}