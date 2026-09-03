package com.yhian.taxiapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.yhian.taxiapp.utils.FirebaseRefs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 1204;

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private MaterialCardView googleSignInButton;
    private MaterialCardView facebookSignInButton;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#073B91"));
        getWindow().setNavigationBarColor(getColor(R.color.background_light));

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        facebookSignInButton = findViewById(R.id.facebookSignInButton);
        TextView registerText = findViewById(R.id.registerText);
        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);

        setupGoogleSignIn();
        setupFacebookSignIn();

        loginButton.setOnClickListener(view -> validateAndContinue());
        googleSignInButton.setOnClickListener(view -> startGoogleSignIn());
        facebookSignInButton.setOnClickListener(view -> startFacebookSignIn());
        registerText.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        forgotPasswordText.setOnClickListener(view -> showPasswordRecoveryDialog());
    }

    private void showPasswordRecoveryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_password_recovery, null, false);
        TextInputEditText recoveryInput = dialogView.findViewById(R.id.recoveryEmailInput);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelRecoveryButton);
        MaterialButton sendButton = dialogView.findViewById(R.id.sendRecoveryButton);

        String currentEmail = String.valueOf(emailInput.getText()).trim();
        if (isValidEmail(currentEmail)) {
            recoveryInput.setText(currentEmail);
            recoveryInput.setSelection(currentEmail.length());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        cancelButton.setOnClickListener(view -> dialog.dismiss());
        sendButton.setOnClickListener(view -> {
            String email = String.valueOf(recoveryInput.getText()).trim();
            if (!isValidEmail(email)) {
                recoveryInput.setError("Ingresa un correo valido");
                return;
            }

            dialog.dismiss();
            sendPasswordRecoveryEmail(email);
        });

        dialog.setOnShowListener(dialogInterface -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    private void sendPasswordRecoveryEmail(String email) {
        setLoading(true);
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        showAuthMessageDialog(
                                "Correo enviado",
                                "Te enviamos un enlace seguro a " + email + ". Revisa tu bandeja de entrada o correo no deseado para crear una nueva contrasena."
                        );
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                        showAuthMessageDialog(
                                "No se pudo enviar",
                                "Verifica que el correo este bien escrito e intenta nuevamente. Detalle: " + error
                        );
                    }
                });
    }

    private void showAuthMessageDialog(String title, String message) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_auth_message, null, false);
        TextView titleText = dialogView.findViewById(R.id.messageTitleText);
        TextView bodyText = dialogView.findViewById(R.id.messageBodyText);
        MaterialButton actionButton = dialogView.findViewById(R.id.messageActionButton);

        titleText.setText(title);
        bodyText.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        actionButton.setOnClickListener(view -> dialog.dismiss());
        dialog.setOnShowListener(dialogInterface -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    private void setupGoogleSignIn() {
        String webClientId = getDefaultWebClientId();
        if (webClientId.isEmpty()) {
            googleSignInClient = null;
            return;
        }

        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
    }

    private String getDefaultWebClientId() {
        int resourceId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        if (resourceId == 0) {
            return "";
        }
        return getString(resourceId);
    }
    private void setupFacebookSignIn() {
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Inicio con Facebook cancelado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Facebook: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void validateAndContinue() {
        String email = String.valueOf(emailInput.getText()).trim();
        String password = String.valueOf(passwordInput.getText()).trim();

        if (email.isEmpty()) {
            emailInput.setError("Ingresa tu correo");
            return;
        }

        if (!email.contains("@")) {
            emailInput.setError("Ingresa un correo valido");
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Ingresa tu contrasena");
            return;
        }

        setLoading(true);
        signInWithEmail(email, password);
    }

    private void signInWithEmail(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        handleAuthenticatedUser(firebaseAuth.getCurrentUser());
                    } else {
                        setLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                        Toast.makeText(LoginActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startGoogleSignIn() {
        if (googleSignInClient == null) {
            Toast.makeText(this, "Configura Google Sign-In en Firebase y descarga de nuevo google-services.json", Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    private void startFacebookSignIn() {
        setLoading(true);
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (callbackManager != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null || account.getIdToken() == null) {
                setLoading(false);
                Toast.makeText(this, "No se pudo obtener la cuenta de Google", Toast.LENGTH_SHORT).show();
                return;
            }

            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                            handleAuthenticatedUser(firebaseAuth.getCurrentUser());
                        } else {
                            setLoading(false);
                            String error = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                            Toast.makeText(this, "Google: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (ApiException error) {
            setLoading(false);
            Toast.makeText(this, "Google: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        handleAuthenticatedUser(firebaseAuth.getCurrentUser());
                    } else {
                        setLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                        Toast.makeText(this, "Facebook: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleAuthenticatedUser(FirebaseUser user) {
        DatabaseReference passengerReference = FirebaseRefs.root().child("pasajeros").child(user.getUid());
        passengerReference.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        setLoading(false);
                        navigateToMain();
                    } else {
                        saveSocialPassengerProfile(passengerReference, user);
                    }
                })
                .addOnFailureListener(error -> {
                    setLoading(false);
                    Toast.makeText(this, "No se pudo validar tu perfil: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveSocialPassengerProfile(DatabaseReference passengerReference, FirebaseUser user) {
        Map<String, Object> passenger = new HashMap<>();
        passenger.put("uid", user.getUid());
        passenger.put("nombre", buildPassengerName(user));
        passenger.put("celular", "");
        passenger.put("correo", user.getEmail() != null ? user.getEmail() : "");
        passenger.put("rol", "pasajero");
        passenger.put("proveedor", buildProviderName(user));
        passenger.put("puntos", 0);
        passenger.put("viajes", 0);
        passenger.put("estado", "activo");
        passenger.put("fechaRegistro", ServerValue.TIMESTAMP);

        passengerReference.setValue(passenger)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    navigateToMain();
                })
                .addOnFailureListener(error -> {
                    setLoading(false);
                    Toast.makeText(this, "Error guardando perfil: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String buildPassengerName(FirebaseUser user) {
        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName().trim();
        }

        if (user.getEmail() != null && user.getEmail().contains("@")) {
            return user.getEmail().substring(0, user.getEmail().indexOf("@"));
        }

        return "Pasajero";
    }

    private String buildProviderName(FirebaseUser user) {
        if (user.getProviderData() == null || user.getProviderData().isEmpty()) {
            return "firebase";
        }

        for (int i = 0; i < user.getProviderData().size(); i++) {
            String providerId = user.getProviderData().get(i).getProviderId();
            if (!"firebase".equals(providerId)) {
                return providerId;
            }
        }

        return "firebase";
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        if (googleSignInButton != null) {
            googleSignInButton.setEnabled(!isLoading);
        }
        if (facebookSignInButton != null) {
            facebookSignInButton.setEnabled(!isLoading);
        }
        loginButton.setText(isLoading ? "Ingresando..." : getString(R.string.login_button));
    }
}










