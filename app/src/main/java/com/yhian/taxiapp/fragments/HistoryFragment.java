package com.yhian.taxiapp.fragments;

import android.content.Intent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private DatabaseReference historyReference;
    private ValueEventListener historyListener;

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
                List<TripItem> trips = new ArrayList<>();

                for (DataSnapshot tripSnapshot : snapshot.getChildren()) {
                    TripItem trip = tripSnapshot.getValue(TripItem.class);

                    if (trip != null) {
                        if (trip.getId() == null) {
                            trip.setId(tripSnapshot.getKey());
                        }
                        trips.add(trip);
                    }
                }

                Collections.sort(trips, (first, second) -> Long.compare(second.getFecha(), first.getFecha()));
                renderTrips(trips);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showEmptyState("No se pudo cargar el historial.");
            }
        };

        historyReference.addValueEventListener(historyListener);
    }

    private void renderTrips(List<TripItem> trips) {
        historyContainer.removeAllViews();

        if (trips.isEmpty()) {
            totalTripsText.setText("0");
            totalPointsText.setText("0 pts");
            showEmptyState("Aun no tienes viajes registrados.");
            return;
        }

        emptyStateText.setVisibility(View.GONE);

        int monthlyTrips = 0;
        for (TripItem trip : trips) {
            if (isCurrentMonth(trip.getFecha())) {
                monthlyTrips++;
            }
            historyContainer.addView(createTripCard(trip));
        }

        totalTripsText.setText(String.valueOf(monthlyTrips));
        totalPointsText.setText((trips.size() * 3) + " pts");
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

    private boolean isCurrentMonth(long timestamp) {
        if (timestamp <= 0) {
            return false;
        }

        Calendar tripDate = Calendar.getInstance();
        tripDate.setTimeInMillis(timestamp);
        Calendar today = Calendar.getInstance();
        return tripDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && tripDate.get(Calendar.MONTH) == today.get(Calendar.MONTH);
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






