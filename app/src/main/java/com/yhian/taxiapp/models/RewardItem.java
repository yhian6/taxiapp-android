package com.yhian.taxiapp.models;

public class RewardItem {

    private String id;
    private String titulo;
    private String descripcion;
    private String imagenUrl;
    private String tipo;
    private long puntosRequeridos;
    private boolean activo;

    public RewardItem() {
    }

    public RewardItem(String id, String titulo, String descripcion, String tipo, long puntosRequeridos) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.puntosRequeridos = puntosRequeridos;
        this.activo = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public long getPuntosRequeridos() {
        return puntosRequeridos;
    }

    public void setPuntosRequeridos(long puntosRequeridos) {
        this.puntosRequeridos = puntosRequeridos;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
