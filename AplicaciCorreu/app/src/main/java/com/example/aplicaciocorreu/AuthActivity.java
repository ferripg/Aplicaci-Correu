package com.example.aplicaciocorreu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.example.aplicaciocorreu.ApiService;
import com.example.aplicaciocorreu.RetrofitClient;
import com.example.aplicaciocorreu.TokenResponse;
import com.example.aplicaciocorreu.User;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends AppCompatActivity {
    private LinearLayout layoutLogin, layoutRegister;
    private Button btnSwitchMode;

    // Camps de LOGIN
    private EditText etLoginUsername, etLoginPassword;

    // Camps de REGISTRE
    private EditText etRegisterUsername, etRegisterPassword, etRegisterEmail, etRegisterSmtpServer,
            etRegisterSmtpPort, etRegisterSmtpUsername, etRegisterSmtpPassword,
            etRegisterImapServer, etRegisterImapPort,etRegisterImapPassword,etRegisterImapUsername;
    private Button btnLogin, btnDoRegister;

    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Inicialitza les vistes
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutRegister = findViewById(R.id.layoutRegister);
        btnSwitchMode = findViewById(R.id.btnSwitchMode);

        // Camps de login
        etLoginUsername = findViewById(R.id.etLoginUsername);
        etLoginPassword = findViewById(R.id.etLoginPassword);

        // Camps de registre
        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterSmtpServer = findViewById(R.id.etRegisterSmtpServer);
        etRegisterSmtpPort = findViewById(R.id.etRegisterSmtpPort);
        etRegisterSmtpUsername = findViewById(R.id.etRegisterSmtpUsername);
        etRegisterSmtpPassword = findViewById(R.id.etRegisterSmtpPassword);
        etRegisterImapServer = findViewById(R.id.etRegisterImapServer);
        etRegisterImapPort = findViewById(R.id.etRegisterImapPort);
        etRegisterImapUsername = findViewById(R.id.etRegisterImapUsername);
        etRegisterImapPassword = findViewById(R.id.etRegisterImapPassword);

        // Configura botons
        btnLogin = findViewById(R.id.btnLogin);
        btnDoRegister = findViewById(R.id.btnDoRegister);

        btnLogin.setOnClickListener(v -> handleLogin());
        btnDoRegister.setOnClickListener(v -> handleRegister());

        btnSwitchMode.setOnClickListener(v -> switchMode());
    }

    private void switchMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            layoutLogin.setVisibility(View.VISIBLE);
            layoutRegister.setVisibility(View.GONE);
            btnSwitchMode.setText("No tens compte? Registra't");
            btnDoRegister.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
        } else {
            layoutLogin.setVisibility(View.GONE);
            layoutRegister.setVisibility(View.VISIBLE);
            btnSwitchMode.setText("Ja tens compte? Inicia sessió");
            btnLogin.setVisibility(View.GONE);
            btnDoRegister.setVisibility(View.VISIBLE);
        }
    }

    private void handleLogin() {
        String username = etLoginUsername.getText().toString();
        String password = etLoginPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Omple usuari i contrasenya", Toast.LENGTH_SHORT).show();
            return;
        }
        loginUser(username, password);
    }

    private void handleRegister() {
        String username = etRegisterUsername.getText().toString();
        String password = etRegisterPassword.getText().toString();
        String email = etRegisterEmail.getText().toString();
        String smtpServer = etRegisterSmtpServer.getText().toString();
        String smtpPortStr = etRegisterSmtpPort.getText().toString();
        String smtpUsername = etRegisterSmtpUsername.getText().toString();
        String smtpPassword = etRegisterSmtpPassword.getText().toString();
        String imapServer = etRegisterImapServer.getText().toString();
        String imapPortStr = etRegisterImapPort.getText().toString();
        String imapUsername = etRegisterImapUsername.getText().toString();
        String imapPassword = etRegisterImapPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty() ||
                smtpServer.isEmpty() || smtpPortStr.isEmpty() || smtpUsername.isEmpty() || smtpPassword.isEmpty() ||
                imapServer.isEmpty() || imapPortStr.isEmpty() || imapUsername.isEmpty() || imapPassword.isEmpty()) {

            Toast.makeText(this, "Omple tots els camps", Toast.LENGTH_SHORT).show();
            return;
        }

        int smtpPort;
        int imapPort;
        try {
            smtpPort = Integer.parseInt(smtpPortStr);
            if (smtpPort < 1 || smtpPort > 65535) {
                Toast.makeText(this, "Port SMTP ha de ser entre 1 i 65535", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Port SMTP no vàlid", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            imapPort = Integer.parseInt(imapPortStr);
            if (imapPort < 1 || imapPort > 65535) {
                Toast.makeText(this, "Port IMAP ha de ser entre 1 i 65535", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Port IMAP no vàlid", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = new User(username, password, email,
                smtpServer, smtpPort, smtpUsername, smtpPassword,
                imapServer, imapPort, imapUsername, imapPassword);

        registerUser(user);
    }
    private void loginUser(String username, String password) {

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Omple tots els camps", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService api = RetrofitClient.getApiService(this);
        Call<TokenResponse> call = api.login(username, password);

        call.enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveTokens(
                            response.body().getAccessToken(),
                            response.body().getRefreshToken()
                    );
                    navigateToMain();
                } else {
                    Toast.makeText(AuthActivity.this, "Credencials incorrectes", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                Toast.makeText(AuthActivity.this, "Error de connexió", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser(User user) {

        ApiService api = RetrofitClient.getApiService(this);
        Call<ResponseBody> call = api.register(user);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AuthActivity.this, "Registre exitós", Toast.LENGTH_SHORT).show();
                    switchMode(); // Torna al mode login
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Gson gson = new Gson();
                        ApiError apiError = gson.fromJson(errorBody, ApiError.class);
                        String errorMsg = apiError != null && apiError.getDetail() != null
                                ? apiError.getDetail()
                                : "Error en el registre";
                        Toast.makeText(AuthActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(AuthActivity.this, "Error en el registre", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(AuthActivity.this, "Error de xarxa", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTokens(String accessToken, String refreshToken) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            EncryptedSharedPreferences prefs = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    "auth_prefs",
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            prefs.edit()
                    .putString("jwt_token", accessToken)
                    .putString("refresh_token", refreshToken) // Guarda el refresh_token
                    .apply();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish(); // Evita tornar enrere amb el botó
    }
}
