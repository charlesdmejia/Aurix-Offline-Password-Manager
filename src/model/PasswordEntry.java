package model;

import java.io.Serializable;

public class PasswordEntry implements Serializable {

    private String website;
    private String username;
    private String password;
    private String category;

    public PasswordEntry(String website, String username, String password, String category) {
        this.website = website;
        this.username = username;
        this.password = password;
        this.category = category;
    }

    public String getWebsite() {
        return website;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCategory() {
        return category;
    }

}