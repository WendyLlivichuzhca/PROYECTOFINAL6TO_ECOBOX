package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class Planta {

    private long id;
    private String nombre;
    private String especie;

    @SerializedName("fecha_creación")
    private String fechacreacion;

    private String descripcion;

    @SerializedName("familia_id")
    private long familiaId;

    private String ubicacion; // Nuevo campo

    public Planta() {
    }

    // Constructor con 6 parámetros (sin ubicacion)
    public Planta(long id, String nombre, String especie, String fechacreacion,
                  String descripcion, long familiaId) {
        this.id = id;
        this.nombre = nombre;
        this.especie = especie;
        this.fechacreacion = fechacreacion;
        this.descripcion = descripcion;
        this.familiaId = familiaId;
        this.ubicacion = ""; // Valor por defecto
    }

    // Constructor con 7 parámetros (con ubicacion)
    public Planta(long id, String nombre, String especie, String fechacreacion,
                  String descripcion, long familiaId, String ubicacion) {
        this.id = id;
        this.nombre = nombre;
        this.especie = especie;
        this.fechacreacion = fechacreacion;
        this.descripcion = descripcion;
        this.familiaId = familiaId;
        this.ubicacion = ubicacion;
    }

    // Getters y Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEspecie() {
        return especie;
    }

    public void setEspecie(String especie) {
        this.especie = especie;
    }

    public String Fechacreacion() {
        return fechacreacion;
    }

    public void setFechaCreacion(String fechacreacion) {
        this.fechacreacion = fechacreacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public long getFamiliaId() {
        return familiaId;
    }

    public void setFamiliaId(long familiaId) {
        this.familiaId = familiaId;
    }

    // Nuevos getter y setter para ubicacion
    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }
}