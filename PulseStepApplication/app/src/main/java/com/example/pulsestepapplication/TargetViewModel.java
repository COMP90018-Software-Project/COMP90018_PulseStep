package com.example.pulsestepapplication;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TargetViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<String> targetHours = new MutableLiveData<>();
    private final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private final String userId = currentUser != null ? currentUser.getUid() : null;

    public LiveData<String> getTargetHours() {
        return targetHours;
    }

    public void fetchTargetHours() {
        if (userId != null) {
            DocumentReference userRef = db.collection("users").document(userId);
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Object target = documentSnapshot.get("target");
                    if (target != null) {
                        targetHours.setValue((String) target);
                    } else {
                        targetHours.setValue("--");  // Default value
                    }
                }
            }).addOnFailureListener(e -> {
                targetHours.setValue("--");  // Set default value in case of failure
            });
        }
    }

    public void updateTargetHours(String newTarget) {
        if (userId != null) {
            db.collection("users").document(userId)
                    .update("target", newTarget)
                    .addOnSuccessListener(aVoid -> targetHours.setValue(newTarget))
                    .addOnFailureListener(e -> {
                        // Handle the error if needed
                    });
        }
    }
}
