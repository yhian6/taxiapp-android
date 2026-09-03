package com.yhian.taxiapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yhian.taxiapp.QrScannerActivity;
import com.yhian.taxiapp.MainActivity;
import com.yhian.taxiapp.R;
import com.yhian.taxiapp.SliderDetailActivity;
import com.yhian.taxiapp.models.SliderItem;
import com.yhian.taxiapp.utils.FirebaseRefs;
import com.yhian.taxiapp.utils.ImageLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final long SLIDER_DELAY_MS = 3500L;
    private long firstRewardGoalPoints = 100L;
    private long currentTotalPoints = 0;

    private TextView greetingText;
    private TextView pointsText;
    private TextView tripsText;
    private TextView statusText;
    private LinearProgressIndicator pointsProgress;
    private HorizontalScrollView homeSliderScroll;
    private LinearLayout sliderItemsContainer;
    private LinearLayout sliderDotsContainer;
    private final List<SliderItem> sliderItems = new ArrayList<>();
    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private int currentSliderPosition = 0;
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            moveToNextSliderItem();
            scheduleSlider();
        }
    };
    private DatabaseReference passengerReference;
    private DatabaseReference sliderReference;
    private DatabaseReference rewardsReference;
    private ValueEventListener passengerListener;
    private ValueEventListener sliderListener;
    private ValueEventListener rewardsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        greetingText = view.findViewById(R.id.greetingText);
        pointsText = view.findViewById(R.id.pointsText);
        tripsText = view.findViewById(R.id.tripsText);
        statusText = view.findViewById(R.id.statusText);
        pointsProgress = view.findViewById(R.id.pointsProgress);
        homeSliderScroll = view.findViewById(R.id.homeSliderScroll);
        sliderItemsContainer = view.findViewById(R.id.sliderItemsContainer);
        sliderDotsContainer = view.findViewById(R.id.sliderDotsContainer);
        MaterialButton scanButton = view.findViewById(R.id.scanQrButton);
        TextView viewRewardsText = view.findViewById(R.id.viewRewardsText);

        scanButton.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), QrScannerActivity.class)));
        viewRewardsText.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openPointsRewards();
            }
        });

        loadPassengerData();
        loadSliderData();
        loadRewardsGoalData();
    }

    private void loadPassengerData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            greetingText.setText("Hola, pasajero");
            currentTotalPoints = 0;
            pointsText.setText("0");
            tripsText.setText("0 viajes");
            updatePointsProgress();
            statusText.setText("Inicia sesion para ver tus puntos.");
            return;
        }

        passengerReference = FirebaseRefs.root()
                .child("pasajeros")
                .child(currentUser.getUid());

        passengerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("nombre").getValue(String.class);
                Long points = snapshot.child("puntos").getValue(Long.class);
                Long trips = snapshot.child("viajes").getValue(Long.class);

                greetingText.setText("Hola, " + getFirstName(name));
                currentTotalPoints = points != null ? points : 0;
                pointsText.setText(String.valueOf(currentTotalPoints));
                tripsText.setText((trips != null ? trips : 0) + " viajes");
                updatePointsProgress();
                statusText.setText(getProgressMessage());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                statusText.setText("No se pudieron cargar tus puntos.");
            }
        };

        passengerReference.addValueEventListener(passengerListener);
    }

    private void updatePointsProgress() {
        int progress = (int) Math.min(100, Math.round((currentTotalPoints * 100f) / firstRewardGoalPoints));
        pointsProgress.setProgressCompat(progress, true);
    }

    private String getProgressMessage() {
        long remaining = firstRewardGoalPoints - currentTotalPoints;

        if (remaining <= 0) {
            return "Ya puedes solicitar tu primer canje.";
        }

        return "Te faltan " + remaining + " pts para tu primer canje.";
    }

    private void loadRewardsGoalData() {
        rewardsReference = FirebaseRefs.root().child("recompensas");
        rewardsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long minPoints = Long.MAX_VALUE;
                boolean found = false;

                for (DataSnapshot rewardSnapshot : snapshot.getChildren()) {
                    Long required = rewardSnapshot.child("puntosRequeridos").getValue(Long.class);
                    Boolean active = rewardSnapshot.child("activo").getValue(Boolean.class);

                    if (required != null && (active == null || active)) {
                        if (required < minPoints) {
                            minPoints = required;
                            found = true;
                        }
                    }
                }

                if (found) {
                    firstRewardGoalPoints = minPoints;
                    updatePointsProgress();
                    statusText.setText(getProgressMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Si falla, mantenemos el valor por defecto (100)
            }
        };
        rewardsReference.addValueEventListener(rewardsListener);
    }

    private void loadSliderData() {
        sliderReference = FirebaseRefs.root().child("slider_home");
        sliderListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sliderItems.clear();

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    SliderItem item = itemSnapshot.getValue(SliderItem.class);

                    if (item != null && item.isActivo()) {
                        if (item.getId() == null) {
                            item.setId(itemSnapshot.getKey());
                        }
                        sliderItems.add(item);
                    }

                    if (sliderItems.size() == 4) {
                        break;
                    }
                }

                if (sliderItems.isEmpty()) {
                    addFallbackSliderItems();
                }

                Collections.sort(sliderItems, (first, second) ->
                        Integer.compare(first.getOrden(), second.getOrden()));
                currentSliderPosition = 0;
                renderSliderItems();
                scrollToSliderPosition(currentSliderPosition);
                scheduleSlider();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "No se cargaron las novedades", Toast.LENGTH_SHORT).show();
            }
        };

        sliderReference.addValueEventListener(sliderListener);
    }

    private void renderSliderItems() {
        sliderItemsContainer.removeAllViews();
        homeSliderScroll.post(() -> homeSliderScroll.smoothScrollTo(0, 0));

        for (int i = 0; i < sliderItems.size(); i++) {
            SliderItem item = sliderItems.get(i);
            View bannerView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_slider_banner, sliderItemsContainer, false);

            int width = getResources().getDisplayMetrics().widthPixels - dpToPx(44);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, dpToPx(190));
            params.setMargins(i == 0 ? dpToPx(22) : 0, 0, dpToPx(14), 0);
            bannerView.setLayoutParams(params);

            ImageView bannerImage = bannerView.findViewById(R.id.bannerImage);
            TextView categoryText = bannerView.findViewById(R.id.bannerCategoryText);
            TextView titleText = bannerView.findViewById(R.id.bannerTitleText);
            TextView descriptionText = bannerView.findViewById(R.id.bannerDescriptionText);

            categoryText.setText(item.getCategoria());
            titleText.setText(item.getTitulo());
            descriptionText.setText(item.getDescripcion());
            ImageLoader.load(item.getImagenUrl(), bannerImage, R.drawable.bg_points_hero);

            int selectedPosition = i;
            bannerView.setOnClickListener(view -> {
                currentSliderPosition = selectedPosition;
                updateDots(currentSliderPosition);
                openSliderDetail(item);
            });

            sliderItemsContainer.addView(bannerView);
        }
    }

    private void moveToNextSliderItem() {
        if (sliderItems.size() <= 1) {
            return;
        }

        currentSliderPosition = (currentSliderPosition + 1) % sliderItems.size();
        scrollToSliderPosition(currentSliderPosition);
    }

    private void scrollToSliderPosition(int position) {
        if (position < 0 || position >= sliderItemsContainer.getChildCount()) {
            return;
        }

        View targetView = sliderItemsContainer.getChildAt(position);
        homeSliderScroll.smoothScrollTo(targetView.getLeft() - dpToPx(20), 0);
        updateDots(position);
    }

    private void scheduleSlider() {
        sliderHandler.removeCallbacks(sliderRunnable);

        if (sliderItems.size() > 1 && isAdded()) {
            sliderHandler.postDelayed(sliderRunnable, SLIDER_DELAY_MS);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        scheduleSlider();
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    private void addFallbackSliderItems() {
        sliderItems.add(new SliderItem(
                "promo_1",
                "Puntos dobles este lunes",
                "Viaja con Cucungara Express y acumula mas rapido tus beneficios.",
                "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=80",
                "Promocion"
        ));
        sliderItems.get(sliderItems.size() - 1).setDetalle(
                "Este lunes todos los viajes registrados con QR acumulan puntos promocionales. Consulta con el comite las condiciones vigentes antes de viajar."
        );
        sliderItems.get(sliderItems.size() - 1).setOrden(1);
        sliderItems.add(new SliderItem(
                "promo_2",
                "Sorteo para pasajeros frecuentes",
                "Cada viaje registrado suma oportunidades para participar en premios.",
                "https://images.unsplash.com/photo-1556122071-e404eaedb77f?auto=format&fit=crop&w=1200&q=80",
                "Sorteo"
        ));
        sliderItems.get(sliderItems.size() - 1).setDetalle(
                "Participan los pasajeros que acumulen viajes durante el mes. Mientras mas viajes registres con QR, mas oportunidades tienes de aparecer en los sorteos del comite."
        );
        sliderItems.get(sliderItems.size() - 1).setOrden(2);
        sliderItems.add(new SliderItem(
                "noticia_1",
                "Cura Mori - Catacaos",
                "Seguimos mejorando la experiencia de nuestros pasajeros.",
                "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80",
                "Noticia"
        ));
        sliderItems.get(sliderItems.size() - 1).setDetalle(
                "Cucungara Express cubre la ruta Cura Mori - Catacaos con unidades registradas por el comite. Pronto se publicaran noticias, ubicacion del paradero y mejoras del servicio."
        );
        sliderItems.get(sliderItems.size() - 1).setOrden(3);
        sliderItems.add(new SliderItem(
                "ganador_1",
                "Ganadores del mes",
                "Muy pronto publicaremos a los pasajeros mas constantes de TaxiApp.",
                "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1200&q=80",
                "Ganadores"
        ));
        sliderItems.get(sliderItems.size() - 1).setDetalle(
                "Aun no hay ganadores publicados. Cuando el gerente apruebe canjes, aqui se podra mostrar a los pasajeros frecuentes destacados del mes."
        );
        sliderItems.get(sliderItems.size() - 1).setOrden(4);
    }

    private void updateDots(int selectedPosition) {
        sliderDotsContainer.removeAllViews();

        for (int i = 0; i < sliderItems.size(); i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    i == selectedPosition ? dpToPx(18) : dpToPx(8),
                    dpToPx(8)
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == selectedPosition ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
            sliderDotsContainer.addView(dot);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void openSliderDetail(SliderItem item) {
        Intent intent = new Intent(requireContext(), SliderDetailActivity.class);
        intent.putExtra(SliderDetailActivity.EXTRA_TITLE, item.getTitulo());
        intent.putExtra(SliderDetailActivity.EXTRA_DESCRIPTION, item.getDescripcion());
        intent.putExtra(SliderDetailActivity.EXTRA_DETAIL, item.getDetalle());
        intent.putExtra(SliderDetailActivity.EXTRA_IMAGE_URL, item.getImagenUrl());
        intent.putExtra(SliderDetailActivity.EXTRA_CATEGORY, item.getCategoria());
        startActivity(intent);
    }

    private String getFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "pasajero";
        }

        return fullName.trim().split("\\s+")[0];
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (passengerReference != null && passengerListener != null) {
            passengerReference.removeEventListener(passengerListener);
        }

        if (sliderReference != null && sliderListener != null) {
            sliderReference.removeEventListener(sliderListener);
        }

        if (rewardsReference != null && rewardsListener != null) {
            rewardsReference.removeEventListener(rewardsListener);
        }

        sliderHandler.removeCallbacks(sliderRunnable);
    }
}



