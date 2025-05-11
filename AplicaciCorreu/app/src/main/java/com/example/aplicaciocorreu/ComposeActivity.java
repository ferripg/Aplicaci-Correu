package com.example.aplicaciocorreu;


import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComposeActivity extends AppCompatActivity {

    private EditText etTo, etSubject, etBody;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        etTo = findViewById(R.id.etTo);
        etSubject = findViewById(R.id.etSubject);
        etBody = findViewById(R.id.etBody);
        apiService = RetrofitClient.getApiService(this);

        findViewById(R.id.btnSend).setOnClickListener(v -> sendEmail());
    }

    private void sendEmail() {
        String to = etTo.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String body = etBody.getText().toString().trim();

        if (to.isEmpty() || subject.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Omple tots els camps", Toast.LENGTH_SHORT).show();
            return;
        }

        EmailRequest request = new EmailRequest(to, subject, body);

        apiService.sendEmail(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ComposeActivity.this, "Correu enviat!", Toast.LENGTH_SHORT).show();
                    finish(); // Torna a MainActivity
                } else {
                    Toast.makeText(ComposeActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ComposeActivity.this, "Error de xarxa", Toast.LENGTH_SHORT).show();
            }
        });
    }
}