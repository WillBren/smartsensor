package com.example.steppcounter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class Login extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_login);

        final EditText usernameInput = findViewById(R.id.username_input);
        final EditText emailInput = findViewById(R.id.email_input);
        Button loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                String username = usernameInput.getText().toString().trim();

                if (!email.isEmpty() && !username.isEmpty()) {
                    // Generate a random MFA code
                    String mfaCode = generateMfaCode();

                    // Send the MFA code to the email
                    sendMfaCodeToEmail(email, mfaCode);

                    // Pass the MFA code to the next activity
                    Intent intent = new Intent(Login.this, MfaActivity.class);
                    intent.putExtra("MFA_CODE", mfaCode);
                    intent.putExtra("EMAIL", email);
                    startActivity(intent);
                } else {
                    Toast.makeText(Login.this, "Please enter both username and email", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String generateMfaCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void sendMfaCodeToEmail(String email, String mfaCode) {
        // Here you would integrate with an email service to send the MFA code.
        // For now, we'll just log it as a placeholder.
        Log.d("MFA", "Sending MFA code " + mfaCode + " to " + email);
    }
}