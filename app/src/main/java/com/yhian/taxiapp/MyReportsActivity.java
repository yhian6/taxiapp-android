package com.yhian.taxiapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.yhian.taxiapp.models.ReportItem;
import com.yhian.taxiapp.utils.FirebaseRefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyReportsActivity extends AppCompatActivity {

    private TextView totalReportsText;
    private TextView pendingReportsText;
    private TextView emptyReportsText;
    private LinearLayout reportsContainer;
    private DatabaseReference reportsReference;
    private ValueEventListener reportsListener;
    private String currentUid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.background_light));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.background_light));
        setContentView(R.layout.activity_my_reports);

        ImageButton backButton = findViewById(R.id.myReportsBackButton);
        totalReportsText = findViewById(R.id.myReportsTotalText);
        pendingReportsText = findViewById(R.id.myReportsPendingText);
        emptyReportsText = findViewById(R.id.myReportsEmptyText);
        reportsContainer = findViewById(R.id.myReportsContainer);

        backButton.setOnClickListener(view -> finish());
        loadReports();
    }

    private void loadReports() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showEmptyState();
            Toast.makeText(this, "Inicia sesion para ver tus reportes", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUid = currentUser.getUid();
        reportsReference = FirebaseRefs.root().child("reportes");
        reportsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ReportItem> reports = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ReportItem report = child.getValue(ReportItem.class);
                    if (report == null) {
                        continue;
                    }
                    if (report.getId() == null || report.getId().trim().isEmpty()) {
                        report.setId(child.getKey());
                    }
                    if (currentUid.equals(report.getPasajeroUid())) {
                        reports.add(report);
                    }
                }

                Collections.sort(reports, (first, second) -> Long.compare(second.getFecha(), first.getFecha()));
                renderReports(reports);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyReportsActivity.this, "No se pudieron cargar tus reportes", Toast.LENGTH_SHORT).show();
            }
        };

        reportsReference.addValueEventListener(reportsListener);
    }

    private void renderReports(List<ReportItem> reports) {
        reportsContainer.removeAllViews();
        totalReportsText.setText(String.valueOf(reports.size()));

        int pendingCount = 0;
        for (ReportItem report : reports) {
            if (isPending(report.getEstado())) {
                pendingCount++;
            }
        }
        pendingReportsText.setText(String.valueOf(pendingCount));

        if (reports.isEmpty()) {
            showEmptyState();
            return;
        }

        emptyReportsText.setVisibility(View.GONE);
        reportsContainer.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ReportItem report : reports) {
            View card = inflater.inflate(R.layout.item_my_report, reportsContainer, false);
            bindReportCard(card, report);
            reportsContainer.addView(card);
        }
    }

    private void bindReportCard(View card, ReportItem report) {
        TextView typeText = card.findViewById(R.id.reportTypeText);
        TextView dateText = card.findViewById(R.id.reportDateText);
        TextView statusText = card.findViewById(R.id.reportStatusText);
        TextView descriptionText = card.findViewById(R.id.reportDescriptionText);
        TextView vehicleText = card.findViewById(R.id.reportVehicleText);
        TextView driverText = card.findViewById(R.id.reportDriverText);
        LinearLayout vehicleBlock = card.findViewById(R.id.reportVehicleBlock);

        typeText.setText(valueOrDefault(report.getTipo(), "Reporte de servicio"));
        dateText.setText(formatDate(report.getFecha()));
        descriptionText.setText(valueOrDefault(report.getDescripcion(), "Sin descripcion registrada."));
        configureStatus(statusText, report.getEstado());

        String plate = valueOrDefault(report.getPlaca(), "");
        String driver = valueOrDefault(report.getConductorNombre(), "");
        boolean hasVehicleInfo = !plate.isEmpty() || !driver.isEmpty();
        vehicleBlock.setVisibility(hasVehicleInfo ? View.VISIBLE : View.GONE);
        if (hasVehicleInfo) {
            vehicleText.setText(plate.isEmpty() ? "Reporte general" : "Unidad " + plate);
            driverText.setText(driver.isEmpty() ? "Conductor pendiente de validar" : "Conductor " + driver);
        }
    }

    private void configureStatus(TextView statusText, String rawStatus) {
        String status = valueOrDefault(rawStatus, "pendiente").toLowerCase(Locale.ROOT);
        if (status.contains("rechaz") || status.contains("cerr")) {
            statusText.setText("Cerrado");
            statusText.setTextColor(ContextCompat.getColor(this, R.color.white));
            statusText.setBackgroundResource(R.drawable.bg_report_status_closed);
        } else if (status.contains("aprob") || status.contains("revis") || status.contains("resuel")) {
            statusText.setText(status.contains("resuel") ? "Resuelto" : "Revisado");
            statusText.setTextColor(ContextCompat.getColor(this, R.color.white));
            statusText.setBackgroundResource(R.drawable.bg_report_status_reviewed);
        } else {
            statusText.setText("Pendiente");
            statusText.setTextColor(ContextCompat.getColor(this, R.color.blue_deep));
            statusText.setBackgroundResource(R.drawable.bg_report_status_pending);
        }
    }

    private void showEmptyState() {
        totalReportsText.setText("0");
        pendingReportsText.setText("0");
        emptyReportsText.setVisibility(View.VISIBLE);
        reportsContainer.setVisibility(View.GONE);
    }

    private boolean isPending(String status) {
        return valueOrDefault(status, "pendiente").toLowerCase(Locale.ROOT).contains("pend");
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return "Fecha pendiente";
        }

        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM, hh:mm a", new Locale("es", "PE"));
        return formatter.format(new Date(timestamp));
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reportsReference != null && reportsListener != null) {
            reportsReference.removeEventListener(reportsListener);
        }
    }
}
