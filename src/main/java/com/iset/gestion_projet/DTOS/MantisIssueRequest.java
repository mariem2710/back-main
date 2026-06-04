package com.iset.gestion_projet.DTOS;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MantisIssueRequest {

    private ProjectRef project;
    private CategoryRef category;
    private String summary;
    private String description;

    @Data
    @AllArgsConstructor
    public static class ProjectRef {
        private Long id;
    }

    @Data
    @AllArgsConstructor
    public static class CategoryRef {
        private String name;
    }
}