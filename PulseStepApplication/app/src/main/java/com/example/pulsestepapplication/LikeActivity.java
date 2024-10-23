package com.example.pulsestepapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.pulsestepapplication.adapter.LikeAdapter;
import com.example.pulsestepapplication.databinding.ActivityLikeBinding;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;

public class LikeActivity extends AppCompatActivity {
    private ActivityLikeBinding binding;
    private LikeAdapter likeAdapter;
    private FirebaseFirestore db;
    DocumentSnapshot documentSnapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLikeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 获取 Firestore 实例
        db = FirebaseFirestore.getInstance();

        // switch ranking list (monthly/daily)
        MaterialButtonToggleGroup toggleButton = findViewById(R.id.bt_switch_rank);
        toggleButton.check(R.id.bt_daily);
        TextView titleTextView = findViewById(R.id.rank_title);
        getData("dailyLike");
        toggleButton.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            // Respond to button selection
            if (isChecked) {
                switch (checkedId) {
                    case R.id.bt_daily:
                        getData("dailyLike");
                        break;
                    case R.id.bt_monthly:
                        getData("monthlyLike");
                        break;
                }
            }
        });

        binding.likeList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        binding.back.setOnClickListener(view -> finish());
    }

    private void getData(String day) {
        // 获取当前用户的 UID
        String userId = getIntent().getStringExtra("USER_ID");
        // 从 Firestore 中获取点赞信息
        if (this.documentSnapshot == null) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            this.documentSnapshot = documentSnapshot;
                            showData(day);
                        } else {
                            Log.e("信息", "不存在");
                        }
                    });
        } else {
            showData(day);
        }
    }

    private void showData(String day) {
        Log.e("信息", this.documentSnapshot.toString());
        Object like = this.documentSnapshot.get(day, Object.class);
        if (like == null) {
            Toast.makeText(this, "no data", Toast.LENGTH_SHORT).show();
            return;
        }

        String data = like.toString();
        data = data.substring(data.indexOf("=") + 1, data.length() - 1);
        List<String> list = Arrays.asList(data.replaceAll("[\\[\\]]", "").split(",\\s*"));

        likeAdapter = new LikeAdapter(LikeActivity.this, list);
        binding.likeList.setAdapter(likeAdapter);
    }
}