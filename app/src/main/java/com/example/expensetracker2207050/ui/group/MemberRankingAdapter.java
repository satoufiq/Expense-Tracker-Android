package com.example.expensetracker2207050.ui.group;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensetracker2207050.R;
import com.example.expensetracker2207050.databinding.ItemMemberRankingBinding;
import com.example.expensetracker2207050.models.GroupMember;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemberRankingAdapter extends RecyclerView.Adapter<MemberRankingAdapter.RankingViewHolder> {

    private List<GroupMember> members = new ArrayList<>();

    public void setMembers(List<GroupMember> members) {
        this.members = members;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RankingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMemberRankingBinding binding = ItemMemberRankingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RankingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RankingViewHolder holder, int position) {
        holder.bind(members.get(position), position + 1);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class RankingViewHolder extends RecyclerView.ViewHolder {
        private final ItemMemberRankingBinding binding;

        RankingViewHolder(ItemMemberRankingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(GroupMember member, int rank) {
            String rankText;
            switch (rank) {
                case 1:
                    rankText = "🥇";
                    break;
                case 2:
                    rankText = "🥈";
                    break;
                case 3:
                    rankText = "🥉";
                    break;
                default:
                    rankText = String.valueOf(rank);
            }
            binding.tvRank.setText(rankText);
            binding.tvMemberName.setText(member.getUsername() != null ? member.getUsername() : "Unknown");
            binding.tvAmount.setText(String.format(Locale.getDefault(), "৳%.2f", member.getTotalSpent()));

            if (rank == 1) {
                binding.tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary));
            } else if (rank == 2) {
                binding.tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.secondary));
            } else {
                binding.tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            }
        }
    }
}

