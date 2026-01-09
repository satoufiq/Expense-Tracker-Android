package com.example.expensetracker2207050.ui.group;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.expensetracker2207050.R;
import com.example.expensetracker2207050.databinding.FragmentGroupMembersBinding;
import com.example.expensetracker2207050.models.Expense;
import com.example.expensetracker2207050.models.Group;
import com.example.expensetracker2207050.models.GroupMember;
import com.example.expensetracker2207050.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupMembersFragment extends Fragment {

    private FragmentGroupMembersBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private GroupMemberAdapter adapter;
    private String groupId;
    private String groupName;
    private List<GroupMember> membersList = new ArrayList<>();
    private boolean isCurrentUserAdmin = false;
    private Group currentGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGroupMembersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            groupId = getArguments().getString("groupId");
            groupName = getArguments().getString("groupName");
        }

        setupRecyclerView();
        setupClickListeners();
        loadGroupData();
    }

    private void setupRecyclerView() {
        adapter = new GroupMemberAdapter();
        binding.rvMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvMembers.setAdapter(adapter);

        adapter.setOnMemberClickListener(member -> {
            if (isCurrentUserAdmin) {
                showMemberOptionsDialog(member);
            } else {
                Toast.makeText(getContext(), "Viewing " + member.getUsername() + "'s profile", Toast.LENGTH_SHORT).show();
            }
        });

        adapter.setOnMemberLongClickListener(member -> {
            if (isCurrentUserAdmin) {
                showMemberOptionsDialog(member);
            }
        });
    }

    private void showMemberOptionsDialog(GroupMember member) {
        String currentUserId = mAuth.getUid();

        // Can't modify yourself
        if (member.getUid().equals(currentUserId)) {
            Toast.makeText(getContext(), "You cannot modify your own status", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> options = new ArrayList<>();
        options.add("View Profile");

        if (isCurrentUserAdmin) {
            if (member.isAdmin()) {
                options.add("Remove Admin Role");
            } else {
                options.add("Make Admin");
            }
            options.add("Remove from Group");
        }

        new AlertDialog.Builder(getContext())
                .setTitle(member.getUsername())
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    switch (which) {
                        case 0: // View Profile
                            Toast.makeText(getContext(), "Viewing " + member.getUsername() + "'s profile", Toast.LENGTH_SHORT).show();
                            break;
                        case 1: // Make Admin / Remove Admin Role
                            if (member.isAdmin()) {
                                removeAdminRole(member);
                            } else {
                                makeAdmin(member);
                            }
                            break;
                        case 2: // Remove from Group
                            confirmRemoveMember(member);
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void makeAdmin(GroupMember member) {
        if (currentGroup == null) return;

        db.collection("groups").document(groupId)
                .update("adminIds", FieldValue.arrayUnion(member.getUid()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), member.getUsername() + " is now an admin", Toast.LENGTH_SHORT).show();
                    loadGroupData(); // Refresh the list
                })
                .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to add admin: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void removeAdminRole(GroupMember member) {
        if (currentGroup == null) return;

        // Can't remove the original creator from admin
        if (member.getUid().equals(currentGroup.getAdminId())) {
            Toast.makeText(getContext(), "Cannot remove admin role from group creator", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("groups").document(groupId)
                .update("adminIds", FieldValue.arrayRemove(member.getUid()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), member.getUsername() + " is no longer an admin", Toast.LENGTH_SHORT).show();
                    loadGroupData(); // Refresh the list
                })
                .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to remove admin: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void confirmRemoveMember(GroupMember member) {
        new AlertDialog.Builder(getContext())
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to remove " + member.getUsername() + " from the group?")
                .setPositiveButton("Remove", (d, w) -> removeMember(member))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeMember(GroupMember member) {
        if (currentGroup == null) return;

        // Can't remove the original creator
        if (member.getUid().equals(currentGroup.getAdminId())) {
            Toast.makeText(getContext(), "Cannot remove the group creator", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove from memberIds and adminIds (if applicable)
        db.collection("groups").document(groupId)
                .update("memberIds", FieldValue.arrayRemove(member.getUid()),
                        "adminIds", FieldValue.arrayRemove(member.getUid()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), member.getUsername() + " has been removed from the group", Toast.LENGTH_SHORT).show();
                    loadGroupData(); // Refresh the list
                })
                .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to remove member: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        binding.btnCompareAnalytics.setOnClickListener(v -> {
            if (groupId != null && !groupId.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putString("groupId", groupId);
                bundle.putString("groupName", groupName);
                Navigation.findNavController(v).navigate(R.id.action_groupMembers_to_compareAnalytics, bundle);
            }
        });
    }

    private void loadGroupData() {
        if (groupId == null || groupId.isEmpty()) {
            showEmpty();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvGroupName.setText(groupName != null ? groupName : "Group");

        // Load group to get member IDs
        db.collection("groups").document(groupId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;

                    Group group = documentSnapshot.toObject(Group.class);
                    if (group != null && group.getMemberIds() != null) {
                        loadMembersDetails(group);
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        showEmpty();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    showEmpty();
                });
    }

    private void loadMembersDetails(Group group) {
        currentGroup = group;
        List<String> memberIds = group.getMemberIds();
        List<String> adminIds = group.getAdminIds();
        String currentUserId = mAuth.getUid();

        // Check if current user is admin
        isCurrentUserAdmin = group.isAdmin(currentUserId);
        adapter.setCurrentUserAdmin(isCurrentUserAdmin);

        binding.tvTotalMembers.setText(String.valueOf(memberIds.size()));

        membersList.clear();
        Map<String, GroupMember> memberMap = new HashMap<>();

        // First, get all expenses for this group to calculate stats
        db.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener(expenseSnapshots -> {
                    if (!isAdded()) return;

                    // Calculate spending per member
                    Map<String, Double> memberSpending = new HashMap<>();
                    Map<String, Integer> memberTransactions = new HashMap<>();
                    double totalGroupSpent = 0;

                    for (QueryDocumentSnapshot doc : expenseSnapshots) {
                        Expense expense = doc.toObject(Expense.class);
                        String contributorId = expense.getContributorId();
                        if (contributorId == null) contributorId = expense.getUserId();

                        double amount = expense.getAmount();
                        totalGroupSpent += amount;

                        memberSpending.put(contributorId,
                                memberSpending.getOrDefault(contributorId, 0.0) + amount);
                        memberTransactions.put(contributorId,
                                memberTransactions.getOrDefault(contributorId, 0) + 1);
                    }

                    binding.tvTotalSpent.setText(String.format(Locale.getDefault(), "৳%.0f", totalGroupSpent));

                    // Now fetch user details for each member
                    final double finalTotalSpent = totalGroupSpent;
                    final int[] loadedCount = {0};

                    for (String memberId : memberIds) {
                        db.collection("users").document(memberId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (!isAdded()) return;

                                    GroupMember member;
                                    if (userDoc.exists()) {
                                        User user = userDoc.toObject(User.class);
                                        member = new GroupMember(memberId,
                                                user != null ? user.getUsername() : "Unknown",
                                                user != null ? user.getEmail() : "");
                                    } else {
                                        member = new GroupMember(memberId, "Unknown User", "");
                                    }

                                    member.setTotalSpent(memberSpending.getOrDefault(memberId, 0.0));
                                    member.setTransactionCount(memberTransactions.getOrDefault(memberId, 0));
                                    // Check if member is in adminIds list
                                    member.setAdmin(group.isAdmin(memberId));

                                    membersList.add(member);
                                    loadedCount[0]++;

                                    if (loadedCount[0] == memberIds.size()) {
                                        // Sort by total spent (descending)
                                        membersList.sort((m1, m2) -> Double.compare(m2.getTotalSpent(), m1.getTotalSpent()));
                                        adapter.setMembers(membersList);
                                        binding.progressBar.setVisibility(View.GONE);

                                        if (membersList.isEmpty()) {
                                            showEmpty();
                                        } else {
                                            binding.llEmpty.setVisibility(View.GONE);
                                            binding.rvMembers.setVisibility(View.VISIBLE);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    loadedCount[0]++;
                                    if (loadedCount[0] == memberIds.size()) {
                                        adapter.setMembers(membersList);
                                        binding.progressBar.setVisibility(View.GONE);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    showEmpty();
                });
    }

    private void showEmpty() {
        binding.llEmpty.setVisibility(View.VISIBLE);
        binding.rvMembers.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

