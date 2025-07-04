package com.auth.authentification_service.DTO;

public class UserInfoDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;

    public UserInfoDto(String id ,String firstName, String lastName , String email) {
        this.id=id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
