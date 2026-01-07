package com.example.expensetracker2207050.ui.alerts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.expensetracker2207050.databinding.FragmentAlertsBinding;
import com.example.expensetracker2207050.models.Alert;
import com.example.expensetracker2207050.models.Invitation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.List;

public class AlertsFragment extends Fragment implements InvitationAdapter.OnInviteActionListener, AlertAdapter.OnAlertClickListener {

    private FragmentAlertsBinding binding;
    private FirebaseFirestore db;
    private InvitationAdapter invitationAdapter;
    private AlertAdapter alertAdapter;
    private ConcatAdapter concatAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAlertsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        // Back button
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        setupRecyclerView();
        loadInvitations();
        loadAlerts();
    }

    private void setupRecyclerView() {
        invitationAdapter = new InvitationAdapter(this);
        alertAdapter = new AlertAdapter(this);
        concatAdapter = new ConcatAdapter(invitationAdapter, alertAdapter);

        binding.rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAlerts.setAdapter(concatAdapter);
    }

    private void loadInvitations() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        db.collection("invitations")
                .whereEqualTo("receiverEmail", email)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((value, error) -> {
                    if (value != null && isAdded()) {
                        List<Invitation> list = value.toObjects(Invitation.class);
                        invitationAdapter.setInvitations(list);
                        updateEmptyState();
                    }
                });
    }

    private void loadAlerts() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("alerts")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null && isAdded()) {
                        List<Alert> list = value.toObjects(Alert.class);
                        alertAdapter.setAlerts(list);
                        updateEmptyState();
                    }
                });
    }

    private void updateEmptyState() {
        boolean isEmpty = invitationAdapter.getItemCount() == 0 && alertAdapter.getItemCount() == 0;
        binding.tvNoAlerts.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAccept(Invitation invitation) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        if ("GROUP".equals(invitation.getType())) {
            // 1. Add user to group
            db.collection("groups").document(invitation.getTargetId())
                    .update("memberIds", FieldValue.arrayUnion(uid))
                    .addOnSuccessListener(aVoid -> {
                        // 2. Mark invitation as accepted
                        db.collection("invitations").document(invitation.getId())
                                .update("status", "ACCEPTED")
                                .addOnSuccessListener(a2 -> {
                                    Toast.makeText(getContext(), "Joined group successfully!", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else if ("PARENT".equals(invitation.getType())) {
            db.collection("users").document(uid)
                    .update("linkedParentUid", invitation.getSenderId())
                    .addOnSuccessListener(aVoid -> {
                        db.collection("invitations").document(invitation.getId())
                                .update("status", "ACCEPTED")
                                .addOnSuccessListener(a2 -> {
                                    Toast.makeText(getContext(), "Parent linked!", Toast.LENGTH_SHORT).show();
                                });
                    });
        }
    }

    @Override
    public void onReject(Invitation invitation) {
        db.collection("invitations").document(invitation.getId()).update("status", "REJECTED");
        Toast.makeText(getContext(), "Rejected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAlertClick(Alert alert) {
        if (!alert.isSeen()) {
            db.collection("alerts").document(alert.getId()).update("seen", true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
