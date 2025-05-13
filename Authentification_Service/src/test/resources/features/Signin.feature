@SignIn
Feature: User SignIn

  @SuccessfulSignIn
  Scenario: Successful signin with valid credentials
    Given I am on the signin page
    When I fill the signin form with valid credentials
    And I submit the signin form
    Then I am redirected to the company choice page

  @FailedSignIn
  Scenario: Failed signin with invalid credentials
    Given I am on the signin page
    When I fill the signin form with invalid credentials
    And I submit the signin form
    Then I see an error message indicating invalid credentials