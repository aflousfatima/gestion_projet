package com.project.project_service.DTO;

import java.util.List;

public  record UserStoryRequest(String title, String description, String priority, String status, Integer effortPoints,
                                List<Long> dependsOn,List<String> tags)
{}
