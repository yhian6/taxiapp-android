package com.yhian.taxiapp.models;

public class ReportItem {

    private String id;
    private String pasajeroUid;
    private String pasajeroNombre;
    private String pasajeroCelular;
    private String tipo;
    private String descripcion;
    private String estado;
    private String vehiculoId;
    private String placa;
    private String conductorId;
    private String conductorNombre;
    private String origen;
    private long fecha;

    public ReportItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPasajeroUid() {
        return pasajeroUid;
    }

    public void setPasajeroUid(String pasajeroUid) {
        this.pasajeroUid = pasajeroUid;
    }

    public String getPasajeroNombre() {
        return pasajeroNombre;
    }

    public void setPasajeroNombre(String pasajeroNombre) {
        this.pasajeroNombre = pasajeroNombre;
    }

    public String getPasajeroCelular() {
        return pasajeroCelular;
    }

    public void setPasajeroCelular(String pasajeroCelular) {
        this.pasajeroCelular = pasajeroCelular;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(String vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
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

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }
}
