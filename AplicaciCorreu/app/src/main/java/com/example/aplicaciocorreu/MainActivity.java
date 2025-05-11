package com.example.aplicaciocorreu;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private EmailAdapter adapter;
    private Button btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerEmails);

        // Configura l'adapter amb el listener d'eliminació
        adapter = new EmailAdapter(new ArrayList<>(), position -> {
            Email email = adapter.getEmailAtPosition(position);
            if (email != null) {
                deleteEmail(email.getUid());
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            loadEmails();
        });

        findViewById(R.id.fabCompose).setOnClickListener(v -> {
            startActivity(new Intent(this, ComposeActivity.class));
        });

        // Botó per tancar sessió
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            logoutUser();
        });

        loadEmails();
    }

    private void logoutUser() {
        try {
            // Esborra el token JWT de les preferències
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            EncryptedSharedPreferences sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    "auth_prefs",
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            sharedPreferences.edit()
                    .remove("jwt_token")
                    .apply();

            // Torna a AuthActivity i tanca MainActivity
            startActivity(new Intent(this, AuthActivity.class));
            finish(); // Evita tornar enrere amb el botó

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadEmails() {
        ApiService api = RetrofitClient.getApiService(this);
        api.getEmails().enqueue(new Callback<EmailResponse>() {
            @Override
            public void onResponse(Call<EmailResponse> call, Response<EmailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Email> emails = response.body().getEmails();  // <-- Obtenir la llista des de l'objecte
                    Collections.reverse(emails);
                    adapter.setEmails(emails);
                }
            }

            @Override
            public void onFailure(Call<EmailResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error carregant correus", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteEmail(String uid) {
        ApiService api = RetrofitClient.getApiService(this);
        api.deleteEmail(uid).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Correu eliminat", Toast.LENGTH_SHORT).show();
                    loadEmails(); // Actualitza la llista després d'eliminar
                } else {
                    Toast.makeText(MainActivity.this, "Error en eliminar el correu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error de xarxa", Toast.LENGTH_SHORT).show();
            }
        });
    }

}