package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class PrediccionIA {
    private long id;

    @SerializedName("resultado_prediccion")
    private String resultadoPrediccion; // Ej: "Planta saludable", "Posible plaga"

    private double confianza; // Porcentaje de certeza (0.0 - 1.0)
    private String fecha;

    @SerializedName("planta_id")
    private long plantaId;

    public PrediccionIA() {}

    public PrediccionIA(long id, String resultadoPrediccion, double confianza, String fecha, long plantaId) {
        this.id = id;
        this.resultadoPrediccion = resultadoPrediccion;
        this.confianza = confianza;
        this.fecha = fecha;
        this.plantaId = plantaId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getResultadoPrediccion() { return resultadoPrediccion; }
    public void setResultadoPrediccion(String resultadoPrediccion) { this.resultadoPrediccion = resultadoPrediccion; }

    public double getConfianza() { return confianza; }
    public void setConfianza(double confianza) { this.confianza = confianza; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public long getPlantaId() { return plantaId; }
    public void setPlantaId(long plantaId) { this.plantaId = plantaId; }
}