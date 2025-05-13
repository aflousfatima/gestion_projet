@SignUp
Feature: User Signup

  @SuccessfulSignUp
  Scenario: Successful signup with valid information
    Given I am on the signup page
    When I fill the form with valid information
    And I submit the form
    Then I see a signup confirmation message
    And a user is created in Keycloak

  @FailedSignUp
  Scenario: Failed signup with an already used email
    Given I am on the signup page
    When I fill the form with an already used email
    And I submit the form
    Then I see an error message indicating the email is already used