package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class TipoSensor {
    private long id;
    private String nombre; // Ej: "Humedad Suelo", "Temperatura"

    @SerializedName("unidad_medida")
    private String unidadMedida; // Ej: "%", "°C"

    public TipoSensor() {}

    public TipoSensor(long id, String nombre, String unidadMedida) {
        this.id = id;
        this.nombre = nombre;
        this.unidadMedida = unidadMedida;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }
}