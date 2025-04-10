package com.task.taskservice.Configuration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "project-service", url = "http://localhost:8085")  // Utilisez un URL dynamique ou un nom de service si vous utilisez Eureka
public interface ProjectClient  {


}

