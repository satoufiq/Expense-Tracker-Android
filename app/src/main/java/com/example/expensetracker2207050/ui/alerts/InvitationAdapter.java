package com.example.expensetracker2207050.ui.alerts;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensetracker2207050.databinding.ItemInvitationBinding;
import com.example.expensetracker2207050.models.Invitation;
import java.util.ArrayList;
import java.util.List;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.InvitationViewHolder> {

    private List<Invitation> invitations = new ArrayList<>();
    private final OnInviteActionListener listener;

    public interface OnInviteActionListener {
        void onAccept(Invitation invitation);
        void onReject(Invitation invitation);
    }

    public InvitationAdapter(OnInviteActionListener listener) {
        this.listener = listener;
    }

    public void setInvitations(List<Invitation> invitations) {
        this.invitations = invitations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInvitationBinding binding = ItemInvitationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new InvitationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        holder.bind(invitations.get(position));
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    class InvitationViewHolder extends RecyclerView.ViewHolder {
        private final ItemInvitationBinding binding;

        public InvitationViewHolder(ItemInvitationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Invitation invitation) {
            binding.tvInviteType.setText(invitation.getType() + " INVITATION");
            binding.tvInviteMessage.setText("You have a new " + invitation.getType().toLowerCase() + " invitation.");

            binding.btnAccept.setOnClickListener(v -> listener.onAccept(invitation));
            binding.btnReject.setOnClickListener(v -> listener.onReject(invitation));
        }
    }
}
