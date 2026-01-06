package com.example.expensetracker2207050.ui.group;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.expensetracker2207050.R;
import com.example.expensetracker2207050.databinding.DialogAddExpenseBinding;
import com.example.expensetracker2207050.databinding.DialogSetBudgetBinding;
import com.example.expensetracker2207050.databinding.FragmentGroupBinding;
import com.example.expensetracker2207050.models.Alert;
import com.example.expensetracker2207050.models.Budget;
import com.example.expensetracker2207050.models.Expense;
import com.example.expensetracker2207050.models.Group;
import com.example.expensetracker2207050.models.Invitation;
import com.example.expensetracker2207050.ui.personal.ExpenseAdapter;
import com.example.expensetracker2207050.viewmodel.GroupViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class GroupFragment extends Fragment {

    private FragmentGroupBinding binding;
    private GroupViewModel viewModel;
    private ExpenseAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private double groupBudgetLimit = 5000.0;
    private boolean alertSentThisSession = false;

    private List<Expense> originalExpenses = new ArrayList<>();
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGroupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GroupViewModel.class);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
        observeViewModel();
        loadGroupBudget();

        binding.btnCreateGroup.setOnClickListener(v -> showCreateGroupDialog());
        binding.btnInvite.setOnClickListener(v -> showInviteDialog());
        binding.fabAddGroupExpense.setOnClickListener(v -> showAddExpenseDialog());

        binding.btnAnalytics.setOnClickListener(v -> {
            Group group = viewModel.getCurrentGroup().getValue();
            if (group != null) {
                Bundle bundle = new Bundle();
                bundle.putString("groupId", group.getId());
                Navigation.findNavController(v).navigate(R.id.action_group_to_analytics, bundle);
            }
        });

        binding.cardGroupInfo.setOnClickListener(v -> {
            Group group = viewModel.getCurrentGroup().getValue();
            if (group != null && group.getAdminId().equals(mAuth.getUid())) {
                showSetBudgetDialog();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter();
        binding.rvGroupExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvGroupExpenses.setAdapter(adapter);
    }


    private void observeViewModel() {
        viewModel.getCurrentGroup().observe(getViewLifecycleOwner(), group -> {
            if (group != null) {
                binding.tvGroupName.setText(group.getName());
                binding.tvMemberCount.setText(String.format(java.util.Locale.getDefault(), "Members: %d", group.getMemberIds().size()));
                binding.llNoGroup.setVisibility(View.GONE);
                binding.cardGroupInfo.setVisibility(View.VISIBLE);
                binding.rvGroupExpenses.setVisibility(View.VISIBLE);
                binding.fabAddGroupExpense.setVisibility(View.VISIBLE);
                binding.llActions.setVisibility(View.VISIBLE);
            } else {
                binding.llNoGroup.setVisibility(View.VISIBLE);
                binding.cardGroupInfo.setVisibility(View.GONE);
                binding.rvGroupExpenses.setVisibility(View.GONE);
                binding.fabAddGroupExpense.setVisibility(View.GONE);
                binding.llActions.setVisibility(View.GONE);
            }
        });

        viewModel.getGroupExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                originalExpenses = expenses;
                adapter.setExpenses(expenses);
                updateGroupBudgetProgress(expenses);
            }
        });
    }

    private void loadGroupBudget() {
        viewModel.getCurrentGroup().observe(getViewLifecycleOwner(), group -> {
            if (group != null) {
                db.collection("budgets").document(group.getId() + "_group")
                        .addSnapshotListener((value, error) -> {
                            if (isAdded() && value != null && value.exists()) {
                                Budget budget = value.toObject(Budget.class);
                                if (budget != null) {
                                    groupBudgetLimit = budget.getAmount();
                                    alertSentThisSession = false;
                                    if (originalExpenses != null) {
                                        updateGroupBudgetProgress(originalExpenses);
                                    }
                                }
                            }
                        });
            }
        });
    }

    private void updateGroupBudgetProgress(java.util.List<Expense> expenses) {
        double total = 0;
        for (Expense e : expenses) total += e.getAmount();
        int progress = (int) ((total / groupBudgetLimit) * 100);
        binding.pbGroupBudget.setProgress(Math.min(progress, 100));
        binding.tvGroupBudgetText.setText(String.format(java.util.Locale.getDefault(), "$%.2f / $%.2f", total, groupBudgetLimit));

        if (total > groupBudgetLimit && !alertSentThisSession) {
            sendGroupBudgetAlert(total);
            alertSentThisSession = true;
        }

        if (total > groupBudgetLimit) {
            binding.pbGroupBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.error)));
        } else {
            binding.pbGroupBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.secondary)));
        }
    }

    private void sendGroupBudgetAlert(double total) {
        Group group = viewModel.getCurrentGroup().getValue();
        if (group == null) return;
        String msg = "Group '" + group.getName() + "' budget exceeded! Total: $" + total;
        for (String memberId : group.getMemberIds()) {
            Alert alert = new Alert(UUID.randomUUID().toString(), memberId, "Group Budget Alert", msg, "BUDGET");
            db.collection("alerts").document(alert.getId()).set(alert);
        }
    }

    private void showCreateGroupDialog() {
        EditText etName = new EditText(getContext());
        etName.setHint("Group Name");
        new AlertDialog.Builder(getContext()).setTitle("Create Group").setView(etName)
                .setPositiveButton("Create", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        String id = UUID.randomUUID().toString();
                        Group group = new Group(id, name, mAuth.getUid());
                        db.collection("groups").document(id).set(group);
                    }
                }).show();
    }

    private void showInviteDialog() {
        Group group = viewModel.getCurrentGroup().getValue();
        if (group == null) return;
        EditText etEmail = new EditText(getContext());
        etEmail.setHint("Member Email");
        new AlertDialog.Builder(getContext()).setTitle("Invite Member").setView(etEmail)
                .setPositiveButton("Invite", (d, w) -> {
                    String email = etEmail.getText().toString().trim();
                    if (!email.isEmpty()) {
                        String id = UUID.randomUUID().toString();
                        Invitation invitation = new Invitation(id, mAuth.getUid(), email, group.getId(), "GROUP");
                        db.collection("invitations").document(id).set(invitation);
                        Toast.makeText(getContext(), "Invitation sent", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void showSetBudgetDialog() {
        DialogSetBudgetBinding b = DialogSetBudgetBinding.inflate(getLayoutInflater());
        b.etBudgetAmount.setText(String.valueOf(groupBudgetLimit));
        new AlertDialog.Builder(getContext()).setView(b.getRoot())
                .setPositiveButton("Save", (d, w) -> {
                    if (b.etBudgetAmount.getText() != null) {
                        String val = b.etBudgetAmount.getText().toString();
                        if (!val.isEmpty()) {
                            double amount = Double.parseDouble(val);
                            Group group = viewModel.getCurrentGroup().getValue();
                            if (group != null) {
                                Budget budget = new Budget(group.getId() + "_group", mAuth.getUid(), group.getId(), amount, "GROUP");
                                db.collection("budgets").document(budget.getId()).set(budget);
                            }
                        }
                    }
                }).show();
    }

    private void showAddExpenseDialog() {
        Group group = viewModel.getCurrentGroup().getValue();
        if (group == null) return;
        DialogAddExpenseBinding dBinding = DialogAddExpenseBinding.inflate(getLayoutInflater());
        String[] cats = {"Food", "Transport", "Shopping", "Bills", "Entertainment", "Other"};
        dBinding.actvCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, cats));
        dBinding.actvCategory.setOnClickListener(v -> dBinding.actvCategory.showDropDown());

        new AlertDialog.Builder(getContext()).setView(dBinding.getRoot())
                .setPositiveButton("Save", (d, w) -> {
                    if (dBinding.etAmount.getText() != null && dBinding.actvCategory.getText() != null) {
                        String amt = dBinding.etAmount.getText().toString();
                        String cat = dBinding.actvCategory.getText().toString();
                        if (!amt.isEmpty() && !cat.isEmpty()) {
                            String desc = dBinding.etDescription.getText() != null ?
                                    dBinding.etDescription.getText().toString() : "";
                            Expense e = new Expense(UUID.randomUUID().toString(), mAuth.getUid(),
                                    Double.parseDouble(amt), cat, new Date(), desc);
                            e.setGroupId(group.getId());
                            e.setContributorId(mAuth.getUid());
                            viewModel.addExpense(e);
                        }
                    }
                }).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
