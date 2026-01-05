package com.example.expensetracker2207050.ui.personal;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.example.expensetracker2207050.databinding.FragmentPersonalBinding;
import com.example.expensetracker2207050.models.Alert;
import com.example.expensetracker2207050.models.Budget;
import com.example.expensetracker2207050.models.Expense;
import com.example.expensetracker2207050.models.User;
import com.example.expensetracker2207050.viewmodel.PersonalViewModel;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PersonalFragment extends Fragment {

    private FragmentPersonalBinding binding;
    private PersonalViewModel viewModel;
    private ExpenseAdapter adapter;
    private final String[] categories = {"Food", "Transport", "Shopping", "Bills", "Entertainment", "Other"};
    private double personalBudgetLimit = 1000.0;
    private boolean alertSentThisSession = false;

    private List<Expense> originalExpenses = new ArrayList<>();
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPersonalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PersonalViewModel.class);

        setupRecyclerView();
        setupFilters();
        loadBudget();
        observeViewModel();

        binding.fabAddExpense.setOnClickListener(v -> showAddExpenseDialog());
        binding.btnAnalytics.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_personal_to_analytics));
        binding.cardBudget.setOnClickListener(v -> showSetBudgetDialog());
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter();
        binding.rvExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvExpenses.setAdapter(adapter);
    }

    private void setupFilters() {
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

        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                currentCategoryFilter = chip.getText().toString();
                applyFilters();
            }
        });
    }

    private void applyFilters() {
        List<Expense> filteredList = new ArrayList<>();
        for (Expense expense : originalExpenses) {
            String desc = expense.getDescription() != null ? expense.getDescription().toLowerCase() : "";
            String cat = expense.getCategory() != null ? expense.getCategory().toLowerCase() : "";

            boolean matchesSearch = desc.contains(currentSearchQuery) || cat.contains(currentSearchQuery);
            boolean matchesCategory = currentCategoryFilter.equals("All") ||
                    (expense.getCategory() != null && expense.getCategory().equalsIgnoreCase(currentCategoryFilter));

            if (matchesSearch && matchesCategory) {
                filteredList.add(expense);
            }
        }
        adapter.setExpenses(filteredList);
        binding.tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadBudget() {
        if (FirebaseAuth.getInstance().getUid() == null) return;
        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("budgets")
                .document(uid + "_personal")
                .addSnapshotListener((value, error) -> {
                    if (isAdded() && value != null && value.exists()) {
                        Budget budget = value.toObject(Budget.class);
                        if (budget != null) {
                            personalBudgetLimit = budget.getAmount();
                            alertSentThisSession = false;
                            if (originalExpenses != null) {
                                updateBudgetProgress(originalExpenses);
                            }
                        }
                    }
                });
    }

    private void observeViewModel() {
        viewModel.getPersonalExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                originalExpenses = expenses;
                applyFilters();
                updateBudgetProgress(expenses);
            }
        });
    }

    private void updateBudgetProgress(java.util.List<Expense> expenses) {
        double total = 0;
        for (Expense e : expenses) {
            total += e.getAmount();
        }
        int progress = (int) ((total / personalBudgetLimit) * 100);
        binding.pbBudget.setProgress(Math.min(progress, 100));
        binding.tvBudgetText.setText(String.format("$%.2f / $%.2f", total, personalBudgetLimit));

        if (total > personalBudgetLimit) {
            binding.pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.error)));
            if (!alertSentThisSession) {
                sendBudgetAlert(total, personalBudgetLimit);
                alertSentThisSession = true;
            }
        } else {
            binding.pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
        }
    }

    private void sendBudgetAlert(double total, double limit) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        String message = String.format("Personal budget exceeded! Total: $%.2f, Limit: $%.2f", total, limit);

        Alert alert = new Alert(UUID.randomUUID().toString(), uid, "Budget Warning", message, "BUDGET");
        FirebaseFirestore.getInstance().collection("alerts").document(alert.getId()).set(alert);

        FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(snapshot -> {
            if (isAdded()) {
                User user = snapshot.toObject(User.class);
                if (user != null && user.getLinkedParentUid() != null) {
                    String parentMsg = String.format("Your child %s has exceeded their personal budget: $%.2f", user.getUsername(), total);
                    Alert parentAlert = new Alert(UUID.randomUUID().toString(), user.getLinkedParentUid(), "Child Budget Alert", parentMsg, "BUDGET");
                    FirebaseFirestore.getInstance().collection("alerts").document(parentAlert.getId()).set(parentAlert);
                }
            }
        });
    }

    private void showSetBudgetDialog() {
        DialogSetBudgetBinding budgetBinding = DialogSetBudgetBinding.inflate(getLayoutInflater());
        budgetBinding.etBudgetAmount.setText(String.valueOf(personalBudgetLimit));

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(budgetBinding.getRoot())
                .create();

        budgetBinding.btnSaveBudget.setOnClickListener(v -> {
            String val = budgetBinding.etBudgetAmount.getText().toString();
            if (!val.isEmpty()) {
                double newLimit = Double.parseDouble(val);
                saveBudgetToFirestore(newLimit);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void saveBudgetToFirestore(double amount) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        Budget budget = new Budget(uid + "_personal", uid, null, amount, "PERSONAL");
        FirebaseFirestore.getInstance().collection("budgets").document(budget.getId()).set(budget);
    }

    private void showAddExpenseDialog() {
        DialogAddExpenseBinding dialogBinding = DialogAddExpenseBinding.inflate(getLayoutInflater());

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categories);
        dialogBinding.actvCategory.setAdapter(catAdapter);
        dialogBinding.actvCategory.setOnClickListener(v -> dialogBinding.actvCategory.showDropDown());

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogBinding.getRoot())
                .create();

        dialogBinding.btnSave.setOnClickListener(v -> {
            String amountStr = dialogBinding.etAmount.getText().toString();
            String category = dialogBinding.actvCategory.getText().toString();
            String desc = dialogBinding.etDescription.getText().toString();

            if (amountStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(getContext(), "Fields required", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            Expense expense = new Expense(UUID.randomUUID().toString(), FirebaseAuth.getInstance().getUid(), amount, category, new Date(), desc);
            viewModel.addExpense(expense);
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
