package com.example.spytool.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.spytool.R;
import com.example.spytool.model.User;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private List<User> usersList;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UsersAdapter(List<User> usersList, OnUserClickListener listener) {
        this.usersList = usersList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = usersList.get(position);
        holder.bind(user);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public void updateUsers(List<User> newUsers) {
        usersList.clear();
        usersList.addAll(newUsers);
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewUserName;
        private TextView textViewUserEmail;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUserName = itemView.findViewById(R.id.textViewUserName);
            textViewUserEmail = itemView.findViewById(R.id.textViewUserEmail);
        }

        public void bind(User user) {
            String displayName = user.getDisplayName();

            if (displayName == null || displayName.trim().isEmpty()) {
                String email = user.getEmail();
                if (email != null && email.contains("@")) {
                    displayName = email.substring(0, email.indexOf("@"));
                    if (!displayName.isEmpty()) {
                        displayName = displayName.substring(0, 1).toUpperCase() +
                                displayName.substring(1);
                    }
                } else {
                    displayName = "Пользователь";
                }
            }

            String email = user.getEmail() != null ? user.getEmail() : "Email не указан";

            textViewUserName.setText(displayName);
            textViewUserEmail.setText(email);
        }
    }
}