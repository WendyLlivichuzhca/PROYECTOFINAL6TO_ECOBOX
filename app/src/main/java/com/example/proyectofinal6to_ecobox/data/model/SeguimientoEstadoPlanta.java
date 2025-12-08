package com.example.proyectofinal6to_ecobox.data.model;

import com.google.gson.annotations.SerializedName;

public class SeguimientoEstadoPlanta {
    private long id;
    private String observacion;
    private String fecha;

    @SerializedName("foto_url")
    private String fotoUrl; // Ruta de la imagen si guardas fotos

    @SerializedName("planta_id")
    private long plantaId;

    public SeguimientoEstadoPlanta() {}

    public SeguimientoEstadoPlanta(long id, String observacion, String fecha, String fotoUrl, long plantaId) {
        this.id = id;
        this.observacion = observacion;
        this.fecha = fecha;
        this.fotoUrl = fotoUrl;
        this.plantaId = plantaId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public long getPlantaId() { return plantaId; }
    public void setPlantaId(long plantaId) { this.plantaId = plantaId; }
}