package com.yhian.taxiapp.fragments;

import android.content.Intent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.yhian.taxiapp.R;
import com.yhian.taxiapp.TripDetailActivity;
import com.yhian.taxiapp.models.TripItem;
import com.yhian.taxiapp.utils.FirebaseRefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Calendar;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private LinearLayout historyContainer;
    private TextView emptyStateText;
    private TextView totalTripsText;
    private TextView totalPointsText;
    private TextView filterText;
    private TextView summaryLabelText;
    private DatabaseReference historyReference;
    private ValueEventListener historyListener;
    private final List<TripItem> allTrips = new ArrayList<>();
    private HistoryFilter currentFilter = HistoryFilter.ALL;

    private enum HistoryFilter {
        ALL("Todo", "TOTAL VIAJES REGISTRADOS"),
        TODAY("Hoy", "TOTAL VIAJES HOY"),
        WEEK("Esta semana", "TOTAL VIAJES ESTA SEMANA"),
        FIFTEEN_DAYS("Ultimos 15 dias", "TOTAL VIAJES EN 15 DIAS"),
        MONTH("Este mes", "TOTAL VIAJES ESTE MES");

        final String label;
        final String summaryLabel;

        HistoryFilter(String label, String summaryLabel) {
            this.label = label;
            this.summaryLabel = summaryLabel;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        historyContainer = view.findViewById(R.id.historyContainer);
        emptyStateText = view.findViewById(R.id.historyEmptyText);
        totalTripsText = view.findViewById(R.id.historyTotalTripsText);
        totalPointsText = view.findViewById(R.id.historyTotalPointsText);
        filterText = view.findViewById(R.id.historyFilterText);
        summaryLabelText = view.findViewById(R.id.historySummaryLabelText);
        ImageButton filterButton = view.findViewById(R.id.historyFilterButton);

        filterButton.setOnClickListener(v -> showFilterDialog());

        loadHistory();
    }

    private void loadHistory() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            showEmptyState("Inicia sesion para ver tus viajes.");
            return;
        }

        historyReference = FirebaseRefs.root()
                .child("pasajeros")
                .child(currentUser.getUid())
                .child("viajes_historial");

        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTrips.clear();

                for (DataSnapshot tripSnapshot : snapshot.getChildren()) {
                    TripItem trip = tripSnapshot.getValue(TripItem.class);

                    if (trip != null) {
                        if (trip.getId() == null) {
                            trip.setId(tripSnapshot.getKey());
                        }
                        allTrips.add(trip);
                    }
                }

                Collections.sort(allTrips, (first, second) -> Long.compare(second.getFecha(), first.getFecha()));
                renderTrips();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showEmptyState("No se pudo cargar el historial.");
            }
        };

        historyReference.addValueEventListener(historyListener);
    }

    private void renderTrips() {
        historyContainer.removeAllViews();
        List<TripItem> trips = getFilteredTrips();

        filterText.setText("Filtro: " + currentFilter.label);
        summaryLabelText.setText(currentFilter.summaryLabel);

        if (trips.isEmpty()) {
            totalTripsText.setText("0");
            totalPointsText.setText("0 pts");
            showEmptyState(allTrips.isEmpty()
                    ? "Aun no tienes viajes registrados."
                    : "No hay viajes en este filtro.");
            return;
        }

        emptyStateText.setVisibility(View.GONE);

        for (TripItem trip : trips) {
            historyContainer.addView(createTripCard(trip));
        }

        totalTripsText.setText(String.valueOf(trips.size()));
        totalPointsText.setText((trips.size() * 3) + " pts");
    }

    private void showFilterDialog() {
        String[] options = {"Todo", "Hoy", "Esta semana", "Ultimos 15 dias", "Este mes"};
        HistoryFilter[] filters = {
                HistoryFilter.ALL,
                HistoryFilter.TODAY,
                HistoryFilter.WEEK,
                HistoryFilter.FIFTEEN_DAYS,
                HistoryFilter.MONTH
        };

        int selectedIndex = 0;
        for (int i = 0; i < filters.length; i++) {
            if (filters[i] == currentFilter) {
                selectedIndex = i;
                break;
            }
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filtrar historial")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    currentFilter = filters[which];
                    renderTrips();
                    dialog.dismiss();
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private List<TripItem> getFilteredTrips() {
        if (currentFilter == HistoryFilter.ALL) {
            return new ArrayList<>(allTrips);
        }

        List<TripItem> filteredTrips = new ArrayList<>();
        long startTime = getFilterStartTime(currentFilter);

        for (TripItem trip : allTrips) {
            if (trip.getFecha() >= startTime) {
                filteredTrips.add(trip);
            }
        }

        return filteredTrips;
    }

    private long getFilterStartTime(HistoryFilter filter) {
        Calendar calendar = Calendar.getInstance();

        if (filter == HistoryFilter.TODAY) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTimeInMillis();
        }

        if (filter == HistoryFilter.WEEK) {
            calendar.add(Calendar.DAY_OF_YEAR, -7);
            return calendar.getTimeInMillis();
        }

        if (filter == HistoryFilter.FIFTEEN_DAYS) {
            calendar.add(Calendar.DAY_OF_YEAR, -15);
            return calendar.getTimeInMillis();
        }

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private View createTripCard(TripItem trip) {
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_trip_history, historyContainer, false);

        TextView routeText = card.findViewById(R.id.tripRouteText);
        TextView dateText = card.findViewById(R.id.tripDateText);
        TextView unitText = card.findViewById(R.id.tripUnitText);
        TextView driverText = card.findViewById(R.id.tripDriverText);
        TextView fareText = card.findViewById(R.id.tripFareText);
        TextView pointsText = card.findViewById(R.id.tripPointsText);

        routeText.setText("Cura Mori - Catacaos");
        dateText.setText(formatDate(trip.getFecha()));
        unitText.setText(valueOrDefault(trip.getNumeroUnidad(), "Unidad") + " | " + valueOrDefault(trip.getPlaca(), "Sin placa"));
        driverText.setText(valueOrDefault(trip.getConductorNombre(), "Conductor no asignado"));
        fareText.setText("Ver Detalle");
        pointsText.setText("+3 pts");
        fareText.setOnClickListener(view -> openTripDetail(trip));
        card.setOnClickListener(view -> openTripDetail(trip));

        return card;
    }

    private void openTripDetail(TripItem trip) {
        Intent intent = new Intent(requireContext(), TripDetailActivity.class);
        intent.putExtra(TripDetailActivity.EXTRA_DATE, trip.getFecha());
        intent.putExtra(TripDetailActivity.EXTRA_PLATE, valueOrDefault(trip.getPlaca(), "Sin placa"));
        intent.putExtra(TripDetailActivity.EXTRA_VEHICLE_ID, valueOrDefault(trip.getVehiculoId(), ""));
        intent.putExtra(TripDetailActivity.EXTRA_VEHICLE_NAME, valueOrDefault(trip.getVehiculoNombre(), "Vehiculo del comite"));
        intent.putExtra(TripDetailActivity.EXTRA_UNIT_NUMBER, valueOrDefault(trip.getNumeroUnidad(), "Unidad"));
        intent.putExtra(TripDetailActivity.EXTRA_DRIVER_ID, valueOrDefault(trip.getConductorId(), ""));
        intent.putExtra(TripDetailActivity.EXTRA_DRIVER_NAME, valueOrDefault(trip.getConductorNombre(), "Conductor no asignado"));
        intent.putExtra(TripDetailActivity.EXTRA_DRIVER_PHONE, valueOrDefault(trip.getConductorCelular(), ""));
        startActivity(intent);
    }

    private void showEmptyState(String message) {
        historyContainer.removeAllViews();
        emptyStateText.setText(message);
        emptyStateText.setVisibility(View.VISIBLE);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (historyReference != null && historyListener != null) {
            historyReference.removeEventListener(historyListener);
        }
    }
}






