package com.example.expensetracker2207050.ui.group;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    private Date filterStartDate = null;
    private Date filterEndDate = null;
    private final String[] filterCategories = {"All", "Food", "Transport", "Shopping", "Bills", "Entertainment", "Other"};

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
        setupSearchAndFilter();
        observeViewModel();
        loadGroupBudget();

        // Back button
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        binding.btnCreateGroup.setOnClickListener(v -> showCreateGroupDialog());
        binding.btnInvite.setOnClickListener(v -> {
            Group group = viewModel.getCurrentGroup().getValue();
            if (group != null && group.isAdmin(mAuth.getUid())) {
                showInviteDialog();
            } else {
                Toast.makeText(getContext(), "Only group admins can invite members", Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnSetBudget.setOnClickListener(v -> {
            Group group = viewModel.getCurrentGroup().getValue();
            if (group != null && group.isAdmin(mAuth.getUid())) {
                showSetBudgetDialog();
            } else {
                Toast.makeText(getContext(), "Only group admins can set budget", Toast.LENGTH_SHORT).show();
            }
        });
        binding.fabAddGroupExpense.setOnClickListener(v -> showAddExpenseDialog());

        binding.btnViewMembers.setOnClickListener(v -> {
            Group group = viewModel.getCurrentGroup().getValue();
            if (group != null) {
                Bundle bundle = new Bundle();
                bundle.putString("groupId", group.getId());
                bundle.putString("groupName", group.getName());
                Navigation.findNavController(v).navigate(R.id.action_group_to_members, bundle);
            }
        });

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
            if (group != null && group.isAdmin(mAuth.getUid())) {
                showSetBudgetDialog();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter();
        adapter.setCurrentUserId(mAuth.getUid());
        adapter.setOnExpenseClickListener(new ExpenseAdapter.OnExpenseClickListener() {
            @Override
            public void onExpenseClick(Expense expense) {
                showGroupExpenseOptionsDialog(expense);
            }

            @Override
            public void onExpenseLongClick(Expense expense) {
                showGroupExpenseOptionsDialog(expense);
            }
        });
        binding.rvGroupExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvGroupExpenses.setAdapter(adapter);
    }

    private void showGroupExpenseOptionsDialog(Expense expense) {
        String currentUid = mAuth.getUid();
        String contributorId = expense.getContributorId() != null ? expense.getContributorId() : expense.getUserId();

        // Only allow edit/delete if the current user is the one who created this expense
        if (!currentUid.equals(contributorId)) {
            Toast.makeText(getContext(), "You can only edit or delete your own expenses", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Expense Options")
                .setItems(new CharSequence[]{"✏️ Edit", "🗑️ Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditGroupExpenseDialog(expense);
                    } else {
                        showDeleteGroupExpenseConfirmDialog(expense);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditGroupExpenseDialog(Expense expense) {
        DialogAddExpenseBinding dBinding = DialogAddExpenseBinding.inflate(getLayoutInflater());
        String[] cats = {"Food", "Transport", "Shopping", "Bills", "Entertainment", "Other"};

        // Pre-fill existing values
        dBinding.etAmount.setText(String.valueOf(expense.getAmount()));
        dBinding.actvCategory.setText(expense.getCategory());
        dBinding.etDescription.setText(expense.getDescription());

        dBinding.actvCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, cats));
        dBinding.actvCategory.setOnClickListener(v -> dBinding.actvCategory.showDropDown());

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Edit Group Expense")
                .setView(dBinding.getRoot())
                .create();

        dBinding.btnSave.setOnClickListener(v -> {
            if (dBinding.etAmount.getText() != null && dBinding.actvCategory.getText() != null) {
                String amt = dBinding.etAmount.getText().toString();
                String cat = dBinding.actvCategory.getText().toString();
                if (!amt.isEmpty() && !cat.isEmpty()) {
                    String desc = dBinding.etDescription.getText() != null ?
                            dBinding.etDescription.getText().toString() : "";
                    expense.setAmount(Double.parseDouble(amt));
                    expense.setCategory(cat);
                    expense.setDescription(desc);
                    viewModel.updateExpense(expense);
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Expense updated!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void showDeleteGroupExpenseConfirmDialog(Expense expense) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete", (d, w) -> {
                    viewModel.deleteExpense(expense.getId());
                    Toast.makeText(getContext(), "Expense deleted!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupSearchAndFilter() {
        // Search text watcher
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Category filter dropdown
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, filterCategories);
        binding.actvFilterCategory.setAdapter(categoryAdapter);
        binding.actvFilterCategory.setOnItemClickListener((parent, v, position, id) -> {
            currentCategoryFilter = filterCategories[position];
            applyFilters();
        });

        // Date filter button
        binding.btnFilterDate.setOnClickListener(v -> showDateFilterDialog());
    }

    private void showDateFilterDialog() {
        String[] options = {"All Time", "Today", "This Week", "This Month", "Custom Range"};
        new AlertDialog.Builder(getContext())
                .setTitle("Filter by Date")
                .setItems(options, (dialog, which) -> {
                    Calendar cal = Calendar.getInstance();
                    switch (which) {
                        case 0:
                            filterStartDate = null;
                            filterEndDate = null;
                            binding.btnFilterDate.setText("Date");
                            break;
                        case 1:
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            cal.set(Calendar.MINUTE, 0);
                            cal.set(Calendar.SECOND, 0);
                            filterStartDate = cal.getTime();
                            filterEndDate = new Date();
                            binding.btnFilterDate.setText("Today");
                            break;
                        case 2:
                            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            cal.set(Calendar.MINUTE, 0);
                            filterStartDate = cal.getTime();
                            filterEndDate = new Date();
                            binding.btnFilterDate.setText("Week");
                            break;
                        case 3:
                            cal.set(Calendar.DAY_OF_MONTH, 1);
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            cal.set(Calendar.MINUTE, 0);
                            filterStartDate = cal.getTime();
                            filterEndDate = new Date();
                            binding.btnFilterDate.setText("Month");
                            break;
                        case 4:
                            showCustomDatePicker();
                            return;
                    }
                    applyFilters();
                })
                .show();
    }

    private void showCustomDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog startPicker = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar startCal = Calendar.getInstance();
            startCal.set(year, month, dayOfMonth, 0, 0, 0);
            filterStartDate = startCal.getTime();

            DatePickerDialog endPicker = new DatePickerDialog(requireContext(), (v2, y2, m2, d2) -> {
                Calendar endCal = Calendar.getInstance();
                endCal.set(y2, m2, d2, 23, 59, 59);
                filterEndDate = endCal.getTime();
                binding.btnFilterDate.setText("Custom");
                applyFilters();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            endPicker.setTitle("Select End Date");
            endPicker.show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        startPicker.setTitle("Select Start Date");
        startPicker.show();
    }

    private void applyFilters() {
        if (originalExpenses == null) return;

        List<Expense> filtered = new ArrayList<>();
        for (Expense expense : originalExpenses) {
            if (!currentSearchQuery.isEmpty()) {
                boolean matchesSearch = expense.getCategory().toLowerCase().contains(currentSearchQuery) ||
                        (expense.getDescription() != null && expense.getDescription().toLowerCase().contains(currentSearchQuery));
                if (!matchesSearch) continue;
            }

            if (!currentCategoryFilter.equals("All") && !expense.getCategory().equals(currentCategoryFilter)) {
                continue;
            }

            if (filterStartDate != null && expense.getDate() != null) {
                if (expense.getDate().before(filterStartDate)) continue;
            }
            if (filterEndDate != null && expense.getDate() != null) {
                if (expense.getDate().after(filterEndDate)) continue;
            }

            filtered.add(expense);
        }

        adapter.setExpenses(filtered);
    }

    private void observeViewModel() {
        viewModel.getCurrentGroup().observe(getViewLifecycleOwner(), group -> {
            if (group != null) {
                binding.tvGroupName.setText(group.getName());
                binding.tvMemberCount.setText(String.format(Locale.getDefault(), "Members: %d", group.getMemberIds().size()));
                binding.llNoGroup.setVisibility(View.GONE);
                binding.cardGroupInfo.setVisibility(View.VISIBLE);
                binding.cardSearch.setVisibility(View.VISIBLE);
                binding.rvGroupExpenses.setVisibility(View.VISIBLE);
                binding.fabAddGroupExpense.setVisibility(View.VISIBLE);
                binding.llActions.setVisibility(View.VISIBLE);

                // Only show invite and set budget buttons for admin
                boolean isAdmin = group.isAdmin(mAuth.getUid());
                binding.btnInvite.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                binding.btnSetBudget.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            } else {
                binding.llNoGroup.setVisibility(View.VISIBLE);
                binding.cardGroupInfo.setVisibility(View.GONE);
                binding.cardSearch.setVisibility(View.GONE);
                binding.rvGroupExpenses.setVisibility(View.GONE);
                binding.fabAddGroupExpense.setVisibility(View.GONE);
                binding.llActions.setVisibility(View.GONE);
            }
        });

        viewModel.getGroupExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                originalExpenses = expenses;
                applyFilters();
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

    private void updateGroupBudgetProgress(List<Expense> expenses) {
        double total = 0;
        for (Expense e : expenses) total += e.getAmount();
        int progress = (int) ((total / groupBudgetLimit) * 100);
        binding.pbGroupBudget.setProgress(Math.min(progress, 100));
        binding.tvGroupBudgetText.setText(String.format(Locale.getDefault(), "৳%.2f / ৳%.2f", total, groupBudgetLimit));

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
        String msg = String.format(Locale.getDefault(), "Group '%s' budget exceeded! Total: ৳%.2f", group.getName(), total);
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
