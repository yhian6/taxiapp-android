package com.yhian.taxiapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.google.zxing.ResultPoint;
import com.yhian.taxiapp.utils.FirebaseRefs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QrScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 2001;
    private static final long SCAN_COOLDOWN_MS = 60 * 60 * 1000; // 1 hora de espera para la misma unidad

    private DecoratedBarcodeView barcodeScannerView;
    private View scannerLine;
    private TextView scannerStatusText;
    private MaterialCardView vehicleDetailsPanel;
    private TextView scannedPlateText;
    private TextView scannedUnitText;
    private TextView scannedDriverText;
    private TextView scannedRouteText;
    private MaterialButton confirmScannedTripButton;
    private MaterialButton reportScannedTripButton;
    private DatabaseReference vehiclesReference;
    private ObjectAnimator scannerLineAnimator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean hasScanned = false;
    private boolean isScannerRunning = false;
    private boolean canRegisterPoints = true;
    private boolean isCheckingPointsAvailability = false;
    private long pointsCooldownRemainingMs = 0L;

    private String vehicleId = "";
    private String unitNumber = "Unidad";
    private String plate = "Sin placa";
    private String vehicleName = "Vehiculo del comite";
    private String driverId = "";
    private String driverName = "Conductor no asignado";
    private String driverPhone = "";
    private String route = "Cura Mori - Catacaos";
    private double fare = 3.0;

    private final BarcodeCallback barcodeCallback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (hasScanned || result == null || result.getText() == null) {
                return;
            }

            String qrCode = result.getText().trim();
            if (qrCode.isEmpty()) {
                return;
            }

            hasScanned = true;
            pauseScanner();
            validateQrCode(qrCode);
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            // ZXing callback required for preview points; no UI action needed here.
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#07111F"));
        getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#07111F"));

        setContentView(R.layout.activity_qr_scanner);
        bindViews();
        vehiclesReference = FirebaseRefs.root().child("vehiculos");

        startScannerLineAnimation();
        startQrScanner();
    }

    private void bindViews() {
        barcodeScannerView = findViewById(R.id.barcodeScannerView);
        scannerLine = findViewById(R.id.scannerLine);
        scannerStatusText = findViewById(R.id.scannerStatusText);
        vehicleDetailsPanel = findViewById(R.id.vehicleDetailsPanel);
        scannedPlateText = findViewById(R.id.scannedPlateText);
        scannedUnitText = findViewById(R.id.scannedUnitText);
        scannedDriverText = findViewById(R.id.scannedDriverText);
        scannedRouteText = findViewById(R.id.scannedRouteText);
        confirmScannedTripButton = findViewById(R.id.confirmScannedTripButton);
        reportScannedTripButton = findViewById(R.id.reportScannedTripButton);
        ImageButton backButton = findViewById(R.id.qrBackButton);

        barcodeScannerView.setStatusText("");
        backButton.setOnClickListener(view -> finish());
        confirmScannedTripButton.setOnClickListener(view -> handleTripConfirmation());
        reportScannedTripButton.setOnClickListener(view -> openLinkedReport());
    }

    private void startQrScanner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }

        vehicleDetailsPanel.setVisibility(View.GONE);
        scannerStatusText.setText("Alinea el codigo dentro del marco");
        canRegisterPoints = true;
        isCheckingPointsAvailability = false;
        pointsCooldownRemainingMs = 0L;
        confirmScannedTripButton.setEnabled(true);
        confirmScannedTripButton.setAlpha(1f);
        confirmScannedTripButton.setText("Confirmar +3 pts");
        hasScanned = false;
        isScannerRunning = true;
        barcodeScannerView.decodeSingle(barcodeCallback);
        barcodeScannerView.resume();
        startScannerLineAnimation();
    }

    private void pauseScanner() {
        isScannerRunning = false;
        barcodeScannerView.pause();
        stopScannerLineAnimation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQrScanner();
            } else {
                scannerStatusText.setText("Activa el permiso de camara para escanear.");
                Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void validateQrCode(String qrCode) {
        scannerStatusText.setText("Validando unidad...");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchVehicleData(qrCode);
    }

    private void fetchVehicleData(String qrCode) {
        vehiclesReference.child(qrCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            scannerStatusText.setText("QR no registrado. Intenta con otra unidad.");
                            Toast.makeText(QrScannerActivity.this, "QR no registrado: " + qrCode, Toast.LENGTH_SHORT).show();
                            restartScannerDelayed();
                            return;
                        }

                        Boolean active = snapshot.child("activo").getValue(Boolean.class);
                        if (active != null && !active) {
                            scannerStatusText.setText("Esta unidad no esta activa.");
                            Toast.makeText(QrScannerActivity.this, "Unidad inactiva", Toast.LENGTH_SHORT).show();
                            restartScannerDelayed();
                            return;
                        }

                        fillVehicleData(qrCode, snapshot);
                        hydrateDriverDataThenShow();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        scannerStatusText.setText("No se pudo validar el codigo.");
                        Toast.makeText(QrScannerActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        restartScannerDelayed();
                    }
                });
    }

    private void fillVehicleData(String scannedVehicleId, DataSnapshot snapshot) {
        vehicleId = scannedVehicleId;
        unitNumber = getStringValue(snapshot, "numeroUnidad", "Unidad");
        plate = getStringValue(snapshot, "placa", "Sin placa");
        
        // Soporta el campo nuevo del panel admin y datos antiguos ya registrados.
        vehicleName = firstStringValue(snapshot, "carro", "auto", "vehiculoNombre", "");
        if (vehicleName.isEmpty()) {
            vehicleName = firstStringValue(snapshot, "nombreVehiculo", "marca", "modelo", "Vehiculo del comite");
        }
        
        driverId = getStringValue(snapshot, "conductorId", "");
        driverName = getStringValue(snapshot, "conductorNombre", "Conductor no asignado");
        driverPhone = firstStringValue(snapshot, "conductorCelular", "conductorTelefono", "celularConductor", "");
        if (driverPhone.isEmpty()) {
            driverPhone = firstStringValue(snapshot, "celular", "telefono", "numeroCelular", "");
        }
        route = getStringValue(snapshot, "ruta", "Cura Mori - Catacaos");
        fare = getDoubleValue(snapshot, "tarifa", 3.0);
    }

    private void hydrateDriverDataThenShow() {
        if (driverId == null || driverId.trim().isEmpty()) {
            showVehiclePanel();
            return;
        }

        FirebaseRefs.root().child("conductores").child(driverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            driverName = firstStringValue(snapshot, "nombre", "nombreCompleto", "conductorNombre", driverName);
                            driverPhone = firstStringValue(snapshot, "celular", "telefono", "conductorCelular", driverPhone);
                            if (driverPhone.isEmpty()) {
                                driverPhone = firstStringValue(snapshot, "numeroCelular", "telefonoCelular", "celularConductor", "");
                            }
                        }
                        showVehiclePanel();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showVehiclePanel();
                    }
                });
    }

    private void showVehiclePanel() {
        scannerStatusText.setText("Unidad encontrada");
        scannedPlateText.setText(plate);
        // Cambiamos unitNumber por vehicleName (marca del carro)
        scannedUnitText.setText(vehicleName + " | S/ " + String.format("%.2f", fare));
        scannedDriverText.setText(driverPhone == null || driverPhone.trim().isEmpty()
                ? driverName
                : driverName + " | " + driverPhone);
        scannedRouteText.setText(route);
        vehicleDetailsPanel.setVisibility(View.VISIBLE);
        vehicleDetailsPanel.setAlpha(0f);
        vehicleDetailsPanel.setTranslationY(dpToPx(28));
        vehicleDetailsPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260)
                .start();
        isCheckingPointsAvailability = true;
        confirmScannedTripButton.setEnabled(false);
        confirmScannedTripButton.setAlpha(0.72f);
        confirmScannedTripButton.setText("Verificando...");
        checkPointsAvailability();
    }

    private void restartScannerDelayed() {
        handler.postDelayed(this::startQrScanner, 1300L);
    }

    private void checkPointsAvailability() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || vehicleId.trim().isEmpty()) {
            return;
        }

        FirebaseRefs.root().child("pasajeros").child(currentUser.getUid())
                .child("ultimos_escaneos").child(vehicleId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long lastScanTime = snapshot.getValue(Long.class);
                        long remainingMillis = getRemainingCooldown(lastScanTime);
                        updatePointsButtonState(remainingMillis);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        updatePointsButtonState(0L);
                    }
                });
    }

    private long getRemainingCooldown(Long lastScanTime) {
        if (lastScanTime == null) {
            return 0L;
        }

        long elapsed = System.currentTimeMillis() - lastScanTime;
        return Math.max(0L, SCAN_COOLDOWN_MS - elapsed);
    }

    private void updatePointsButtonState(long remainingMillis) {
        isCheckingPointsAvailability = false;
        pointsCooldownRemainingMs = remainingMillis;
        canRegisterPoints = remainingMillis <= 0L;
        confirmScannedTripButton.setEnabled(true);

        if (canRegisterPoints) {
            confirmScannedTripButton.setText("Confirmar +3 pts");
            confirmScannedTripButton.setAlpha(1f);
            return;
        }

        confirmScannedTripButton.setText("Puntos en " + formatCooldownShort(remainingMillis));
        confirmScannedTripButton.setAlpha(0.72f);
    }

    private String formatCooldownShort(long millis) {
        long totalMinutes = Math.max(1L, (long) Math.ceil(millis / 60000.0));
        return totalMinutes == 1L ? "1 min" : totalMinutes + " min";
    }

    private void handleTripConfirmation() {
        if (isCheckingPointsAvailability) {
            Toast.makeText(this, "Estamos validando tus puntos, espera un momento", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!canRegisterPoints) {
            showCooldownAlert(pointsCooldownRemainingMs);
            return;
        }

        registerTripFromScanner();
    }

    private void showCooldownAlert(long remainingMillis) {
        String timeText = formatCooldownShort(remainingMillis);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Tus puntos ya fueron registrados")
                .setMessage("Ya ganaste los 3 puntos de este viaje. Podras volver a sumar puntos en esta unidad dentro de "
                        + timeText + ".\n\nSi hubo algun problema con el conductor o el servicio, puedes enviar un reporte ahora.")
                .setIcon(R.drawable.ic_points)
                .setNegativeButton("Cerrar", null)
                .setPositiveButton("Reportar ahora", (dialog, which) -> openLinkedReport())
                .show();
    }

    private void registerTripFromScanner() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesion", Toast.LENGTH_SHORT).show();
            return;
        }

        setConfirmLoading(true);
        DatabaseReference root = FirebaseRefs.root();
        String tripId = root.child("viajes").push().getKey();
        String movementId = root.child("movimientos_puntos").push().getKey();

        if (tripId == null || movementId == null) {
            setConfirmLoading(false);
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
        trip.put("puntosGanados", 3L);
        trip.put("estado", "registrado");
        trip.put("fecha", ServerValue.TIMESTAMP);

        Map<String, Object> movement = new HashMap<>();
        movement.put("id", movementId);
        movement.put("pasajeroUid", currentUser.getUid());
        movement.put("tipo", "ganancia");
        movement.put("motivo", "Viaje registrado");
        movement.put("viajeId", tripId);
        movement.put("puntos", 3L);
        movement.put("fecha", ServerValue.TIMESTAMP);

        Map<String, Object> updates = new HashMap<>();
        updates.put("viajes/" + tripId, trip);
        updates.put("movimientos_puntos/" + movementId, movement);
        updates.put("pasajeros/" + currentUser.getUid() + "/viajes_historial/" + tripId, trip);
        updates.put("pasajeros/" + currentUser.getUid() + "/movimientos_puntos/" + movementId, movement);
        updates.put("pasajeros/" + currentUser.getUid() + "/puntos", ServerValue.increment(3L));
        updates.put("pasajeros/" + currentUser.getUid() + "/viajes", ServerValue.increment(1));
        updates.put("pasajeros/" + currentUser.getUid() + "/ultimos_escaneos/" + vehicleId, ServerValue.TIMESTAMP);

        root.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    setConfirmLoading(false);
                    openTripSuccessDetail();
                })
                .addOnFailureListener(error -> {
                    setConfirmLoading(false);
                    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void openTripSuccessDetail() {
        Intent intent = new Intent(QrScannerActivity.this, TripSuccessActivity.class);
        intent.putExtra(TripSuccessActivity.EXTRA_DATE, System.currentTimeMillis());
        intent.putExtra(TripSuccessActivity.EXTRA_PLATE, plate);
        intent.putExtra(TripSuccessActivity.EXTRA_VEHICLE_ID, vehicleId);
        intent.putExtra(TripSuccessActivity.EXTRA_VEHICLE_NAME, vehicleName);
        intent.putExtra(TripSuccessActivity.EXTRA_UNIT_NUMBER, unitNumber);
        intent.putExtra(TripSuccessActivity.EXTRA_DRIVER_ID, driverId);
        intent.putExtra(TripSuccessActivity.EXTRA_DRIVER_NAME, driverName);
        intent.putExtra(TripSuccessActivity.EXTRA_DRIVER_PHONE, driverPhone);
        startActivity(intent);
        finish();
    }

    private void setConfirmLoading(boolean isLoading) {
        confirmScannedTripButton.setEnabled(!isLoading);
        reportScannedTripButton.setEnabled(!isLoading);
        confirmScannedTripButton.setText(isLoading ? "Registrando..." : "Confirmar +3 pts");
    }

    private void openLinkedReport() {
        Intent intent = new Intent(QrScannerActivity.this, ReportActivity.class);
        intent.putExtra(ReportActivity.EXTRA_VEHICLE_ID, vehicleId);
        intent.putExtra(ReportActivity.EXTRA_PLATE, plate);
        intent.putExtra(ReportActivity.EXTRA_DRIVER_ID, driverId);
        intent.putExtra(ReportActivity.EXTRA_DRIVER_NAME, driverName);
        startActivity(intent);
    }

    private void startScannerLineAnimation() {
        if (scannerLine == null || scannerLineAnimator != null && scannerLineAnimator.isStarted()) {
            return;
        }

        scannerLine.post(() -> {
            stopScannerLineAnimation();
            scannerLineAnimator = ObjectAnimator.ofFloat(scannerLine, "translationY", -dpToPx(82), dpToPx(82));
            scannerLineAnimator.setDuration(1200L);
            scannerLineAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            scannerLineAnimator.setRepeatMode(ObjectAnimator.REVERSE);
            scannerLineAnimator.start();
        });
    }

    private void stopScannerLineAnimation() {
        if (scannerLineAnimator != null) {
            scannerLineAnimator.cancel();
            scannerLineAnimator = null;
        }
        if (scannerLine != null) {
            scannerLine.setTranslationY(0f);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasScanned && barcodeScannerView != null && hasCameraPermission()) {
            barcodeScannerView.resume();
            if (!isScannerRunning) {
                startQrScanner();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeScannerView != null) {
            barcodeScannerView.pause();
        }
        stopScannerLineAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        stopScannerLineAnimation();
    }

    private boolean hasCameraPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private String getStringValue(DataSnapshot snapshot, String key, String fallback) {
        Object value = snapshot.child(key).getValue();
        if (value == null) {
            return fallback;
        }

        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private String firstStringValue(DataSnapshot snapshot, String firstKey, String secondKey, String thirdKey, String fallback) {
        String first = getStringValue(snapshot, firstKey, "");
        if (!first.isEmpty()) {
            return first;
        }

        String second = getStringValue(snapshot, secondKey, "");
        if (!second.isEmpty()) {
            return second;
        }

        return getStringValue(snapshot, thirdKey, fallback);
    }

    private double getDoubleValue(DataSnapshot snapshot, String key, double fallback) {
        Object value = snapshot.child(key).getValue();

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            return value != null ? Double.parseDouble(String.valueOf(value)) : fallback;
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}








