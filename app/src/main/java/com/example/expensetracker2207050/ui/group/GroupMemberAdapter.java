package com.example.expensetracker2207050.ui.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensetracker2207050.databinding.ItemGroupMemberBinding;
import com.example.expensetracker2207050.models.GroupMember;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder> {

    private List<GroupMember> members = new ArrayList<>();
    private OnMemberClickListener clickListener;
    private OnMemberLongClickListener longClickListener;
    private boolean isCurrentUserAdmin = false;

    public interface OnMemberClickListener {
        void onMemberClick(GroupMember member);
    }

    public interface OnMemberLongClickListener {
        void onMemberLongClick(GroupMember member);
    }

    public void setOnMemberClickListener(OnMemberClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnMemberLongClickListener(OnMemberLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setCurrentUserAdmin(boolean isAdmin) {
        this.isCurrentUserAdmin = isAdmin;
        notifyDataSetChanged();
    }

    public void setMembers(List<GroupMember> members) {
        this.members = members;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGroupMemberBinding binding = ItemGroupMemberBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MemberViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        holder.bind(members.get(position));
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private final ItemGroupMemberBinding binding;

        MemberViewHolder(ItemGroupMemberBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(GroupMember member) {
            binding.tvMemberName.setText(member.getUsername() != null ? member.getUsername() : "Unknown");
            binding.tvMemberEmail.setText(member.getEmail() != null ? member.getEmail() : "");
            binding.tvMemberSpent.setText(String.format(Locale.getDefault(), "৳%.2f", member.getTotalSpent()));
            binding.tvTransactionCount.setText(String.format(Locale.getDefault(), "%d transactions", member.getTransactionCount()));

            binding.chipAdmin.setVisibility(member.isAdmin() ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onMemberClick(member);
                }
            });

            // Long click for admin to remove members (but not the admin themselves)
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null && isCurrentUserAdmin && !member.isAdmin()) {
                    longClickListener.onMemberLongClick(member);
                    return true;
                }
                return false;
            });
        }
    }
}

