package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class Riego {
    private long id;
    private String fecha;

    @SerializedName("cantidad_agua")
    private double cantidadAgua; // O duración en segundos

    private String metodo; // Ej: "Automático", "Manual"

    @SerializedName("planta_id")
    private long plantaId;

    public Riego() {}

    public Riego(long id, String fecha, double cantidadAgua, String metodo, long plantaId) {
        this.id = id;
        this.fecha = fecha;
        this.cantidadAgua = cantidadAgua;
        this.metodo = metodo;
        this.plantaId = plantaId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public double getCantidadAgua() { return cantidadAgua; }
    public void setCantidadAgua(double cantidadAgua) { this.cantidadAgua = cantidadAgua; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public long getPlantaId() { return plantaId; }
    public void setPlantaId(long plantaId) { this.plantaId = plantaId; }
}