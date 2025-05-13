package com.auth.authentification_service.e2e.steps;

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
public class SigninSteps {
    private final SigninPage signinPage;

    public SigninSteps() {
        this.signinPage = new SigninPage(WebDriverFactory.getDriver());
    }

    @Given("I am on the signin page")
    public void i_am_on_the_signin_page() {
        signinPage.navigateTo();
    }

    @When("I fill the signin form with valid credentials")
    public void i_fill_the_signin_form_with_valid_credentials() {
        String email = "testuser@example.com";
        String password = "Password123";
        signinPage.fillForm(email, password);
    }

    @When("I fill the signin form with invalid credentials")
    public void i_fill_the_signin_form_with_invalid_credentials() {
        signinPage.fillForm("invalid@example.com", "WrongPassword");
    }

    @When("I submit the signin form")
    public void i_submit_the_signin_form() {
        signinPage.submitForm();
    }

    @Then("I am redirected to the company choice page")
    public void i_am_redirected_to_the_company_choice_page() {
        // Vérifier le message de succès avant la redirection
        String successMessage = signinPage.getSuccessMessage();
        Assertions.assertTrue(
                successMessage.contains("Login Successful"),
                "Le message de succès attendu n'est pas affiché. Message reçu : " + successMessage
        );
        Assertions.assertTrue(
                signinPage.isRedirectedToCompanyChoice(),
                "La redirection vers la page company-choice n'a pas eu lieu."
        );
    }

    @Then("I see an error message indicating invalid credentials")
    public void i_see_an_error_message_indicating_invalid_credentials() {
        String message = signinPage.getErrorMessage();
        System.out.println("Message d'erreur reçu : " + message);
        Assertions.assertTrue(
                message.contains("Error trying to connect") || message.contains("Invalid credentials"),
                "Le message d'erreur attendu n'est pas affiché. Message reçu : " + message
        );
    }

    @After
    public void tearDown() {
        WebDriverFactory.quitDriver();
    }
}