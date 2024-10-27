package com.example.pulsestepapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.pulsestepapplication.adapter.LikeAdapter;
import com.example.pulsestepapplication.bean.MessageBean;
import com.example.pulsestepapplication.databinding.ActivityLikeBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.List;

public class LikeActivity extends AppCompatActivity {
    private ActivityLikeBinding binding;
    private LikeAdapter likeAdapter;
    private FirebaseFirestore db;


    private List<MessageBean> messageBeanList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLikeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        messageBeanList = new ArrayList<>();
        likeAdapter = new LikeAdapter(LikeActivity.this, messageBeanList);
        binding.likeList.setAdapter(likeAdapter);

        // 获取 Firestore 实例
        db = FirebaseFirestore.getInstance();

        getData();

        binding.likeList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        binding.back.setOnClickListener(view -> finish());

        // 设置长按删除监听
        likeAdapter.setItemListener(new LikeAdapter.ItemListener() {
            @Override
            public void ItemClick(String collection) {

            }

            @Override
            public void delete(int position) {
                MessageBean message = messageBeanList.get(position);

                // 显示删除确认对话框
                new AlertDialog.Builder(LikeActivity.this)
                        .setTitle("Confirm Deletion")
                        .setMessage("Are you sure you want to delete this message?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            db.collection("message").document(message.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        // 从列表中删除并刷新
                                        messageBeanList.remove(position);
                                        likeAdapter.notifyItemRemoved(position);
                                        Toast.makeText(LikeActivity.this, "Message has deleted", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(LikeActivity.this, "Delete failed" + e.getMessage(), Toast.LENGTH_SHORT).show();

                                    });
                        }).setNegativeButton("Cancel",null)
                        .show();
            }
        });
    }

    private void getData() {
        // 获取当前用户的 UID
        String userId = getIntent().getStringExtra("USER_ID");
        // 从 Firestore 中获取点赞信息
        db.collection("message")
                .whereEqualTo("updateUserId", userId)
                .get(Source.SERVER)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            MessageBean messageBean = document.toObject(MessageBean.class);
                            messageBeanList.add(messageBean);
                        }
                        messageBeanList.sort((msg1, msg2) -> Long.compare(msg2.getTimestamp(),
                                msg1.getTimestamp()));
                        likeAdapter.notifyDataSetChanged();
                        readMessage(messageBeanList);
                    } else {
                        Log.w("Firestore", "Error getting notifications", task.getException());
                    }
                });

    }

    private void readMessage(List<MessageBean> messageBeanList) {
        for (int i = 0; i < messageBeanList.size(); i++) {

            if ("0".equals(messageBeanList.get(i).getIsRead())) {
                String messageId = messageBeanList.get(i).getId();
                DocumentReference msgRef = db.collection("message").document(messageId);

                msgRef.update("isRead", "1")
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firestore", "Notification marked as read successfully!");
                        })
                        .addOnFailureListener(e -> {
                            Log.w("Firestore", "Error updating notification", e);
                        });
            }


        }
    }

}