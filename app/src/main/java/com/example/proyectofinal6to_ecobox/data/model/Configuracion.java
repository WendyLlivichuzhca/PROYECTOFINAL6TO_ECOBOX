package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class Configuracion {
    private long id;

    // Si la API devuelve un objeto JSON, aquí podrías usar 'Object' o crear otra clase 'Preferencias'
    private Object preferencias;

    @SerializedName("fecha_actualizacion")
    private String fechaActualizacion;

    @SerializedName("usuario_id")
    private long usuarioId;

    public Configuracion() {
    }

    public Configuracion(long id, Object preferencias, String fechaActualizacion, long usuarioId) {
        this.id = id;
        this.preferencias = preferencias;
        this.fechaActualizacion = fechaActualizacion;
        this.usuarioId = usuarioId;
    }

    // Getters y Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Object getPreferencias() { return preferencias; }
    public void setPreferencias(Object preferencias) { this.preferencias = preferencias; }

    public String getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(String fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(long usuarioId) { this.usuarioId = usuarioId; }
}