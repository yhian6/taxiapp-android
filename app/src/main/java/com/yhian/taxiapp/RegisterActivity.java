package com.yhian.taxiapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.yhian.taxiapp.utils.FirebaseRefs;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText fullNameInput;
    private TextInputEditText phoneInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton registerButton;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference passengersReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#073B91"));
        getWindow().setNavigationBarColor(getColor(R.color.background_light));

        setContentView(R.layout.activity_register);

        fullNameInput = findViewById(R.id.fullNameInput);
        phoneInput = findViewById(R.id.phoneInput);
        emailInput = findViewById(R.id.registerEmailInput);
        passwordInput = findViewById(R.id.registerPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.createAccountButton);
        TextView loginText = findViewById(R.id.loginText);
        findViewById(R.id.registerBackButton).setOnClickListener(view -> finish());

        firebaseAuth = FirebaseAuth.getInstance();
        passengersReference = FirebaseRefs.root().child("pasajeros");

        registerButton.setOnClickListener(view -> validateAndRegister());
        loginText.setOnClickListener(view -> finish());
    }

    private void validateAndRegister() {
        String fullName = String.valueOf(fullNameInput.getText()).trim();
        String phone = normalizePhone(String.valueOf(phoneInput.getText()).trim());
        String email = String.valueOf(emailInput.getText()).trim();
        String password = String.valueOf(passwordInput.getText()).trim();
        String confirmPassword = String.valueOf(confirmPasswordInput.getText()).trim();

        if (fullName.isEmpty()) {
            fullNameInput.setError("Ingresa tu nombre");
            return;
        }

        if (phone.isEmpty()) {
            phoneInput.setError("Ingresa tu celular");
            return;
        }

        if (phone.length() < 9) {
            phoneInput.setError("Ingresa un celular valido");
            return;
        }

        if (email.isEmpty()) {
            emailInput.setError("Ingresa tu correo");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Minimo 6 caracteres");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Las contrasenas no coinciden");
            return;
        }

        setLoading(true);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        savePassengerProfile(
                                firebaseAuth.getCurrentUser().getUid(),
                                fullName,
                                phone,
                                email
                        );
                    } else {
                        setLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                        Toast.makeText(
                                RegisterActivity.this,
                                "Error: " + error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void savePassengerProfile(String uid, String fullName, String phone, String email) {
        Map<String, Object> passenger = new HashMap<>();
        passenger.put("uid", uid);
        passenger.put("nombre", fullName);
        passenger.put("celular", phone);
        passenger.put("correo", email);
        passenger.put("rol", "pasajero");
        passenger.put("puntos", 0);
        passenger.put("viajes", 0);
        passenger.put("estado", "activo");
        passenger.put("fechaRegistro", ServerValue.TIMESTAMP);

        passengersReference.child(uid).setValue(passenger)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Cuenta creada correctamente", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(error -> {
                    setLoading(false);
                    Toast.makeText(
                            this,
                            "Error guardando perfil: " + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }
    private void setLoading(boolean isLoading) {
        registerButton.setEnabled(!isLoading);
        registerButton.setText(isLoading ? "Creando cuenta..." : "Crear cuenta");
    }
}



