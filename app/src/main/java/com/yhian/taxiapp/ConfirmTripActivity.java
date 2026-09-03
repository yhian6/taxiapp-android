package com.yhian.taxiapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.yhian.taxiapp.utils.FirebaseRefs;

import java.util.HashMap;
import java.util.Map;

public class ConfirmTripActivity extends AppCompatActivity {

    private static final long POINTS_PER_TRIP = 3L;

    public static final String EXTRA_VEHICLE_ID = "extra_vehicle_id";
    public static final String EXTRA_UNIT_NUMBER = "extra_unit_number";
    public static final String EXTRA_PLATE = "extra_plate";
    public static final String EXTRA_VEHICLE_NAME = "extra_vehicle_name";
    public static final String EXTRA_DRIVER_ID = "extra_driver_id";
    public static final String EXTRA_DRIVER_NAME = "extra_driver_name";
    public static final String EXTRA_DRIVER_PHONE = "extra_driver_phone";
    public static final String EXTRA_ROUTE = "extra_route";
    public static final String EXTRA_FARE = "extra_fare";
    public static final String EXTRA_POINTS = "extra_points";

    private MaterialButton registerTripButton;
    private String vehicleId;
    private String unitNumber;
    private String plate;
    private String vehicleName;
    private String driverId;
    private String driverName;
    private String driverPhone;
    private String route;
    private double fare;
    private long points;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(getColor(R.color.green_dark));
        getWindow().setNavigationBarColor(getColor(R.color.background_light));

        setContentView(R.layout.activity_confirm_trip);
        readIntentData();
        bindViews();
    }

    private void readIntentData() {
        vehicleId = getIntent().getStringExtra(EXTRA_VEHICLE_ID);
        unitNumber = getIntent().getStringExtra(EXTRA_UNIT_NUMBER);
        plate = getIntent().getStringExtra(EXTRA_PLATE);
        vehicleName = getIntent().getStringExtra(EXTRA_VEHICLE_NAME);
        driverId = getIntent().getStringExtra(EXTRA_DRIVER_ID);
        driverName = getIntent().getStringExtra(EXTRA_DRIVER_NAME);
        driverPhone = getIntent().getStringExtra(EXTRA_DRIVER_PHONE);
        route = getIntent().getStringExtra(EXTRA_ROUTE);
        fare = getIntent().getDoubleExtra(EXTRA_FARE, 3.0);
        points = POINTS_PER_TRIP;

        vehicleId = vehicleId != null ? vehicleId : "";
        unitNumber = unitNumber != null ? unitNumber : "Unidad";
        plate = plate != null ? plate : "Sin placa";
        vehicleName = vehicleName != null ? vehicleName : "Vehiculo del comite";
        driverId = driverId != null ? driverId : "";
        driverName = driverName != null ? driverName : "Conductor no asignado";
        driverPhone = driverPhone != null ? driverPhone : "";
        route = route != null ? route : "Cura Mori - Catacaos";
    }

    private void bindViews() {
        TextView routeText = findViewById(R.id.confirmRouteText);
        TextView fareText = findViewById(R.id.confirmFareText);
        TextView pointsText = findViewById(R.id.confirmPointsText);
        TextView unitText = findViewById(R.id.confirmUnitText);
        TextView plateText = findViewById(R.id.confirmPlateText);
        TextView driverText = findViewById(R.id.confirmDriverText);
        ImageButton backButton = findViewById(R.id.confirmBackButton);
        MaterialButton reportButton = findViewById(R.id.reportFromTripButton);
        registerTripButton = findViewById(R.id.registerTripButton);

        routeText.setText(route);
        fareText.setText("S/ " + String.format("%.2f", fare));
        pointsText.setText("+" + points + " puntos");
        unitText.setText(unitNumber);
        plateText.setText(plate);
        driverText.setText(driverName);

        backButton.setOnClickListener(view -> finish());
        registerTripButton.setOnClickListener(view -> registerTrip());
        reportButton.setOnClickListener(view -> openLinkedReport());
    }

    private void registerTrip() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesion", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        DatabaseReference root = FirebaseRefs.root();
        String tripId = root.child("viajes").push().getKey();
        String movementId = root.child("movimientos_puntos").push().getKey();

        if (tripId == null || movementId == null) {
            setLoading(false);
            Toast.makeText(this, "No se pudo registrar el viaje", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> trip = new HashMap<>();
        trip.put("id", tripId);
        trip.put("pasajeroUid", currentUser.getUid());
        trip.put("vehiculoId", vehicleId);
        trip.put("numeroUnidad", unitNumber);
        trip.put("placa", plate);
        trip.put("vehiculoNombre", vehicleName);
        trip.put("conductorId", driverId);
        trip.put("conductorNombre", driverName);
        trip.put("conductorCelular", driverPhone);
        trip.put("ruta", route);
        trip.put("tarifa", fare);
        trip.put("puntosGanados", points);
        trip.put("estado", "registrado");
        trip.put("fecha", ServerValue.TIMESTAMP);

        Map<String, Object> movement = new HashMap<>();
        movement.put("id", movementId);
        movement.put("pasajeroUid", currentUser.getUid());
        movement.put("tipo", "ganancia");
        movement.put("motivo", "Viaje registrado");
        movement.put("viajeId", tripId);
        movement.put("puntos", points);
        movement.put("fecha", ServerValue.TIMESTAMP);

        Map<String, Object> updates = new HashMap<>();
        updates.put("viajes/" + tripId, trip);
        updates.put("movimientos_puntos/" + movementId, movement);
        updates.put("pasajeros/" + currentUser.getUid() + "/viajes_historial/" + tripId, trip);
        updates.put("pasajeros/" + currentUser.getUid() + "/movimientos_puntos/" + movementId, movement);
        updates.put("pasajeros/" + currentUser.getUid() + "/puntos", ServerValue.increment(points));
        updates.put("pasajeros/" + currentUser.getUid() + "/viajes", ServerValue.increment(1));

        root.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Viaje registrado. Ganaste " + points + " puntos", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(error -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void openLinkedReport() {
        Intent intent = new Intent(ConfirmTripActivity.this, ReportActivity.class);
        intent.putExtra(ReportActivity.EXTRA_VEHICLE_ID, vehicleId);
        intent.putExtra(ReportActivity.EXTRA_PLATE, plate);
        intent.putExtra(ReportActivity.EXTRA_DRIVER_ID, driverId);
        intent.putExtra(ReportActivity.EXTRA_DRIVER_NAME, driverName);
        startActivity(intent);
    }

    private void setLoading(boolean isLoading) {
        registerTripButton.setEnabled(!isLoading);
        registerTripButton.setText(isLoading ? "Registrando..." : getString(R.string.register_trip_button));
    }
}

