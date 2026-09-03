package com.yhian.taxiapp.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.yhian.taxiapp.R;
import com.yhian.taxiapp.models.RewardItem;
import com.yhian.taxiapp.utils.FirebaseRefs;
import com.yhian.taxiapp.utils.ImageLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PointsFragment extends Fragment {

    private static final int REWARD_PREVIEW_LIMIT = 2;
    private static final int REDEMPTION_PREVIEW_LIMIT = 2;
    private static final String ARG_FOCUS_REWARDS = "focus_rewards";

    private ScrollView pointsScrollView;
    private TextView rewardsSectionTitle;
    private TextView totalPointsText;
    private TextView missingPointsText;
    private TextView progressPercentText;
    private TextView progressRequirementText;
    private ProgressBar rewardProgressBar;
    private TextView emptyRewardsText;
    private TextView emptyRedemptionsText;
    private MaterialButton seeMoreRewardsButton;
    private MaterialButton seeMoreRedemptionsButton;
    private LinearLayout rewardsContainer;
    private LinearLayout redemptionsContainer;
    private final List<RewardItem> rewards = new ArrayList<>();
    private final List<RedemptionRequest> redemptionRequests = new ArrayList<>();
    private boolean showingAllRewards = false;
    private boolean showingAllRedemptions = false;
    private long currentTotalPoints = 0;
    private String currentPassengerName = "";
    private String currentPassengerPhone = "";
    private String currentPassengerEmail = "";
    private DatabaseReference passengerReference;
    private DatabaseReference rewardsReference;
    private DatabaseReference redemptionsReference;
    private Query redemptionsQuery;
    private ValueEventListener passengerListener;
    private ValueEventListener rewardsListener;
    private ValueEventListener redemptionsListener;

    public static PointsFragment newInstance(boolean focusRewards) {
        PointsFragment fragment = new PointsFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FOCUS_REWARDS, focusRewards);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_points, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pointsScrollView = view.findViewById(R.id.pointsScrollView);
        rewardsSectionTitle = view.findViewById(R.id.rewardsSectionTitle);
        totalPointsText = view.findViewById(R.id.totalPointsText);
        missingPointsText = view.findViewById(R.id.missingPointsText);
        progressPercentText = view.findViewById(R.id.progressPercentText);
        progressRequirementText = view.findViewById(R.id.progressRequirementText);
        rewardProgressBar = view.findViewById(R.id.rewardProgressBar);
        emptyRewardsText = view.findViewById(R.id.emptyRewardsText);
        emptyRedemptionsText = view.findViewById(R.id.emptyRedemptionsText);
        seeMoreRewardsButton = view.findViewById(R.id.seeMoreRewardsButton);
        seeMoreRedemptionsButton = view.findViewById(R.id.seeMoreRedemptionsButton);
        rewardsContainer = view.findViewById(R.id.rewardsContainer);
        redemptionsContainer = view.findViewById(R.id.redemptionsContainer);
        seeMoreRewardsButton.setOnClickListener(view1 -> {
            showingAllRewards = !showingAllRewards;
            renderRewards();
        });
        seeMoreRedemptionsButton.setOnClickListener(view1 -> {
            showingAllRedemptions = !showingAllRedemptions;
            renderRedemptions();
        });

        loadPointsData();
        loadRewards();
        focusRewardsIfRequested();
    }

    private void focusRewardsIfRequested() {
        boolean focusRewards = getArguments() != null && getArguments().getBoolean(ARG_FOCUS_REWARDS, false);
        if (focusRewards) {
            pointsScrollView.postDelayed(this::scrollToRewards, 250L);
        }
    }

    private void scrollToRewards() {
        if (pointsScrollView == null || rewardsSectionTitle == null) {
            return;
        }

        pointsScrollView.smoothScrollTo(0, rewardsSectionTitle.getTop());
    }

    private void loadPointsData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            showUnavailableState();
            return;
        }

        passengerReference = FirebaseRefs.root().child("pasajeros").child(currentUser.getUid());
        redemptionsReference = FirebaseRefs.root().child("solicitudes_canje");

        passengerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long points = snapshot.child("puntos").getValue(Long.class);
                currentPassengerName = valueOrDefault(snapshot.child("nombre").getValue(String.class), "");
                currentPassengerPhone = valueOrDefault(snapshot.child("celular").getValue(String.class), "");
                currentPassengerEmail = valueOrDefault(snapshot.child("correo").getValue(String.class),
                        currentUser.getEmail() != null ? currentUser.getEmail() : "");
                currentTotalPoints = points != null ? points : 0;
                totalPointsText.setText(String.valueOf(currentTotalPoints));
                updateProgressCard();
                renderRewards();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showUnavailableState();
            }
        };

        passengerReference.addValueEventListener(passengerListener);
        loadRedemptions(currentUser.getUid());
    }

    private void loadRedemptions(String passengerUid) {
        redemptionsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                redemptionRequests.clear();

                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    RedemptionRequest request = RedemptionRequest.fromSnapshot(requestSnapshot);

                    if (passengerUid.equals(request.passengerUid)) {
                        redemptionRequests.add(request);
                    }
                }

                Collections.sort(redemptionRequests, (first, second) ->
                        Long.compare(second.requestDate, first.requestDate));
                renderRewards();
                renderRedemptions();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                emptyRedemptionsText.setText("No se pudieron cargar tus canjes.");
                emptyRedemptionsText.setVisibility(View.VISIBLE);
            }
        };

        redemptionsQuery = redemptionsReference.orderByChild("pasajeroUid").equalTo(passengerUid);
        redemptionsQuery.addValueEventListener(redemptionsListener);
    }

    private void loadRewards() {
        rewardsReference = FirebaseRefs.root().child("recompensas");
        rewardsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rewards.clear();

                for (DataSnapshot rewardSnapshot : snapshot.getChildren()) {
                    RewardItem reward = rewardSnapshot.getValue(RewardItem.class);

                    if (reward != null && reward.isActivo()) {
                        if (reward.getId() == null) {
                            reward.setId(rewardSnapshot.getKey());
                        }
                        rewards.add(reward);
                    }
                }

                if (rewards.isEmpty()) {
                    addFallbackRewards();
                }

                Collections.sort(rewards, (first, second) ->
                        Long.compare(first.getPuntosRequeridos(), second.getPuntosRequeridos()));
                updateProgressCard();
                renderRewards();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                addFallbackRewards();
                updateProgressCard();
                renderRewards();
            }
        };

        rewardsReference.addValueEventListener(rewardsListener);
    }

    private void addFallbackRewards() {
        rewards.clear();
        rewards.add(new RewardItem(
                "viaje_100",
                "Viaje gratis",
                "Canje para pasajeros frecuentes al llegar a la primera meta.",
                "Canje",
                100
        ));
        rewards.add(new RewardItem(
                "pack_500",
                "Six pack de vasos",
                "Premio especial para clientes constantes del comite.",
                "Beneficio",
                500
        ));
        rewards.add(new RewardItem(
                "canasta_1000",
                "Canasta de viveres",
                "Recompensa premium para pasajeros con mayor fidelidad.",
                "Premio",
                1000
        ));
    }

    private void updateProgressCard() {
        if (rewardProgressBar == null || missingPointsText == null || progressPercentText == null
                || progressRequirementText == null) {
            return;
        }

        if (rewards.isEmpty()) {
            rewardProgressBar.setProgress(0);
            missingPointsText.setText("Sin meta activa");
            progressPercentText.setText("0%");
            progressRequirementText.setText("El gerente aun no activo recompensas.");
            return;
        }

        RewardItem nextReward = rewards.get(rewards.size() - 1);
        for (RewardItem reward : rewards) {
            if (currentTotalPoints < reward.getPuntosRequeridos()) {
                nextReward = reward;
                break;
            }
        }

        long required = Math.max(1, nextReward.getPuntosRequeridos());
        long missing = Math.max(0, required - currentTotalPoints);
        int percent = (int) Math.min(100, Math.round((currentTotalPoints * 100.0) / required));

        rewardProgressBar.setProgress(percent);
        progressPercentText.setText(percent + "%");
        progressRequirementText.setText(required + " puntos requeridos para "
                + valueOrDefault(nextReward.getTitulo(), "la siguiente recompensa"));

        if (missing == 0) {
            missingPointsText.setText("Recompensa lista para canjear");
        } else {
            missingPointsText.setText("Faltan " + missing + " pts");
        }
    }

    private void renderRewards() {
        if (rewardsContainer == null) {
            return;
        }

        rewardsContainer.removeAllViews();

        if (rewards.isEmpty()) {
            emptyRewardsText.setVisibility(View.VISIBLE);
            seeMoreRewardsButton.setVisibility(View.GONE);
            return;
        }

        emptyRewardsText.setVisibility(View.GONE);
        boolean hasMoreRewards = rewards.size() > REWARD_PREVIEW_LIMIT;
        int visibleCount = showingAllRewards || !hasMoreRewards
                ? rewards.size()
                : REWARD_PREVIEW_LIMIT;

        for (int index = 0; index < visibleCount; index++) {
            rewardsContainer.addView(createRewardCard(rewards.get(index)));
        }

        seeMoreRewardsButton.setVisibility(hasMoreRewards ? View.VISIBLE : View.GONE);
        seeMoreRewardsButton.setText(showingAllRewards ? "Ver menos" : "Ver mas");
    }

    private View createRewardCard(RewardItem reward) {
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_reward, rewardsContainer, false);

        TextView titleText = card.findViewById(R.id.rewardTitleText);
        TextView descriptionText = card.findViewById(R.id.rewardDescriptionText);
        TextView typeText = card.findViewById(R.id.rewardTypeText);
        TextView requiredText = card.findViewById(R.id.rewardRequiredText);
        TextView statusText = card.findViewById(R.id.rewardStatusText);
        ImageView rewardImage = card.findViewById(R.id.rewardImage);
        MaterialButton requestButton = card.findViewById(R.id.requestRewardButton);

        boolean available = currentTotalPoints >= reward.getPuntosRequeridos();
        boolean hasPendingRequest = hasPendingRequestForReward(reward.getId());
        long missing = Math.max(0, reward.getPuntosRequeridos() - currentTotalPoints);

        titleText.setText(valueOrDefault(reward.getTitulo(), "Recompensa"));
        descriptionText.setText(valueOrDefault(reward.getDescripcion(), "Beneficio disponible para pasajeros frecuentes."));
        typeText.setText(valueOrDefault(reward.getTipo(), "Canje"));
        requiredText.setText(reward.getPuntosRequeridos() + " pts");
        if (hasPendingRequest) {
            statusText.setText("Solicitud pendiente de revision");
        } else {
            statusText.setText(available ? "Disponible para solicitar" : "Te faltan " + missing + " pts");
        }
        ImageLoader.load(reward.getImagenUrl(), rewardImage, R.drawable.bg_points_hero);

        requestButton.setEnabled(available && !hasPendingRequest);
        requestButton.setText(hasPendingRequest ? "Pendiente" : available ? "Canjear" : "Bloqueado");
        card.setAlpha(available ? 1.0f : 0.72f);

        requestButton.setOnClickListener(view -> requestReward(reward));
        return card;
    }

    private void requestReward(RewardItem reward) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesion para canjear", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentTotalPoints < reward.getPuntosRequeridos()) {
            Toast.makeText(requireContext(), "Aun no tienes puntos suficientes", Toast.LENGTH_SHORT).show();
            return;
        }

        if (hasPendingRequestForReward(reward.getId())) {
            Toast.makeText(requireContext(), "Ya tienes este canje pendiente", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference requestsReference = FirebaseRefs.root().child("solicitudes_canje");
        String requestId = requestsReference.push().getKey();

        if (requestId == null) {
            Toast.makeText(requireContext(), "No se pudo crear la solicitud", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("id", requestId);
        request.put("pasajeroUid", currentUser.getUid());
        request.put("pasajeroNombre", valueOrDefault(currentPassengerName, "Pasajero"));
        request.put("pasajeroCelular", currentPassengerPhone);
        request.put("pasajeroCorreo", currentPassengerEmail);
        request.put("recompensaId", reward.getId());
        request.put("recompensaTitulo", reward.getTitulo());
        request.put("puntosRequeridos", reward.getPuntosRequeridos());
        request.put("puntosActuales", currentTotalPoints);
        request.put("estado", "pendiente");
        request.put("fechaSolicitud", ServerValue.TIMESTAMP);

        requestsReference.child(requestId).setValue(request)
                .addOnSuccessListener(unused ->
                        Toast.makeText(requireContext(), "Solicitud enviada al gerente", Toast.LENGTH_LONG).show())
                .addOnFailureListener(error ->
                        Toast.makeText(requireContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void renderRedemptions() {
        if (redemptionsContainer == null) {
            return;
        }

        redemptionsContainer.removeAllViews();

        if (redemptionRequests.isEmpty()) {
            emptyRedemptionsText.setVisibility(View.VISIBLE);
            seeMoreRedemptionsButton.setVisibility(View.GONE);
            return;
        }

        emptyRedemptionsText.setVisibility(View.GONE);
        boolean hasMoreRedemptions = redemptionRequests.size() > REDEMPTION_PREVIEW_LIMIT;
        int visibleCount = showingAllRedemptions || !hasMoreRedemptions
                ? redemptionRequests.size()
                : REDEMPTION_PREVIEW_LIMIT;

        for (int index = 0; index < visibleCount; index++) {
            redemptionsContainer.addView(createRedemptionCard(redemptionRequests.get(index)));
        }

        seeMoreRedemptionsButton.setVisibility(hasMoreRedemptions ? View.VISIBLE : View.GONE);
        seeMoreRedemptionsButton.setText(showingAllRedemptions ? "Ver menos" : "Ver mas");
    }

    private View createRedemptionCard(RedemptionRequest request) {
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_redemption_request, redemptionsContainer, false);

        TextView titleText = card.findViewById(R.id.redemptionTitleText);
        TextView dateText = card.findViewById(R.id.redemptionDateText);
        TextView pointsText = card.findViewById(R.id.redemptionPointsText);
        TextView statusText = card.findViewById(R.id.redemptionStatusText);
        TextView messageText = card.findViewById(R.id.redemptionMessageText);

        titleText.setText(valueOrDefault(request.rewardTitle, "Recompensa solicitada"));
        dateText.setText(formatDate(request.requestDate));
        pointsText.setText(request.requiredPoints + " pts");
        statusText.setText(formatRedemptionStatus(request.status));
        statusText.setTextColor(getRedemptionStatusColor(request.status));
        messageText.setText(getRedemptionMessage(request.status));

        return card;
    }

    private boolean hasPendingRequestForReward(String rewardId) {
        if (rewardId == null || rewardId.trim().isEmpty()) {
            return false;
        }

        for (RedemptionRequest request : redemptionRequests) {
            if (rewardId.equals(request.rewardId) && "pendiente".equalsIgnoreCase(request.status)) {
                return true;
            }
        }

        return false;
    }

    private void showUnavailableState() {
        totalPointsText.setText("0");
        rewardProgressBar.setProgress(0);
        missingPointsText.setText("Inicia sesion");
        progressPercentText.setText("0%");
        progressRequirementText.setText("Inicia sesion para ver tus recompensas.");
        emptyRewardsText.setText("Inicia sesion para ver tus recompensas.");
        emptyRewardsText.setVisibility(View.VISIBLE);
    }

    private String valueOrDefault(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return "Fecha pendiente";
        }

        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, h:mm a", new Locale("es", "PE"));
        return formatter.format(new Date(timestamp));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (passengerReference != null && passengerListener != null) {
            passengerReference.removeEventListener(passengerListener);
        }

        if (rewardsReference != null && rewardsListener != null) {
            rewardsReference.removeEventListener(rewardsListener);
        }

        if (redemptionsQuery != null && redemptionsListener != null) {
            redemptionsQuery.removeEventListener(redemptionsListener);
        }
    }

    private String formatRedemptionStatus(String status) {
        if ("aprobado".equalsIgnoreCase(status)) {
            return "Aprobado";
        }

        if ("rechazado".equalsIgnoreCase(status)) {
            return "Rechazado";
        }

        return "Pendiente";
    }

    private int getRedemptionStatusColor(String status) {
        if ("aprobado".equalsIgnoreCase(status)) {
            return Color.parseColor("#111B6D");
        }

        if ("rechazado".equalsIgnoreCase(status)) {
            return Color.parseColor("#D64545");
        }

        return Color.parseColor("#A26700");
    }

    private String getRedemptionMessage(String status) {
        if ("aprobado".equalsIgnoreCase(status)) {
            return "El gerente aprobo tu canje. Acercate al comite para coordinar la entrega.";
        }

        if ("rechazado".equalsIgnoreCase(status)) {
            return "Tu solicitud fue revisada y no fue aprobada. Puedes consultar con el comite.";
        }

        return "Tu solicitud ya fue enviada. El gerente debe revisarla desde el panel.";
    }

    private static class RedemptionRequest {
        String id;
        String passengerUid;
        String rewardId;
        String rewardTitle;
        String status;
        long requiredPoints;
        long requestDate;

        static RedemptionRequest fromSnapshot(DataSnapshot snapshot) {
            RedemptionRequest request = new RedemptionRequest();
            request.id = value(snapshot, "id", snapshot.getKey());
            request.passengerUid = value(snapshot, "pasajeroUid", "");
            request.rewardId = value(snapshot, "recompensaId", "");
            request.rewardTitle = value(snapshot, "recompensaTitulo", "Recompensa solicitada");
            request.status = value(snapshot, "estado", "pendiente");
            request.requiredPoints = longValue(snapshot, "puntosRequeridos");
            request.requestDate = longValue(snapshot, "fechaSolicitud");
            return request;
        }

        private static String value(DataSnapshot snapshot, String key, String fallback) {
            String value = snapshot.child(key).getValue(String.class);
            return value != null && !value.trim().isEmpty() ? value : fallback;
        }

        private static long longValue(DataSnapshot snapshot, String key) {
            Object value = snapshot.child(key).getValue();

            if (value instanceof Number) {
                return ((Number) value).longValue();
            }

            try {
                return value != null ? Long.parseLong(String.valueOf(value)) : 0;
            } catch (NumberFormatException error) {
                return 0;
            }
        }
    }
}



