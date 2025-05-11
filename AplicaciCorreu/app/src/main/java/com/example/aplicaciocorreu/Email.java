package com.example.aplicaciocorreu;

import com.google.gson.annotations.SerializedName;

public class Email {

    @SerializedName("uid")
    private String uid;
    @SerializedName("from")
    private String from;

    @SerializedName("subject")
    private String subject;

    @SerializedName("date")
    private String date;

    @SerializedName("preview")
    private String preview;

    public Email(String uid, String from, String subject, String date, String preview) {
        this.uid = uid;
        this.from = from;
        this.subject = subject;
        this.date = date;
        this.preview = preview;
    }

    // Getters
    public String getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public String getDate() {
        return date;
    }

    public String getPreview() {
        return preview;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

}