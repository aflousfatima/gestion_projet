package com.auth.authentification_service.DTO;

public class UserInfoDto {
    private String firstName;
    private String lastName;

    public UserInfoDto(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
