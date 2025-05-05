package com.githubintegration.githubintegrationservice.Controller;

import com.githubintegration.githubintegrationservice.Config.AuthClient;
import com.githubintegration.githubintegrationservice.Service.GithubTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

// Interface Feign pour appeler le service d'authentification


@RestController
@RequestMapping("/api/github-integration")
@RequiredArgsConstructor
public class OAuthController {

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    @Value("${github.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final GithubTokenService githubTokenService;
    private final AuthClient authClient;

    @GetMapping("/oauth/login")
    public ResponseEntity<Void> loginWithGithub() {
        String githubUrl = "https://github.com/login/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=repo,user";

        URI redirect = URI.create(githubUrl);
        return ResponseEntity.status(HttpStatus.FOUND).location(redirect).build();
    }

    @GetMapping("/oauth/callback")
    public ResponseEntity<Void> callback(
            @RequestParam("code") String code,
            @RequestHeader("Authorization") String authorization) {
        try {
            String tokenUrl = "https://github.com/login/oauth/access_token";
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getBody().containsKey("error")) {
                throw new RuntimeException("Erreur GitHub : " + response.getBody().get("error_description"));
            }

            String accessToken = (String) response.getBody().get("access_token");
            String userId = authClient.decodeToken(authorization);
            if (userId == null || userId.isEmpty()) {
                throw new RuntimeException("Impossible de récupérer le userId depuis le token JWT");
            }

            githubTokenService.saveToken(userId, accessToken);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:3000/user/settings/integrations?github=success"))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:3000/user/settings/integrations?github=error&message=" + e.getMessage()))
                    .build();
        }
    }

    @GetMapping("/check-token")
    public ResponseEntity<Map<String, Boolean>> checkGithubToken(
            @RequestHeader("Authorization") String authorization) {
        try {
            String userId = authClient.decodeToken(authorization);
            if (userId == null || userId.isEmpty()) {
                throw new RuntimeException("Impossible de récupérer le userId depuis le token JWT");
            }
            boolean hasToken = githubTokenService.getAccessTokenByUserId(userId) != null;
            return ResponseEntity.ok(Map.of("hasToken", hasToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("hasToken", false));
        }
    }
}