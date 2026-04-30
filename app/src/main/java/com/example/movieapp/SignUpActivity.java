package com.example.movieapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText ageInput;
    private FirebaseAuth firebaseAuth;
    private EditText confirmPasswordInput;
    private EditText emailInput;
    private EditText fullNameInput;
    private EditText passwordInput;
    private Button signUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        firebaseAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sign_up_content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        setupActions();
    }

    private void bindViews() {
        fullNameInput = findViewById(R.id.sign_up_full_name_input);
        ageInput = findViewById(R.id.sign_up_age_input);
        emailInput = findViewById(R.id.sign_up_email_input);
        passwordInput = findViewById(R.id.sign_up_password_input);
        confirmPasswordInput = findViewById(R.id.sign_up_confirm_password_input);
        signUpButton = findViewById(R.id.sign_up_button_action);
    }

    private int getValidatedAge(String ageText) {
        if (TextUtils.isEmpty(ageText)) {
            ageInput.setError(getString(R.string.error_age_required));
            ageInput.requestFocus();
            return -1;
        }

        try {
            int age = Integer.parseInt(ageText);
            if (age <= 0) {
                ageInput.setError(getString(R.string.error_age_invalid));
                ageInput.requestFocus();
                return -1;
            }
            return age;
        } catch (NumberFormatException exception) {
            ageInput.setError(getString(R.string.error_age_invalid));
            ageInput.requestFocus();
            return -1;
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void openHome() {
        Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void saveProfileAndOpenHome(String fullName, int age) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.signup_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            UserPreferences.saveUserProfile(SignUpActivity.this, age);

            Map<String, Object> userDocument = new HashMap<>();
            userDocument.put("name", fullName);
            userDocument.put("email", user.getEmail());
            userDocument.put("age", age);
            userDocument.put("is_kid_mode", age < 18);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .set(userDocument)
                    .addOnCompleteListener(writeTask -> openHome());
        });
    }

    private void setupActions() {
        ImageView backButton = findViewById(R.id.sign_up_back_button);
        TextView signInLink = findViewById(R.id.sign_in_link);

        backButton.setOnClickListener(v -> navigateToLogin());
        signUpButton.setOnClickListener(v -> signUpUser());
        signInLink.setOnClickListener(v -> navigateToLogin());
    }

    private void signUpUser() {
        String fullName = fullNameInput.getText().toString().trim();
        String ageText = ageInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (TextUtils.isEmpty(fullName)) {
            fullNameInput.setError(getString(R.string.error_full_name_required));
            fullNameInput.requestFocus();
            return;
        }

        int age = getValidatedAge(ageText);
        if (age == -1) {
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.setError(getString(R.string.error_email_required));
            emailInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(getString(R.string.error_email_invalid));
            emailInput.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError(getString(R.string.error_password_short));
            passwordInput.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.error_password_mismatch));
            confirmPasswordInput.requestFocus();
            return;
        }

        signUpButton.setEnabled(false);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    signUpButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        saveProfileAndOpenHome(fullName, age);
                    } else {
                        Toast.makeText(
                                SignUpActivity.this,
                                task.getException() != null ? task.getException().getMessage() : getString(R.string.signup_failed),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
