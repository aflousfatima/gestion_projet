package com.task.taskservice.Configuration;

import com.task.taskservice.Chatbot.DTO.PredictionResponse;
import com.task.taskservice.Chatbot.DTO.TaskInput;
import feign.Request;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-service", url = "http://localhost:8000")
public interface IAClient {
    @PostMapping("/api/v1/predict")
    PredictionResponse predictTaskDuration(@RequestBody TaskInput taskInput);

}