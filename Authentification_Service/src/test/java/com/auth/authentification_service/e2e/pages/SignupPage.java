package com.auth.authentification_service.e2e.pages;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

public class SignupPage {
    private WebDriver driver;

    @FindBy(name = "firstName")
    private WebElement firstNameInput;

    @FindBy(name = "lastName")
    private WebElement lastNameInput;

    @FindBy(name = "username")
    private WebElement usernameInput;

    @FindBy(name = "email")
    private WebElement emailInput;

    @FindBy(name = "password")
    private WebElement passwordInput;

    @FindBy(name = "termsAccepted")
    private WebElement termsCheckbox;

    @FindBy(className = "submit-btn")
    private WebElement submitButton;

    @FindBy(className = "success-message")
    private WebElement successMessage;

    @FindBy(className = "error-message")
    private WebElement errorMessage;

    public SignupPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    public void navigateTo() {
        driver.get("http://localhost:3000/authentification/signup");
    }

    public void fillForm(String firstName, String lastName, String username, String email, String password) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.elementToBeClickable(firstNameInput)).sendKeys(firstName);
        wait.until(ExpectedConditions.elementToBeClickable(lastNameInput)).sendKeys(lastName);
        wait.until(ExpectedConditions.elementToBeClickable(usernameInput)).sendKeys(username);
        wait.until(ExpectedConditions.elementToBeClickable(emailInput)).sendKeys(email);
        wait.until(ExpectedConditions.elementToBeClickable(passwordInput)).sendKeys(password);
        wait.until(ExpectedConditions.elementToBeClickable(termsCheckbox)).click();
    }

    public void submitForm() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        submitButton.click();

        // Capture d'écran pour débogage
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("submit-screenshot.png"));
        } catch (Exception e) {
            System.out.println("Erreur capture d'écran : " + e.getMessage());
        }
    }

    public String getSuccessMessage() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            wait.until(ExpectedConditions.visibilityOf(successMessage));
            String text = successMessage.getText();
            System.out.println("Success message HTML: " + successMessage.getAttribute("outerHTML"));
            System.out.println("Success message text: " + text);
            return text;
        } catch (Exception e) {
            System.out.println("Erreur récupération message succès : " + e.getMessage());
            return "";
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
}
