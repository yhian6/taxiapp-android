package com.yhian.taxiapp.models;

public class TripItem {

    private String id;
    private String ruta;
    private String placa;
    private String vehiculoId;
    private String vehiculoNombre;
    private String numeroUnidad;
    private String conductorId;
    private String conductorNombre;
    private String conductorCelular;
    private double tarifa;
    private long puntosGanados;
    private long fecha;

    public TripItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(String vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public String getVehiculoNombre() {
        return vehiculoNombre;
    }

    public void setVehiculoNombre(String vehiculoNombre) {
        this.vehiculoNombre = vehiculoNombre;
    }

    public String getNumeroUnidad() {
        return numeroUnidad;
    }

    public void setNumeroUnidad(String numeroUnidad) {
        this.numeroUnidad = numeroUnidad;
    }

    public String getConductorId() {
        return conductorId;
    }

    public void setConductorId(String conductorId) {
        this.conductorId = conductorId;
    }

    public String getConductorNombre() {
        return conductorNombre;
    }

    public void setConductorNombre(String conductorNombre) {
        this.conductorNombre = conductorNombre;
    }

    public String getConductorCelular() {
        return conductorCelular;
    }

    public void setConductorCelular(String conductorCelular) {
        this.conductorCelular = conductorCelular;
    }

    public double getTarifa() {
        return tarifa;
    }

    public void setTarifa(double tarifa) {
        this.tarifa = tarifa;
    }

    public long getPuntosGanados() {
        return puntosGanados;
    }

    public void setPuntosGanados(long puntosGanados) {
        this.puntosGanados = puntosGanados;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }
}
