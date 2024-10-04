package com.example.steppcounter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MfaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_mfa_activity);

        final EditText mfaCodeInput = findViewById(R.id.mfa_code_input);
        Button verifyButton = findViewById(R.id.verify_button);

        // Get the passed MFA code from the LoginActivity
        final String generatedMfaCode = getIntent().getStringExtra("MFA_CODE");

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputtedCode = mfaCodeInput.getText().toString().trim();

                // Check if the code matches
                if (inputtedCode.equals(generatedMfaCode)) {
                    // If correct, log the user into the app
                    Toast.makeText(MfaActivity.this, "MFA Code Verified. Logging in...", Toast.LENGTH_SHORT).show();

                    // Redirect to MainActivity
                    Intent intent = new Intent(MfaActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // Close this activity so the user can't go back to the MFA screen
                } else {
                    Toast.makeText(MfaActivity.this, "Invalid MFA Code", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}