package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class Medicion {
    private long id;
    private double valor; // El dato leído por el sensor

    private String fecha; // Timestamp de la lectura

    @SerializedName("sensor_id")
    private long sensorId;

    public Medicion() {}

    public Medicion(long id, double valor, String fecha, long sensorId) {
        this.id = id;
        this.valor = valor;
        this.fecha = fecha;
        this.sensorId = sensorId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public long getSensorId() { return sensorId; }
    public void setSensorId(long sensorId) { this.sensorId = sensorId; }
}