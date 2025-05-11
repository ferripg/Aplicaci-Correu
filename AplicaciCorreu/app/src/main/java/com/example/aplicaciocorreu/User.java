package com.example.aplicaciocorreu;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("username")
    private String username;
    @SerializedName("password")
    private String password;
    @SerializedName("email")
    private String email;
    @SerializedName("smtp_server")
    private String smtpServer;
    @SerializedName("smtp_port")
    private int smtpPort;
    @SerializedName("smtp_username")
    private String smtpUsername;
    @SerializedName("smtp_password")
    private String smtpPassword;
    @SerializedName("imap_server")
    private String imapServer;
    @SerializedName("imap_port")
    private int imapPort;
    @SerializedName("imap_username")
    private String imapUsername;
    @SerializedName("imap_password")
    private String imapPassword;
    public User(String username, String password, String email,
                String smtpServer, int smtpPort, String smtpUsername, String smtpPassword,
                String imapServer, int imapPort, String imapUsername, String imapPassword) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
        this.imapServer = imapServer;
        this.imapPort = imapPort;
        this.imapUsername = imapUsername;
        this.imapPassword = imapPassword;
    }
}