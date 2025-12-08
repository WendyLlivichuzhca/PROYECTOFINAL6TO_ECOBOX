package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Representa la tabla 'authtoken_token' de Django Rest Framework.
 * Se usa para mantener la sesión abierta.
 */
public class AuthToken {

    // La llave primaria es el propio token (varchar 40)
    @SerializedName("key")
    private String key;

    @SerializedName("created")
    private String created;

    @SerializedName("user_id")
    private long userId;

    // Constructor vacío requerido para Gson y otras librerías
    public AuthToken() {
    }

    // Constructor completo
    public AuthToken(String key, String created, long userId) {
        this.key = key;
        this.created = created;
        this.userId = userId;
    }

    // Getters y Setters

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "AuthToken{" +
                "key='" + key + '\'' +
                ", userId=" + userId +
                '}';
    }
}