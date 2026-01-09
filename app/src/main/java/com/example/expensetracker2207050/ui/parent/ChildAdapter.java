package com.example.expensetracker2207050.ui.parent;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensetracker2207050.databinding.ItemChildBinding;
import com.example.expensetracker2207050.models.User;
import java.util.ArrayList;
import java.util.List;

public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ChildViewHolder> {

    private List<User> children = new ArrayList<>();
    private final OnChildClickListener listener;
    private OnMessageClickListener messageClickListener;

    public interface OnChildClickListener {
        void onChildClick(User child);
    }

    public interface OnMessageClickListener {
        void onMessageClick(User child);
    }

    public ChildAdapter(OnChildClickListener listener) {
        this.listener = listener;
    }

    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.messageClickListener = listener;
    }

    public void setChildren(List<User> children) {
        this.children = children;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChildBinding binding = ItemChildBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ChildViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        holder.bind(children.get(position));
    }

    @Override
    public int getItemCount() {
        return children.size();
    }

    class ChildViewHolder extends RecyclerView.ViewHolder {
        private final ItemChildBinding binding;

        public ChildViewHolder(ItemChildBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(User child) {
            binding.tvChildName.setText(child.getUsername());
            binding.tvChildEmail.setText(child.getEmail());
            binding.btnViewExpenses.setOnClickListener(v -> listener.onChildClick(child));
            binding.btnMessage.setOnClickListener(v -> {
                if (messageClickListener != null) {
                    messageClickListener.onMessageClick(child);
                }
            });
        }
    }
}
