package com.yhian.taxiapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.yhian.taxiapp.utils.FirebaseRefs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TripDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DATE = "extra_date";
    public static final String EXTRA_PLATE = "extra_plate";
    public static final String EXTRA_VEHICLE_ID = "extra_vehicle_id";
    public static final String EXTRA_VEHICLE_NAME = "extra_vehicle_name";
    public static final String EXTRA_UNIT_NUMBER = "extra_unit_number";
    public static final String EXTRA_DRIVER_ID = "extra_driver_id";
    public static final String EXTRA_DRIVER_NAME = "extra_driver_name";
    public static final String EXTRA_DRIVER_PHONE = "extra_driver_phone";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(getColor(R.color.background_light));
        getWindow().setNavigationBarColor(getColor(R.color.background_light));

        setContentView(R.layout.activity_trip_detail);
        bindViews();
    }

    private void bindViews() {
        ImageButton backButton = findViewById(R.id.tripDetailBackButton);
        TextView dateText = findViewById(R.id.detailDateText);
        TextView plateText = findViewById(R.id.detailPlateText);
        TextView vehicleText = findViewById(R.id.detailVehicleText);
        TextView driverText = findViewById(R.id.detailDriverText);
        TextView driverPhoneText = findViewById(R.id.detailDriverPhoneText);

        long date = getIntent().getLongExtra(EXTRA_DATE, 0L);
        String plate = getIntent().getStringExtra(EXTRA_PLATE);
        String vehicleId = getIntent().getStringExtra(EXTRA_VEHICLE_ID);
        String vehicleName = getIntent().getStringExtra(EXTRA_VEHICLE_NAME);
        String unitNumber = getIntent().getStringExtra(EXTRA_UNIT_NUMBER);
        String driverId = getIntent().getStringExtra(EXTRA_DRIVER_ID);
        String driverName = getIntent().getStringExtra(EXTRA_DRIVER_NAME);
        String driverPhone = getIntent().getStringExtra(EXTRA_DRIVER_PHONE);

        backButton.setOnClickListener(view -> finish());
        dateText.setText(formatDate(date));
        plateText.setText(valueOrDefault(plate, "Sin placa"));
        vehicleText.setText(buildVehicleLine(vehicleName, unitNumber));
        driverText.setText(valueOrDefault(driverName, "Conductor no asignado"));
        driverPhoneText.setText(valueOrDefault(driverPhone, "Sin celular registrado"));
        loadVehicleAndDriverData(vehicleId, driverId, unitNumber, vehicleText, driverText, driverPhoneText);
    }

    private void loadVehicleAndDriverData(String vehicleId, String driverId, String unitNumber, TextView vehicleText,
                                          TextView driverText, TextView driverPhoneText) {
        if (vehicleId == null || vehicleId.trim().isEmpty()) {
            loadDriverFromFirebase(driverId, driverText, driverPhoneText);
            return;
        }

        FirebaseRefs.root().child("vehiculos").child(vehicleId.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String auto = firstStringValue(snapshot, "auto", "vehiculoNombre", "nombreVehiculo", "");
                            if (auto.isEmpty()) {
                                auto = getSnapshotString(snapshot, "modelo");
                            }
                            String unit = firstStringValue(snapshot, "numeroUnidad", "unidad", "codigo", unitNumber);
                            if (!auto.isEmpty()) {
                                vehicleText.setText(buildVehicleLine(auto, unit));
                            }

                            String linkedDriverId = getSnapshotString(snapshot, "conductorId");
                            if (driverId == null || driverId.trim().isEmpty()) {
                                loadDriverFromFirebase(linkedDriverId, driverText, driverPhoneText);
                                return;
                            }
                        }

                        loadDriverFromFirebase(driverId, driverText, driverPhoneText);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        loadDriverFromFirebase(driverId, driverText, driverPhoneText);
                    }
                });
    }

    private void loadDriverFromFirebase(String driverId, TextView driverText, TextView driverPhoneText) {
        if (driverId == null || driverId.trim().isEmpty()) {
            return;
        }

        FirebaseRefs.root().child("conductores").child(driverId.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            return;
                        }

                        String name = firstStringValue(snapshot, "nombre", "nombreCompleto", "conductorNombre", "");
                        String phone = firstStringValue(snapshot, "celular", "telefono", "conductorCelular", "");
                        if (phone.isEmpty()) {
                            phone = firstStringValue(snapshot, "numeroCelular", "telefonoCelular", "celularConductor", "");
                        }

                        if (!name.isEmpty()) {
                            driverText.setText(name);
                        }
                        if (!phone.isEmpty()) {
                            driverPhoneText.setText(phone);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // El detalle mantiene los datos del viaje si Firebase no responde.
                    }
                });
    }

    private String firstStringValue(DataSnapshot snapshot, String firstKey, String secondKey, String thirdKey, String fallback) {
        String first = getSnapshotString(snapshot, firstKey);
        if (!first.isEmpty()) {
            return first;
        }

        String second = getSnapshotString(snapshot, secondKey);
        if (!second.isEmpty()) {
            return second;
        }

        String third = getSnapshotString(snapshot, thirdKey);
        return third.isEmpty() ? valueOrDefault(fallback, "") : third;
    }

    private String getSnapshotString(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        if (value == null) {
            return "";
        }

        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "" : text;
    }

    private String buildVehicleLine(String vehicleName, String unitNumber) {
        String name = valueOrDefault(vehicleName, "Vehiculo del comite");
        String unit = valueOrDefault(unitNumber, "Unidad");
        return name + " | " + unit;
    }

    private String valueOrDefault(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return "Fecha pendiente";
        }

        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM, hh:mm a", new Locale("es", "PE"));
        return formatter.format(new Date(timestamp))
                .replace("a. m.", "AM")
                .replace("p. m.", "PM");
    }
}
