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
        adapter.setOnMessageClickListener(this::showSendMessageDialog);
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
                .setItems(new CharSequence[]{
                    "📊 View Expenses & Analytics",
                    "💬 Send Message",
                    "💡 Send Suggestion",
                    "💰 Set Child's Budget",
                    "📋 View Message History"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Navigation.findNavController(requireView()).navigate(R.id.action_parent_to_childExpenses, bundle);
                            break;
                        case 1:
                            showSendMessageDialog(child);
                            break;
                        case 2:
                            showSendSuggestionDialog(child);
                            break;
                        case 3:
                            showSetChildBudgetDialog(child);
                            break;
                        case 4:
                            showMessageHistoryDialog(child);
                            break;
                    }
                })
                .show();
    }

    private void showSendMessageDialog(User child) {
        android.widget.EditText etMessage = new android.widget.EditText(getContext());
        etMessage.setHint("Type your message here...");
        etMessage.setMinLines(3);
        etMessage.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(getContext())
                .setTitle("Send Message to " + child.getUsername())
                .setView(etMessage)
                .setPositiveButton("Send", (d, w) -> {
                    String message = etMessage.getText().toString().trim();
                    if (!message.isEmpty()) {
                        sendMessageToChild(child, "Message from Parent", message, "MESSAGE");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendMessageToChild(User child, String title, String message, String type) {
        // Create alert for child
        Alert alert = new Alert(UUID.randomUUID().toString(), child.getUid(), title, message, type);
        db.collection("alerts").document(alert.getId()).set(alert)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Message sent to " + child.getUsername(), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        // Persist in messages collection for history
        java.util.Map<String, Object> msgData = new java.util.HashMap<>();
        msgData.put("senderId", mAuth.getUid());
        msgData.put("receiverId", child.getUid());
        msgData.put("receiverName", child.getUsername());
        msgData.put("title", title);
        msgData.put("message", message);
        msgData.put("type", type);
        msgData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        msgData.put("direction", "PARENT_TO_CHILD");

        db.collection("parent_child_messages").add(msgData);
    }

    private void showSetChildBudgetDialog(User child) {
        android.widget.EditText etBudget = new android.widget.EditText(getContext());
        etBudget.setHint("Budget Amount (৳)");
        etBudget.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etBudget.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(getContext())
                .setTitle("Set Budget for " + child.getUsername())
                .setView(etBudget)
                .setPositiveButton("Set", (d, w) -> {
                    String amountStr = etBudget.getText().toString().trim();
                    if (!amountStr.isEmpty()) {
                        double amount = Double.parseDouble(amountStr);
                        setChildBudget(child, amount);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setChildBudget(User child, double amount) {
        com.example.expensetracker2207050.models.Budget budget =
            new com.example.expensetracker2207050.models.Budget(
                child.getUid() + "_personal",
                child.getUid(),
                null,
                amount,
                "PERSONAL"
            );

        db.collection("budgets").document(budget.getId()).set(budget)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Budget set for " + child.getUsername(), Toast.LENGTH_SHORT).show();
                    // Notify child about budget update
                    String message = String.format(java.util.Locale.getDefault(),
                        "Your parent has set your budget to ৳%.2f", amount);
                    Alert alert = new Alert(UUID.randomUUID().toString(), child.getUid(),
                        "Budget Updated", message, "BUDGET_UPDATE");
                    db.collection("alerts").document(alert.getId()).set(alert);
                })
                .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to set budget", Toast.LENGTH_SHORT).show());
    }

    private void showMessageHistoryDialog(User child) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
        progressDialog.setMessage("Loading messages...");
        progressDialog.show();

        db.collection("parent_child_messages")
                .whereEqualTo("senderId", mAuth.getUid())
                .whereEqualTo("receiverId", child.getUid())
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(sentDocs -> {
                    db.collection("parent_child_messages")
                            .whereEqualTo("senderId", child.getUid())
                            .whereEqualTo("receiverId", mAuth.getUid())
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(20)
                            .get()
                            .addOnSuccessListener(receivedDocs -> {
                                progressDialog.dismiss();

                                java.util.List<String> messageList = new java.util.ArrayList<>();
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault());

                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : sentDocs) {
                                    String title = doc.getString("title");
                                    String msg = doc.getString("message");
                                    com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                                    String time = ts != null ? sdf.format(ts.toDate()) : "";
                                    messageList.add("📤 You → " + child.getUsername() + " (" + time + ")\n" + title + ": " + msg);
                                }

                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : receivedDocs) {
                                    String title = doc.getString("title");
                                    String msg = doc.getString("message");
                                    com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                                    String time = ts != null ? sdf.format(ts.toDate()) : "";
                                    messageList.add("📥 " + child.getUsername() + " → You (" + time + ")\n" + title + ": " + msg);
                                }

                                if (messageList.isEmpty()) {
                                    Toast.makeText(getContext(), "No messages yet", Toast.LENGTH_SHORT).show();
                                } else {
                                    new AlertDialog.Builder(getContext())
                                            .setTitle("Message History with " + child.getUsername())
                                            .setItems(messageList.toArray(new String[0]), null)
                                            .setPositiveButton("Close", null)
                                            .show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "Failed to load messages", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to load messages", Toast.LENGTH_SHORT).show();
                });
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
