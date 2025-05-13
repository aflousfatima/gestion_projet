package com.auth.authentification_service.e2e.steps;

import com.auth.authentification_service.e2e.pages.DashboardPage;
import com.auth.authentification_service.e2e.pages.SigninPage;
import com.auth.authentification_service.e2e.utils.WebDriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class SignoutSteps {
    private final SigninPage signinPage;
    private final DashboardPage dashboardPage;

    public SignoutSteps() {
        this.signinPage = new SigninPage(WebDriverFactory.getDriver());
        this.dashboardPage = new DashboardPage(WebDriverFactory.getDriver());
    }

    @Given("I am logged in and on the dashboard page")
    public void i_am_logged_in_and_on_the_dashboard_page() {
        // Connexion préalable avec un utilisateur de test
        signinPage.navigateTo();
        signinPage.fillForm("testuser@example.com", "Password123");
        signinPage.submitForm();

        // Vérifier la redirection après connexion
        Assertions.assertTrue(
                signinPage.isRedirectedToCompanyChoice(),
                "La connexion a échoué ou la redirection vers /company/company-choice n'a pas eu lieu."
        );

        // Naviguer vers le tableau de bord
        dashboardPage.navigateTo();
    }

    @When("I open the user dropdown menu")
    public void i_open_the_user_dropdown_menu() {
        dashboardPage.openUserDropdown();
    }

    @When("I click on the logout button")
    public void i_click_on_the_logout_button() {
        dashboardPage.clickLogout();
    }

    @Then("I am redirected to the signin page")
    public void i_am_redirected_to_the_signin_page() {
        Assertions.assertTrue(
                dashboardPage.isRedirectedToSigninPage(),
                "La redirection vers la page signin n'a pas eu lieu."
        );
    }

    @After
    public void tearDown() {
        WebDriverFactory.quitDriver();
    }
}