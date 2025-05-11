package com.example.aplicaciocorreu;

import android.content.Context;
import android.content.Intent;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenAuthenticator implements Authenticator {
    private final Context context;
    private final EncryptedSharedPreferences prefs;

    public TokenAuthenticator(Context context) {
        this.context = context;
        try {
            // 1. Genera o obté la clau mestra
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            // 2. Configura EncryptedSharedPreferences
            prefs = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    "auth_prefs",
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch (GeneralSecurityException | IOException e) {
            // 3. En cas d'error, llença una RuntimeException
            throw new RuntimeException("Error inicialitzant xifratge", e);
        }
    }

    @Override
    public Request authenticate(Route route, Response response) {
        // 1. Verifica si l'error és 401 (token expirat o invàlid)
        if (responseCount(response) >= 3 || response.code() != 401) {
            return null; // Evita bucle infinit
        }

        // 2. Obté el refresh token de les preferències
        String refreshToken = prefs.getString("refresh_token", null);
        if (refreshToken == null) {
            logoutUser();
            return null;
        }

        // 3. Fes la petició per renovar el token
        ApiService api = RetrofitClient.getApiService(context);
        Call<TokenResponse> call = api.refreshToken(new RefreshTokenRequest(refreshToken));

        try {
            retrofit2.Response<TokenResponse> res = call.execute();
            if (res.isSuccessful() && res.body() != null) {
                // 4. Desa els nous tokens
                prefs.edit()
                        .putString("jwt_token", res.body().getAccessToken())
                        .putString("refresh_token", res.body().getRefreshToken())
                        .apply();

                // 5. Retorna la petició original amb el nou token
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + res.body().getAccessToken())
                        .build();
            } else {
                logoutUser();
            }
        } catch (IOException e) {
            e.printStackTrace();
            logoutUser();
        }
        return null;
    }

    // Evita intents infinits de renovar el token
    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    // Tanca sessió i redirigeix a AuthActivity
    private void logoutUser() {
        prefs.edit()
                .clear()
                .apply();

        Intent intent = new Intent(context, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}