package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class Planta {
    private long id;
    private String nombre; // Ej: "Tomate Cherry"
    private String especie;

    @SerializedName("fecha_plantacion")
    private String fechaPlantacion;

    private String descripcion;

    @SerializedName("familia_id")
    private long familiaId;

    public Planta() {}

    public Planta(long id, String nombre, String especie, String fechaPlantacion, String descripcion, long familiaId) {
        this.id = id;
        this.nombre = nombre;
        this.especie = especie;
        this.fechaPlantacion = fechaPlantacion;
        this.descripcion = descripcion;
        this.familiaId = familiaId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public String getFechaPlantacion() { return fechaPlantacion; }
    public void setFechaPlantacion(String fechaPlantacion) { this.fechaPlantacion = fechaPlantacion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public long getFamiliaId() { return familiaId; }
    public void setFamiliaId(long familiaId) { this.familiaId = familiaId; }
}