package com.example.spytool.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spytool.R;
import com.example.spytool.model.StegoMessage;
import com.example.spytool.repository.DropboxRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StegoMessagesAdapter
        extends RecyclerView.Adapter<StegoMessagesAdapter.MessageViewHolder> {

    public interface OnMessageClickListener {
        void onDecodeClick(StegoMessage message);
    }

    private final List<StegoMessage> messagesList;
    private final OnMessageClickListener listener;

    public StegoMessagesAdapter(List<StegoMessage> messagesList,
                                OnMessageClickListener listener) {
        this.messagesList = messagesList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stego_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        StegoMessage message = messagesList.get(position);
        holder.bind(message);

        holder.buttonDecode.setOnClickListener(v -> {
            if (listener != null && !message.isDecoded()) {
                listener.onDecodeClick(message);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        TextView textViewFileName, textViewTime, textViewStatus;
        Button buttonDecode;
        ImageView imageViewPreview;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewFileName = itemView.findViewById(R.id.textViewFileName);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            buttonDecode = itemView.findViewById(R.id.buttonDecode);
        }

        void bind(StegoMessage message) {

            textViewFileName.setText(
                    message.getFileName() != null
                            ? message.getFileName()
                            : "stego_image.png"
            );

            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());
            textViewTime.setText(sdf.format(new Date(message.getTimestamp())));

            if (message.isDecoded()) {
                textViewStatus.setText("✅ Декодировано");
                buttonDecode.setEnabled(false);
                buttonDecode.setText("Уже декодировано");
            } else {
                textViewStatus.setText("⏳ Ожидает");
                buttonDecode.setEnabled(true);
                buttonDecode.setText("🔍 Декодировать");
            }


        }
    }
}

