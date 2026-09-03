package com.yhian.taxiapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.yhian.taxiapp.utils.FirebaseRefs;

import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    public static final String EXTRA_VEHICLE_ID = "extra_vehicle_id";
    public static final String EXTRA_PLATE = "extra_plate";
    public static final String EXTRA_DRIVER_ID = "extra_driver_id";
    public static final String EXTRA_DRIVER_NAME = "extra_driver_name";

    private Spinner reportTypeSpinner;
    private TextInputEditText descriptionInput;
    private MaterialButton sendReportButton;
    private TextView linkedVehicleText;
    private TextView linkedDriverText;
    private DatabaseReference passengersReference;
    private DatabaseReference reportsReference;

    private String passengerName = "";
    private String passengerPhone = "";
    private String vehicleId = "";
    private String plate = "";
    private String driverId = "";
    private String driverName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(getColor(R.color.green_dark));
        getWindow().setNavigationBarColor(getColor(R.color.background_light));

        setContentView(R.layout.activity_report);

        reportTypeSpinner = findViewById(R.id.reportTypeSpinner);
        descriptionInput = findViewById(R.id.reportDescriptionInput);
        sendReportButton = findViewById(R.id.sendReportButton);
        linkedVehicleText = findViewById(R.id.linkedVehicleText);
        linkedDriverText = findViewById(R.id.linkedDriverText);
        ImageButton backButton = findViewById(R.id.reportBackButton);

        passengersReference = FirebaseRefs.root().child("pasajeros");
        reportsReference = FirebaseRefs.root().child("reportes");

        readLinkedServiceData();
        setupReportTypes();
        loadPassengerData();

        backButton.setOnClickListener(view -> finish());
        sendReportButton.setOnClickListener(view -> validateAndSendReport());
    }

    private void readLinkedServiceData() {
        vehicleId = getIntent().getStringExtra(EXTRA_VEHICLE_ID);
        plate = getIntent().getStringExtra(EXTRA_PLATE);
        driverId = getIntent().getStringExtra(EXTRA_DRIVER_ID);
        driverName = getIntent().getStringExtra(EXTRA_DRIVER_NAME);

        vehicleId = vehicleId != null ? vehicleId : "";
        plate = plate != null ? plate : "";
        driverId = driverId != null ? driverId : "";
        driverName = driverName != null ? driverName : "";

        linkedVehicleText.setText(plate.isEmpty() ? "Vehiculo: reporte general" : "Vehiculo: " + plate);
        linkedDriverText.setText(driverName.isEmpty() ? "Conductor: pendiente de QR" : "Conductor: " + driverName);
    }

    private void setupReportTypes() {
        String[] reportTypes = {
                "Mala atencion",
                "Exceso de velocidad",
                "Cobro incorrecto",
                "Vehiculo en mal estado",
                "No respeto la ruta",
                "Otro"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                reportTypes
        );
        reportTypeSpinner.setAdapter(adapter);
    }

    private void loadPassengerData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            return;
        }

        passengersReference.child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("nombre").getValue(String.class);
                        String phone = snapshot.child("celular").getValue(String.class);
                        passengerName = name != null ? name : "";
                        passengerPhone = phone != null ? phone : "";
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        passengerName = "";
                        passengerPhone = "";
                    }
                });
    }

    private void validateAndSendReport() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String type = String.valueOf(reportTypeSpinner.getSelectedItem());
        String description = String.valueOf(descriptionInput.getText()).trim();

        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesion para reportar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.length() < 10) {
            descriptionInput.setError("Describe mejor el problema");
            return;
        }

        setLoading(true);
        String reportId = reportsReference.push().getKey();

        if (reportId == null) {
            setLoading(false);
            Toast.makeText(this, "No se pudo crear el reporte", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> report = new HashMap<>();
        report.put("id", reportId);
        report.put("pasajeroUid", currentUser.getUid());
        report.put("pasajeroNombre", passengerName);
        report.put("pasajeroCelular", passengerPhone);
        report.put("tipo", type);
        report.put("descripcion", description);
        report.put("estado", "pendiente");
        report.put("vehiculoId", vehicleId);
        report.put("placa", plate);
        report.put("conductorId", driverId);
        report.put("conductorNombre", driverName);
        report.put("origen", plate.isEmpty() && driverName.isEmpty() ? "general" : "qr");
        report.put("fecha", ServerValue.TIMESTAMP);

        reportsReference.child(reportId).setValue(report)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Reporte enviado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(error -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        sendReportButton.setEnabled(!isLoading);
        sendReportButton.setText(isLoading ? "Enviando..." : getString(R.string.send_report_button));
    }
}
