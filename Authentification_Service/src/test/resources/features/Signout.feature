@SignOut
Feature: User SignOut

  @SuccessfulSignOut
  Scenario: Successful signout
    Given I am logged in and on the dashboard page
    When I open the user dropdown menu
    And I click on the logout button
    Then I am redirected to the signin page