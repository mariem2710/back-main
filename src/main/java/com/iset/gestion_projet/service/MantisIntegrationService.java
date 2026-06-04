package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.MantisIssueRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MantisIntegrationService {

    @Value("${mantis.api.url}")
    private String apiUrl;

    @Value("${mantis.project.id}")
    private Long projectId;

    @Value("${mantis.category}")
    private String category;

    @Value("${mantis.api.token}")       // ✅ corrigé
    private String apiToken;

    private final RestTemplate restTemplate;

    public Long createIssue(String summary, String description) {
        MantisIssueRequest request = new MantisIssueRequest(
                new MantisIssueRequest.ProjectRef(projectId),
                new MantisIssueRequest.CategoryRef(category),
                summary,
                description
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiToken);    // ✅ corrigé
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<MantisIssueRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/issues",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map issue = (Map) response.getBody().get("issue");
            return Long.valueOf(issue.get("id").toString());

        } catch (Exception e) {
            System.err.println("❌ Erreur MantisBT : " + e.getMessage());
            return null;
        }
    }
}