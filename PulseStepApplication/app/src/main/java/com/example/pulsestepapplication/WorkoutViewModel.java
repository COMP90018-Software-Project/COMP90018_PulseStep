package com.example.pulsestepapplication;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class WorkoutViewModel extends ViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private final String userId = currentUser != null ? currentUser.getUid() : null;

    public LiveData<String> getUserName() {
        return userName;
    }

    public void fetchUserName() {
        if (userId != null) {
            DocumentReference userRef = db.collection("users").document(userId);
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Object name = documentSnapshot.get("fullName");
                    userName.setValue((String) name);
                }
            }).addOnFailureListener(e -> {
                userName.setValue("--");  // Set default value in case of failure
            });
        }

    }
}
