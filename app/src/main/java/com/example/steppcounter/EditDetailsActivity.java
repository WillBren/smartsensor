package com.example.steppcounter;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditDetailsActivity extends AppCompatActivity {

    private EditText etName, etAge, etWeight, etHeight, etGender;
    private Button btnSaveDetails;
    private FirebaseFirestore firestore;
    private FirebaseAuth mAuth;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_details);

        // Initialize Firebase instances
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get current user ID
        userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        // Link UI elements
        etName = findViewById(R.id.et_name);
        etAge = findViewById(R.id.et_age);
        etWeight = findViewById(R.id.et_weight);
        etHeight = findViewById(R.id.et_height);
        etGender = findViewById(R.id.et_gender);
        btnSaveDetails = findViewById(R.id.btn_save_details);

        // Set up button listener for saving details
        btnSaveDetails.setOnClickListener(v -> saveUserDetails());
    }

    private void saveUserDetails() {
        String name = etName.getText().toString();
        String age = etAge.getText().toString();
        String weight = etWeight.getText().toString();
        String height = etHeight.getText().toString();
        String gender = etGender.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(age) || TextUtils.isEmpty(weight) || TextUtils.isEmpty(height) || TextUtils.isEmpty(gender)) {
            Toast.makeText(EditDetailsActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a map of the data to store
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("name", name);
        userDetails.put("age", Integer.parseInt(age));
        userDetails.put("weight", Double.parseDouble(weight));
        userDetails.put("height", Double.parseDouble(height));
        userDetails.put("gender", gender);

        // Get the user's document and add data to the "Details" collection under it
        CollectionReference detailsCollectionRef = firestore.collection("users")
                .document(userId)  // Targeting the correct user document
                .collection("Details");  // Collection under that document

        // Add a document to the "Details" collection under this user
        detailsCollectionRef.document("personalInfo").set(userDetails)  // You can specify a custom document name like "personalInfo"
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(EditDetailsActivity.this, "Details updated successfully", Toast.LENGTH_SHORT).show();
                        finish();  // Close the activity after saving
                    } else {
                        Toast.makeText(EditDetailsActivity.this, "Failed to update details", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
