package com.example.aplicaciocorreu;

import com.google.gson.annotations.SerializedName;

public class ApiError {
    @SerializedName("detail")
    private String detail;

    public String getDetail() {
        return detail;
    }
}