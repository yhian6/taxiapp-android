package com.yhian.taxiapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.facebook.login.LoginManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.yhian.taxiapp.LoginActivity;
import com.yhian.taxiapp.R;
import com.yhian.taxiapp.utils.FirebaseRefs;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "taxiapp_settings";
    private static final String KEY_DARK_MODE = "dark_mode";

    private TextView initialsText;
    private TextView nameText;
    private TextView fullNameText;
    private TextView pointsBadgeText;
    private TextView phoneText;
    private TextView emailText;
    private SwitchMaterial darkModeSwitch;
    private MaterialCardView changePasswordCard;
    private DatabaseReference passengerReference;
    private ValueEventListener passengerListener;
    private String currentName = "";
    private String currentPhone = "";
    private String currentEmail = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initialsText = view.findViewById(R.id.profileInitialsText);
        nameText = view.findViewById(R.id.profileNameText);
        fullNameText = view.findViewById(R.id.profileFullNameText);
        pointsBadgeText = view.findViewById(R.id.profilePointsBadgeText);
        phoneText = view.findViewById(R.id.profilePhoneText);
        emailText = view.findViewById(R.id.profileEmailText);
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        TextView editButton = view.findViewById(R.id.editProfileButton);
        changePasswordCard = view.findViewById(R.id.changePasswordCard);
        MaterialButton logoutButton = view.findViewById(R.id.logoutButton);

        setupDarkMode();
        loadProfile();

        editButton.setOnClickListener(v -> showEditProfileDialog());
        changePasswordCard.setOnClickListener(v -> sendPasswordReset());
        logoutButton.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void setupDarkMode() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        darkModeSwitch.setChecked(isDarkMode);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });
    }

    private void loadProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            setProfileDefaults();
            return;
        }

        currentEmail = currentUser.getEmail() != null ? currentUser.getEmail() : "Sin correo";
        emailText.setText(currentEmail);
        passengerReference = FirebaseRefs.root().child("pasajeros").child(currentUser.getUid());
        passengerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = getSnapshotString(snapshot, "nombre");
                String phone = getSnapshotString(snapshot, "celular");
                long points = getLongValue(snapshot, "puntos", 0L);

                currentName = name;
                currentPhone = phone;

                String displayName = currentName.isEmpty() ? "Pasajero" : currentName;
                nameText.setText(displayName);
                fullNameText.setText(displayName);
                phoneText.setText(currentPhone.isEmpty() ? "Sin celular" : currentPhone);
                emailText.setText(currentEmail);
                initialsText.setText(buildInitials(displayName));
                pointsBadgeText.setText(points + " puntos");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "No se pudo cargar el perfil", Toast.LENGTH_SHORT).show();
            }
        };

        passengerReference.addValueEventListener(passengerListener);
    }

    private void setProfileDefaults() {
        currentEmail = "Sin correo";
        nameText.setText("Pasajero");
        fullNameText.setText("Pasajero");
        phoneText.setText("Sin celular");
        emailText.setText(currentEmail);
        initialsText.setText("P");
        pointsBadgeText.setText("0 puntos");
        if (changePasswordCard != null) {
            changePasswordCard.setVisibility(View.GONE);
        }
    }

    private void updatePasswordCardVisibility(FirebaseUser user) {
        if (changePasswordCard == null || user == null) {
            return;
        }

        boolean hasPasswordLogin = false;
        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
            if ("password".equals(profile.getProviderId())) {
                hasPasswordLogin = true;
                break;
            }
        }

        changePasswordCard.setVisibility(hasPasswordLogin ? View.VISIBLE : View.GONE);
    }

    private void showEditProfileDialog() {
        Context context = requireContext();
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_edit_profile, null, false);
        TextInputEditText nameInput = content.findViewById(R.id.editNameInput);
        TextInputEditText phoneInput = content.findViewById(R.id.editPhoneInput);
        MaterialButton cancelButton = content.findViewById(R.id.cancelEditProfileButton);
        MaterialButton saveButton = content.findViewById(R.id.saveEditProfileButton);
        nameInput.setText(currentName);
        phoneInput.setText(currentPhone);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(content)
                .create();

        cancelButton.setOnClickListener(view -> dialog.dismiss());
        saveButton.setOnClickListener(view -> {
            String newName = String.valueOf(nameInput.getText()).trim();
            String newPhone = String.valueOf(phoneInput.getText()).trim();
            if (newName.isEmpty()) {
                nameInput.setError("Ingresa tu nombre");
                return;
            }
            if (newPhone.length() < 9) {
                phoneInput.setError("Ingresa un celular valido");
                return;
            }
            saveProfile(newName, newPhone);
            dialog.dismiss();
        });

        showTransparentDialog(dialog);
    }

    private void saveProfile(String name, String phone) {
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa tu nombre", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.length() < 9) {
            Toast.makeText(requireContext(), "Ingresa un celular valido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (passengerReference == null) {
            Toast.makeText(requireContext(), "No se encontro tu perfil", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", name);
        updates.put("celular", phone);

        passengerReference.updateChildren(updates)
                .addOnSuccessListener(unused ->
                        Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(error ->
                        Toast.makeText(requireContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void sendPasswordReset() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String email = currentUser != null ? currentUser.getEmail() : currentEmail;

        if (email == null || email.trim().isEmpty() || "Sin correo".equals(email)) {
            Toast.makeText(requireContext(), "No se encontro un correo para recuperar la contraseña", Toast.LENGTH_LONG).show();
            return;
        }

        showPasswordResetConfirmation(email.trim());
    }

    private void showPasswordResetConfirmation(String email) {
        showProfileConfirmDialog(
                R.drawable.ic_lock,
                "Cambiar contraseña",
                "Te enviaremos un enlace seguro a " + email + ". Si no aparece, revisa spam o promociones.",
                "Enviar enlace",
                () -> requestPasswordReset(email)
        );
    }

    private void requestPasswordReset(String email) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> showPasswordResetDialog(email))
                .addOnFailureListener(error ->
                        Toast.makeText(requireContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showPasswordResetDialog(String email) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_auth_message, null, false);
        TextView titleText = dialogView.findViewById(R.id.messageTitleText);
        TextView bodyText = dialogView.findViewById(R.id.messageBodyText);
        MaterialButton actionButton = dialogView.findViewById(R.id.messageActionButton);

        titleText.setText("Correo enviado");
        bodyText.setText("Te enviamos un enlace seguro a " + email + ". Revisa tu bandeja de entrada o correo no deseado para crear una nueva contraseña.");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        actionButton.setOnClickListener(view -> dialog.dismiss());
        showTransparentDialog(dialog);
    }

    private void showLogoutConfirmation() {
        showProfileConfirmDialog(
                R.drawable.ic_logout,
                "Cerrar sesión",
                "¿Deseas cerrar tu sesión en TransPiura? Tendrás que volver a ingresar para usar tu cuenta.",
                "Cerrar sesión",
                this::logout
        );
    }

    private void showProfileConfirmDialog(int iconRes, String title, String message, String actionText, Runnable action) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_profile_confirm, null, false);
        android.widget.ImageView iconView = dialogView.findViewById(R.id.profileConfirmIcon);
        TextView titleText = dialogView.findViewById(R.id.profileConfirmTitleText);
        TextView bodyText = dialogView.findViewById(R.id.profileConfirmBodyText);
        MaterialButton cancelButton = dialogView.findViewById(R.id.profileConfirmCancelButton);
        MaterialButton actionButton = dialogView.findViewById(R.id.profileConfirmActionButton);

        iconView.setImageResource(iconRes);
        titleText.setText(title);
        bodyText.setText(message);
        actionButton.setText(actionText);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        cancelButton.setOnClickListener(view -> dialog.dismiss());
        actionButton.setOnClickListener(view -> {
            dialog.dismiss();
            action.run();
        });
        showTransparentDialog(dialog);
    }

    private void showTransparentDialog(AlertDialog dialog) {
        dialog.setOnShowListener(dialogInterface -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private String buildInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "P";
        }

        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
        return (first + second).toUpperCase();
    }

    private String getSnapshotString(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        return value == null ? "" : String.valueOf(value).trim();
    }

    private long getLongValue(DataSnapshot snapshot, String key, long fallback) {
        Object value = snapshot.child(key).getValue();
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        try {
            return value != null ? Long.parseLong(String.valueOf(value)) : fallback;
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (passengerReference != null && passengerListener != null) {
            passengerReference.removeEventListener(passengerListener);
        }
    }
}





