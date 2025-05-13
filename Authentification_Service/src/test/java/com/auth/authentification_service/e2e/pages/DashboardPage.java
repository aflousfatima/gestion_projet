package com.auth.authentification_service.e2e.pages;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

public class DashboardPage {
    private WebDriver driver;

    @FindBy(css = "div.user-icon")
    private WebElement userIcon;

    @FindBy(css = "div.dropdown-item:has(.fa-sign-out-alt)")
    private WebElement logoutButton;

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    public void navigateTo() {
        driver.get("http://localhost:3000/user/dashboard");
        // Attendre que la page soit chargée
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.user-icon")));
            System.out.println("Page du tableau de bord chargée avec succès.");
        } catch (Exception e) {
            System.out.println("Erreur lors du chargement de la page du tableau de bord : " + e.getMessage());
            takeScreenshot("target/dashboard-load-error-screenshot.png");
        }
    }

    public void openUserDropdown() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            wait.until(ExpectedConditions.elementToBeClickable(userIcon));
            userIcon.click();
            System.out.println("Menu déroulant ouvert avec succès.");
        } catch (Exception e) {
            System.out.println("Erreur lors de l'ouverture du menu déroulant : " + e.getMessage());
            takeScreenshot("target/user-dropdown-error-screenshot.png");
            throw e;
        }
    }

    public void clickLogout() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            wait.until(ExpectedConditions.elementToBeClickable(logoutButton));
            logoutButton.click();
            System.out.println("Clic sur le bouton de déconnexion effectué.");
            takeScreenshot("target/logout-click-screenshot.png");
        } catch (Exception e) {
            System.out.println("Erreur lors du clic sur le bouton de déconnexion : " + e.getMessage());
            takeScreenshot("target/logout-error-screenshot.png");
            throw e;
        }
    }

    public boolean isRedirectedToSigninPage() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        try {
            wait.until(ExpectedConditions.urlContains("/authentification/signin"));
            System.out.println("Redirection vers la page de connexion confirmée.");
            return driver.getCurrentUrl().contains("/authentification/signin");
        } catch (Exception e) {
            System.out.println("Erreur lors de la vérification de la redirection : " + e.getMessage());
            takeScreenshot("target/signin-redirect-error-screenshot.png");
            return false;
        }
    }

    private void takeScreenshot(String filePath) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File(filePath));
            System.out.println("Capture d'écran enregistrée : " + filePath);
        } catch (Exception e) {
            System.out.println("Erreur lors de la capture d'écran : " + e.getMessage());
        }
    }
}