package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class Sensor {
    private long id;
    private String nombre;
    private String ubicacion;

    @SerializedName("fecha_instalacion")
    private String fechaInstalacion;

    private boolean activo;

    @SerializedName("estado_sensor_id")
    private long estadoSensorId;

    @SerializedName("planta_id")
    private long plantaId;

    @SerializedName("tipo_sensor_id")
    private long tipoSensorId;

    public Sensor() {
    }

    public Sensor(long id, String nombre, String ubicacion, String fechaInstalacion, boolean activo, long estadoSensorId, long plantaId, long tipoSensorId) {
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.fechaInstalacion = fechaInstalacion;
        this.activo = activo;
        this.estadoSensorId = estadoSensorId;
        this.plantaId = plantaId;
        this.tipoSensorId = tipoSensorId;
    }

    // Getters y Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getFechaInstalacion() { return fechaInstalacion; }
    public void setFechaInstalacion(String fechaInstalacion) { this.fechaInstalacion = fechaInstalacion; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public long getEstadoSensorId() { return estadoSensorId; }
    public void setEstadoSensorId(long estadoSensorId) { this.estadoSensorId = estadoSensorId; }

    public long getPlantaId() { return plantaId; }
    public void setPlantaId(long plantaId) { this.plantaId = plantaId; }

    public long getTipoSensorId() { return tipoSensorId; }
    public void setTipoSensorId(long tipoSensorId) { this.tipoSensorId = tipoSensorId; }

    @Override
    public String toString() {
        return nombre + " (" + ubicacion + ")";
    }
}