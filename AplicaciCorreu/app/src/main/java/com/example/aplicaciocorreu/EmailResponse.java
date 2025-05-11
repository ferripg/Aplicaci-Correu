package com.example.aplicaciocorreu;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class EmailResponse {
    @SerializedName("emails")
    private List<Email> emails;

    public List<Email> getEmails() {
        return emails;
    }
}