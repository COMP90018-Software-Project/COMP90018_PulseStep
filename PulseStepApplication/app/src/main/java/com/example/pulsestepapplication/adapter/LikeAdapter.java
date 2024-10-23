package com.example.pulsestepapplication.adapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.pulsestepapplication.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class LikeAdapter extends RecyclerView.Adapter<LikeAdapter.ViewHolder> {
    private List<String> list = new ArrayList<>();
    private Context mActivity;
    private ItemListener mItemListener;
    private FirebaseFirestore db;

    public void setItemListener(ItemListener itemListener) {
        this.mItemListener = itemListener;
    }

    public LikeAdapter(Activity activity, List<String> list) {
        this.mActivity = activity;
        this.list = list;
        // 获取 Firestore 实例
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.item_like, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        String collection = list.get(i);
        if (collection != null) {
            getUserInfo(viewHolder.profileImage,viewHolder.userName,collection);
        }
    }

    private void getUserInfo(CircleImageView imageView, TextView textView, String userId) {
        // 从 Firestore 中获取用户信息
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 获取用户的全名
                        String fullName = documentSnapshot.getString("fullName");
                        textView.post(() -> textView.setText(fullName));
                    }
                });

        FirebaseStorage.getInstance().getReference()
                .child("users")
                .child(userId)
                .child("images/profile_image")
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    // 成功获取到图片 URL，设置用户自定义头像
                    Log.e("ProfileFragment", uri + "");
                    Glide.with(mActivity).load(uri).apply(RequestOptions.circleCropTransform()).into(imageView);
                })
                .addOnFailureListener(exception -> {
                    Log.e("ProfileFragment",   "文件不存在");
                    // 文件不存在，处理 StorageException，并根据性别设置默认头像
                    if (exception instanceof StorageException) {
                        StorageException storageException = (StorageException) exception;
                        if (storageException.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            imageView.setImageResource(R.drawable.default_avatar);
                        } else {
                            Log.e("ProfileFragment", "Error fetching profile image: " + exception.getMessage());
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView profileImage;
        private TextView userName;
        private TextView project;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            userName = itemView.findViewById(R.id.userName);
            project = itemView.findViewById(R.id.project);
        }
    }

    public interface ItemListener {
        void ItemClick(String collection);

        void Delete(String collection);
    }
}
