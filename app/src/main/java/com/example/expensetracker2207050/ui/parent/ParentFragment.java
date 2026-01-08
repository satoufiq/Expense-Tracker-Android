package com.example.expensetracker2207050.ui.parent;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.expensetracker2207050.R;
import com.example.expensetracker2207050.databinding.FragmentParentBinding;
import com.example.expensetracker2207050.models.Alert;
import com.example.expensetracker2207050.models.Invitation;
import com.example.expensetracker2207050.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.UUID;

public class ParentFragment extends Fragment implements ChildAdapter.OnChildClickListener {

    private FragmentParentBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ChildAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentParentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
        loadLinkedChildren();

        // Back button
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        binding.btnInviteChild.setOnClickListener(v -> showInviteChildDialog());
    }

    private void setupRecyclerView() {
        adapter = new ChildAdapter(this);
        binding.rvChildren.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvChildren.setAdapter(adapter);
    }

    private void loadLinkedChildren() {
        String parentUid = mAuth.getCurrentUser().getUid();
        db.collection("users")
                .whereEqualTo("linkedParentUid", parentUid)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        java.util.List<User> children = value.toObjects(User.class);
                        adapter.setChildren(children);
                        if (isAdded()) {
                            binding.tvNoChildren.setVisibility(children.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }
                });
    }

    private void showInviteChildDialog() {
        EditText etEmail = new EditText(getContext());
        etEmail.setHint("Child's Email");
        new AlertDialog.Builder(getContext())
                .setTitle("Invite Child")
                .setView(etEmail)
                .setPositiveButton("Invite", (d, w) -> {
                    String email = etEmail.getText().toString().trim();
                    if (!email.isEmpty()) {
                        String id = UUID.randomUUID().toString();
                        Invitation invitation = new Invitation(id, mAuth.getCurrentUser().getUid(), email, mAuth.getCurrentUser().getUid(), "PARENT");
                        db.collection("invitations").document(id).set(invitation);
                        Toast.makeText(getContext(), "Invitation sent", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onChildClick(User child) {
        Bundle bundle = new Bundle();
        bundle.putString("childUid", child.getUid());
        bundle.putString("childName", child.getUsername());

        new AlertDialog.Builder(getContext())
                .setTitle("Options for " + child.getUsername())
                .setItems(new CharSequence[]{"📊 View Expenses & Analytics", "💡 Send Suggestion"}, (dialog, which) -> {
                    if (which == 0) {
                        Navigation.findNavController(requireView()).navigate(R.id.action_parent_to_childExpenses, bundle);
                    } else {
                        showSendSuggestionDialog(child);
                    }
                })
                .show();
    }

    private void showSendSuggestionDialog(User child) {
        EditText etSuggestion = new EditText(getContext());
        etSuggestion.setHint("Type your suggestion here...");

        new AlertDialog.Builder(getContext())
                .setTitle("Suggestions for " + child.getUsername())
                .setView(etSuggestion)
                .setPositiveButton("Send", (d, w) -> {
                    String text = etSuggestion.getText().toString().trim();
                    if (!text.isEmpty()) {
                        sendSuggestion(child, text);
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void sendSuggestion(User child, String text) {
        String parentName = "Parent"; // Could load from user profile

        Alert alert = new Alert(UUID.randomUUID().toString(), child.getUid(), "Parent Suggestion", text, "SUGGESTION");
        db.collection("alerts").document(alert.getId()).set(alert)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Suggestion sent to " + child.getUsername(), Toast.LENGTH_SHORT).show());

        // Also persist in suggestions collection as per guidelines
        java.util.Map<String, Object> suggestion = new java.util.HashMap<>();
        suggestion.put("parentId", mAuth.getUid());
        suggestion.put("childId", child.getUid());
        suggestion.put("message", text);
        suggestion.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        db.collection("suggestions").add(suggestion);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
