        package com.auth.authentification_service.e2e.steps;

import com.auth.authentification_service.e2e.pages.SignupPage;
import com.auth.authentification_service.e2e.utils.WebDriverFactory;
import com.auth.authentification_service.utils.IsUserExist;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class SignupSteps {
    private final SignupPage signupPage;
    private final IsUserExist isUserExist;
    private String emailUsed;

    @Autowired
    public SignupSteps(IsUserExist isUserExist) {
        this.signupPage = new SignupPage(WebDriverFactory.getDriver());
        this.isUserExist = isUserExist;
    }

    @Given("I am on the signup page")
    public void i_am_on_the_signup_page() {
        signupPage.navigateTo();
    }

    @When("I fill the form with valid information")
    public void i_fill_the_form_with_valid_information() {
        emailUsed = "john.doe" + System.currentTimeMillis() + "@example.com";
        signupPage.fillForm(
                "John",
                "Doe",
                "johndoe" + System.currentTimeMillis(),
                emailUsed,
                "Password123"
        );
    }

    @When("I fill the form with an already used email")
    public void i_fill_the_form_with_an_already_used_email() {
        signupPage.fillForm(
                "John",
                "Doe",
                "johndoe" + System.currentTimeMillis(),
                "existing@example.com",
                "Password123"
        );
    }

    @And("I submit the form")
    public void i_submit_the_form() {
        signupPage.submitForm();
    }

    @Then("I see a signup confirmation message")
    public void i_see_a_signup_confirmation_message() {
        String message = signupPage.getSuccessMessage();
        System.out.println("Message reçu : " + message);
        Assertions.assertTrue(message.contains("Registration successful"),
                "Le message de confirmation n'est pas affiché. Message reçu : " + message);
    }

    @Then("I see an error message indicating the email is already used")
    public void i_see_an_error_message_indicating_the_email_is_already_used() {
        String message = signupPage.getErrorMessage();
        System.out.println("Message d'erreur reçu : " + message);
        Assertions.assertTrue(message.contains("Email Already Exist "),
                "Le message d'erreur attendu n'est pas affiché. Message reçu : " + message);
    }

    @And("a user is created in Keycloak")
    public void a_user_is_created_in_keycloak() {
        String accessToken = isUserExist.getAdminToken();
        boolean userExists = isUserExist.userExists(emailUsed, accessToken);
        Assertions.assertTrue(userExists, "L'utilisateur n'a pas été créé dans Keycloak");
    }

    @After
    public void tearDown() {
        WebDriverFactory.quitDriver();
    }
}
