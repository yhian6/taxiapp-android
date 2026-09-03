package com.yhian.taxiapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TripSuccessActivity extends AppCompatActivity {

    public static final String EXTRA_DATE = "extra_date";
    public static final String EXTRA_PLATE = "extra_plate";
    public static final String EXTRA_VEHICLE_ID = "extra_vehicle_id";
    public static final String EXTRA_VEHICLE_NAME = "extra_vehicle_name";
    public static final String EXTRA_UNIT_NUMBER = "extra_unit_number";
    public static final String EXTRA_DRIVER_ID = "extra_driver_id";
    public static final String EXTRA_DRIVER_NAME = "extra_driver_name";
    public static final String EXTRA_DRIVER_PHONE = "extra_driver_phone";

    private long date;
    private String plate;
    private String vehicleId;
    private String vehicleName;
    private String unitNumber;
    private String driverId;
    private String driverName;
    private String driverPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(getColor(R.color.background_light));
        getWindow().setNavigationBarColor(getColor(R.color.background_light));

        setContentView(R.layout.activity_trip_success);
        readExtras();
        bindViews();
    }

    private void readExtras() {
        date = getIntent().getLongExtra(EXTRA_DATE, System.currentTimeMillis());
        plate = getIntent().getStringExtra(EXTRA_PLATE);
        vehicleId = getIntent().getStringExtra(EXTRA_VEHICLE_ID);
        vehicleName = getIntent().getStringExtra(EXTRA_VEHICLE_NAME);
        unitNumber = getIntent().getStringExtra(EXTRA_UNIT_NUMBER);
        driverId = getIntent().getStringExtra(EXTRA_DRIVER_ID);
        driverName = getIntent().getStringExtra(EXTRA_DRIVER_NAME);
        driverPhone = getIntent().getStringExtra(EXTRA_DRIVER_PHONE);
    }

    private void bindViews() {
        ImageButton closeButton = findViewById(R.id.tripSuccessCloseButton);
        TextView dateText = findViewById(R.id.tripSuccessDateText);
        TextView plateText = findViewById(R.id.tripSuccessPlateText);
        TextView vehicleText = findViewById(R.id.tripSuccessVehicleText);
        TextView driverText = findViewById(R.id.tripSuccessDriverText);
        TextView driverPhoneText = findViewById(R.id.tripSuccessDriverPhoneText);
        MaterialButton doneButton = findViewById(R.id.tripSuccessDoneButton);

        dateText.setText(formatDate(date));
        plateText.setText(valueOrDefault(plate, "Sin placa"));
        vehicleText.setText(buildVehicleLine(vehicleName, unitNumber));
        driverText.setText(valueOrDefault(driverName, "Conductor no asignado"));
        driverPhoneText.setText(valueOrDefault(driverPhone, "Sin celular registrado"));

        closeButton.setOnClickListener(view -> finish());
        doneButton.setOnClickListener(view -> finish());
    }

    private void openTripDetail() {
        Intent intent = new Intent(TripSuccessActivity.this, TripDetailActivity.class);
        intent.putExtra(TripDetailActivity.EXTRA_DATE, date);
        intent.putExtra(TripDetailActivity.EXTRA_PLATE, plate);
        intent.putExtra(TripDetailActivity.EXTRA_VEHICLE_ID, vehicleId);
        intent.putExtra(TripDetailActivity.EXTRA_VEHICLE_NAME, vehicleName);
        intent.putExtra(TripDetailActivity.EXTRA_UNIT_NUMBER, unitNumber);
        intent.putExtra(TripDetailActivity.EXTRA_DRIVER_ID, driverId);
        intent.putExtra(TripDetailActivity.EXTRA_DRIVER_NAME, driverName);
        intent.putExtra(TripDetailActivity.EXTRA_DRIVER_PHONE, driverPhone);
        startActivity(intent);
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
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM, hh:mm a", new Locale("es", "PE"));
        return formatter.format(new Date(timestamp))
                .replace("a. m.", "AM")
                .replace("p. m.", "PM");
    }
}
