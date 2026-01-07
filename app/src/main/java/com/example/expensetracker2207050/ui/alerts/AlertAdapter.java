package com.example.expensetracker2207050.ui.alerts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensetracker2207050.databinding.ItemAlertBinding;
import com.example.expensetracker2207050.models.Alert;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private List<Alert> alerts = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
    private final OnAlertClickListener listener;

    public interface OnAlertClickListener {
        void onAlertClick(Alert alert);
    }

    public AlertAdapter(OnAlertClickListener listener) {
        this.listener = listener;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlertBinding binding = ItemAlertBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new AlertViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        holder.bind(alerts.get(position));
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    class AlertViewHolder extends RecyclerView.ViewHolder {
        private final ItemAlertBinding binding;

        public AlertViewHolder(ItemAlertBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Alert alert) {
            binding.tvAlertTitle.setText(alert.getTitle());
            binding.tvAlertMessage.setText(alert.getMessage());
            if (alert.getTimestamp() != null) {
                binding.tvAlertTime.setText(sdf.format(alert.getTimestamp()));
            }

            binding.getRoot().setAlpha(alert.isSeen() ? 0.6f : 1.0f);

            if ("BUDGET".equals(alert.getType())) {
                binding.tvAlertTitle.setTextColor(binding.getRoot().getContext().getResources().getColor(android.R.color.holo_red_light));
            } else if ("SUGGESTION".equals(alert.getType())) {
                binding.tvAlertTitle.setTextColor(binding.getRoot().getContext().getResources().getColor(android.R.color.holo_blue_light));
            } else {
                binding.tvAlertTitle.setTextColor(binding.getRoot().getContext().getResources().getColor(com.example.expensetracker2207050.R.color.primary));
            }

            binding.getRoot().setOnClickListener(v -> listener.onAlertClick(alert));
        }
    }
}
