package com.example.aplicaciocorreu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EmailAdapter extends RecyclerView.Adapter<EmailAdapter.EmailViewHolder> {
    private List<Email> emailList;
    private final OnDeleteClickListener onDeleteClickListener;

    // Interfície dins de l'adapter
    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    public EmailAdapter(List<Email> emailList, OnDeleteClickListener listener) {
        this.emailList = emailList;
        this.onDeleteClickListener = listener;
    }
    @NonNull
    @Override
    public EmailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_email, parent, false);
        return new EmailViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EmailViewHolder holder, int position) {
        Email email = emailList.get(position);

        holder.tvFrom.setText(email.getFrom());
        holder.tvSubject.setText(email.getSubject());
        holder.tvPreview.setText(email.getPreview());
        holder.tvDate.setText(email.getDate());

        // Configura el clic al botó d'eliminar
        holder.btnDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(position);
            }
        });
    }

    public void setEmails(List<Email> emails) {
        this.emailList = emails;
        notifyDataSetChanged();
    }

    public Email getEmailAtPosition(int position) {
        return emailList.get(position);
    }

    @Override
    public int getItemCount() {
        return emailList != null ? emailList.size() : 0;
    }

    static class EmailViewHolder extends RecyclerView.ViewHolder {
        TextView tvFrom, tvSubject, tvPreview, tvDate;
        ImageButton btnDelete;

        public EmailViewHolder(View itemView) {
            super(itemView);
            tvFrom = itemView.findViewById(R.id.tvFrom);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}