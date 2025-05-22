package com.task.taskservice.Chatbot.DTO;


public class PredictionResponse {
    private String taskTitle;
    private float predictedDuration;
    private float expectedDuration;
    private String modelName;

    // Getters et setters
    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
    public float getPredictedDuration() { return predictedDuration; }
    public void setPredictedDuration(float predictedDuration) { this.predictedDuration = predictedDuration; }
    public float getExpectedDuration() { return expectedDuration; }
    public void setExpectedDuration(float expectedDuration) { this.expectedDuration = expectedDuration; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
}