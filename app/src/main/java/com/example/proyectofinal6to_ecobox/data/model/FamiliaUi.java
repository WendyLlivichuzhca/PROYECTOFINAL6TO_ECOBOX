package com.example.proyectofinal6to_ecobox.data.model;

public class FamiliaUi {
    private long id;
    private String nombre;
    private String codigo;
    private int cantidadMiembros;
    private int cantidadPlantas;
    private String rolNombre; // "Administrador" o "Miembro"
    private String inicial;

    // Constructor vacío (opcional, pero recomendado)
    public FamiliaUi() {
    }

    // Constructor completo
    public FamiliaUi(long id, String nombre, String codigo, int cantidadMiembros, int cantidadPlantas, String rolNombre, String inicial) {
        this.id = id;
        this.nombre = nombre;
        this.codigo = codigo;
        this.cantidadMiembros = cantidadMiembros;
        this.cantidadPlantas = cantidadPlantas;
        this.rolNombre = rolNombre;
        this.inicial = inicial;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCodigo() {
        return codigo;
    }

    public int getCantidadMiembros() {
        return cantidadMiembros;
    }

    public int getCantidadPlantas() {
        return cantidadPlantas;
    }

    public String getRolNombre() {
        return rolNombre;
    }

    public String getInicial() {
        return inicial;
    }
}