package com.yhian.taxiapp;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.yhian.taxiapp.fragments.HistoryFragment;
import com.yhian.taxiapp.fragments.HomeFragment;
import com.yhian.taxiapp.fragments.PointsFragment;
import com.yhian.taxiapp.fragments.ProfileFragment;
import com.yhian.taxiapp.utils.FirebaseRefs;

import java.util.ArrayDeque;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private boolean focusRewardsOnNextPointsOpen;
    private DatabaseReference notificationsReference;
    private ChildEventListener notificationsListener;
    private final Queue<AppNotification> pendingNotifications = new ArrayDeque<>();
    private boolean notificationDialogShowing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(getColor(R.color.background_light));
        window.setNavigationBarColor(getColor(R.color.background_light));
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        showFragment(new HomeFragment());
        listenPassengerNotifications();

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                showFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_points) {
                if (focusRewardsOnNextPointsOpen) {
                    showFragment(PointsFragment.newInstance(true));
                    focusRewardsOnNextPointsOpen = false;
                } else {
                    showFragment(new PointsFragment());
                }
                return true;
            } else if (itemId == R.id.nav_history) {
                showFragment(new HistoryFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                showFragment(new ProfileFragment());
                return true;
            }

            return false;
        });
    }


    public void openPointsRewards() {
        focusRewardsOnNextPointsOpen = true;
        if (bottomNavigation != null && bottomNavigation.getSelectedItemId() != R.id.nav_points) {
            bottomNavigation.setSelectedItemId(R.id.nav_points);
            return;
        }

        showFragment(PointsFragment.newInstance(true));
        focusRewardsOnNextPointsOpen = false;
    }

    public void openHistory() {
        if (bottomNavigation != null && bottomNavigation.getSelectedItemId() != R.id.nav_history) {
            bottomNavigation.setSelectedItemId(R.id.nav_history);
            return;
        }

        showFragment(new HistoryFragment());
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void listenPassengerNotifications() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        notificationsReference = FirebaseRefs.root().child("notificaciones").child(currentUser.getUid());
        notificationsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Boolean read = snapshot.child("leida").getValue(Boolean.class);
                if (Boolean.TRUE.equals(read)) {
                    return;
                }

                String title = getSnapshotString(snapshot, "titulo", "Nueva notificacion");
                String message = getSnapshotString(snapshot, "mensaje", "Tienes una novedad en tu cuenta.");
                String type = getSnapshotString(snapshot, "tipo", "general");
                pendingNotifications.add(new AppNotification(snapshot.getKey(), title, message, type));
                showNextNotification();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        notificationsReference.addChildEventListener(notificationsListener);
    }

    private void showNextNotification() {
        if (notificationDialogShowing || pendingNotifications.isEmpty() || isFinishing()) {
            return;
        }

        AppNotification notification = pendingNotifications.poll();
        if (notification == null) {
            return;
        }

        notificationDialogShowing = true;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_auth_message, null, false);
        ImageView iconView = dialogView.findViewById(R.id.messageIcon);
        TextView titleText = dialogView.findViewById(R.id.messageTitleText);
        TextView bodyText = dialogView.findViewById(R.id.messageBodyText);
        MaterialButton actionButton = dialogView.findViewById(R.id.messageActionButton);

        iconView.setImageResource("canje".equals(notification.type) ? R.drawable.ic_points : R.drawable.ic_taxiapp_logo);
        titleText.setText(notification.title);
        bodyText.setText(notification.message);
        actionButton.setText("Entendido");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        actionButton.setOnClickListener(view -> {
            markNotificationAsRead(notification.id);
            dialog.dismiss();
        });
        dialog.setOnDismissListener(dialogInterface -> {
            notificationDialogShowing = false;
            showNextNotification();
        });
        dialog.setOnShowListener(dialogInterface -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    private void markNotificationAsRead(String notificationId) {
        if (notificationsReference == null || notificationId == null || notificationId.trim().isEmpty()) {
            return;
        }

        notificationsReference.child(notificationId).child("leida").setValue(true);
    }

    private String getSnapshotString(DataSnapshot snapshot, String key, String fallback) {
        Object value = snapshot.child(key).getValue();
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return fallback;
        }
        return String.valueOf(value).trim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationsReference != null && notificationsListener != null) {
            notificationsReference.removeEventListener(notificationsListener);
        }
    }

    private static class AppNotification {
        final String id;
        final String title;
        final String message;
        final String type;

        AppNotification(String id, String title, String message, String type) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.type = type;
        }
    }
}




