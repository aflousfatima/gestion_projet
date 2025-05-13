package com.auth.authentification_service.e2e.pages;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

public class SigninPage {
    private WebDriver driver;

    @FindBy(name = "email")
    private WebElement emailInput;

    @FindBy(name = "password")
    private WebElement passwordInput;

    @FindBy(className = "submit1-btn")
    private WebElement submitButton;

    @FindBy(className = "error-message")
    private WebElement errorMessage;

    @FindBy(className = "success-message")
    private WebElement successMessage;

    public SigninPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    public void navigateTo() {
        driver.get("http://localhost:3000/authentification/signin");
    }

    public void fillForm(String email, String password) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.elementToBeClickable(emailInput)).sendKeys(email);
        wait.until(ExpectedConditions.elementToBeClickable(passwordInput)).sendKeys(password);
    }

    public void submitForm() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        submitButton.click();

        // Capture d'écran pour débogage
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("target/signin-submit-screenshot.png"));
        } catch (Exception e) {
            System.out.println("Erreur capture d'écran : " + e.getMessage());
        }
    }

    public boolean isRedirectedToCompanyChoice() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15)); // Augmenter le délai
        try {
            wait.until(ExpectedConditions.urlContains("/company/company-choice"));
            return driver.getCurrentUrl().contains("/company/company-choice");
        } catch (Exception e) {
            System.out.println("Erreur lors de la vérification de la redirection : " + e.getMessage());
            return false;
        }
    }

    public String getErrorMessage() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            wait.until(ExpectedConditions.visibilityOf(errorMessage));
            String text = errorMessage.getText();
            System.out.println("Error message text: " + text);
            return text;
        } catch (Exception e) {
            System.out.println("Erreur récupération message erreur : " + e.getMessage());
            return "";
        }
    }

    public String getSuccessMessage() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            wait.until(ExpectedConditions.visibilityOf(successMessage));
            String text = successMessage.getText();
            System.out.println("Success message text: " + text);
            return text;
        } catch (Exception e) {
            System.out.println("Erreur récupération message succès : " + e.getMessage());
            return "";
        }
    }
}