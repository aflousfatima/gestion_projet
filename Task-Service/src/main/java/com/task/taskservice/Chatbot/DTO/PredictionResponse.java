package com.task.taskservice.Chatbot.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PredictionResponse {
    @JsonProperty("task_title")
    private String taskTitle;
    @JsonProperty("predicted_duration")
    private float predictedDuration;
    @JsonProperty("expected_duration")
    private float expectedDuration;
    @JsonProperty("model_name")
    private String modelName;

    // Getters et setters inchangés
    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
    public float getPredictedDuration() { return predictedDuration; }
    public void setPredictedDuration(float predictedDuration) { this.predictedDuration = predictedDuration; }
    public float getExpectedDuration() { return expectedDuration; }
    public void setExpectedDuration(float expectedDuration) { this.expectedDuration = expectedDuration; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
}