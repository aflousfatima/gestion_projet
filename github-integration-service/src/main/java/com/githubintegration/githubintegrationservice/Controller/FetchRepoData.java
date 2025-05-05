package com.githubintegration.githubintegrationservice.Controller;

import com.githubintegration.githubintegrationservice.Service.GithubTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
@RestController
@RequestMapping("/fetch_data")
@RequiredArgsConstructor
public class FetchRepoData {

    @Autowired
   public GithubTokenService githubTokenService;
    public  RestTemplate restTemplate;
    @GetMapping("/repos/{owner}/{repo}/commits")
    public ResponseEntity<Object> getCommits(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam String userId // ou tokenId selon ta logique
    ) {
        String accessToken = githubTokenService.getAccessTokenByUserId(userId); // à implémenter

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/commits";

        ResponseEntity<Object> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Object.class);

        return ResponseEntity.ok(response.getBody());
    }


    @GetMapping("/repos/{owner}/{repo}/branches")
    public ResponseEntity<Object> getBranches(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam String userId
    ) {
        String accessToken = githubTokenService.getAccessTokenByUserId(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/branches";

        ResponseEntity<Object> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Object.class);

        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/user")
    public ResponseEntity<Object> getAuthenticatedUser(@RequestParam String userId) {
        String accessToken = githubTokenService.getAccessTokenByUserId(userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                "https://api.github.com/user", HttpMethod.GET, entity, Object.class);

        return ResponseEntity.ok(response.getBody());
    }

}
