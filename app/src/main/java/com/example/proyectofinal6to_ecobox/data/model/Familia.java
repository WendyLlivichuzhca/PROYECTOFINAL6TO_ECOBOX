package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class Familia {
    private long id;
    private String nombre;

    @SerializedName("codigo_invitacion")
    private String codigoInvitacion;

    @SerializedName("fecha_creacion")
    private String fechaCreacion;

    @SerializedName("cantidad_plantas")
    private int cantidadPlantas;

    public Familia() {
    }

    public Familia(long id, String nombre, String codigoInvitacion, String fechaCreacion, int cantidadPlantas) {
        this.id = id;
        this.nombre = nombre;
        this.codigoInvitacion = codigoInvitacion;
        this.fechaCreacion = fechaCreacion;
        this.cantidadPlantas = cantidadPlantas;
    }

    // Getters y Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCodigoInvitacion() { return codigoInvitacion; }
    public void setCodigoInvitacion(String codigoInvitacion) { this.codigoInvitacion = codigoInvitacion; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public int getCantidadPlantas() { return cantidadPlantas; }
    public void setCantidadPlantas(int cantidadPlantas) { this.cantidadPlantas = cantidadPlantas; }

    @Override
    public String toString() {
        return nombre;
    }
}