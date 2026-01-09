package com.example.expensetracker2207050.ui.personal;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
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
import androidx.core.content.ContextCompat;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PersonalFragment extends Fragment {

    private FragmentPersonalBinding binding;
    private PersonalViewModel viewModel;
    private ExpenseAdapter adapter;
    private final String[] categories = {"Food", "Transport", "Shopping", "Bills", "Entertainment", "Other"};
    private final String[] filterCategories = {"All", "Food", "Transport", "Shopping", "Bills", "Entertainment", "Other"};
    private double personalBudgetLimit = 1000.0;
    private boolean alertSentThisSession = false;

    private List<Expense> originalExpenses = new ArrayList<>();
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All";
    private Date filterStartDate = null;
    private Date filterEndDate = null;

    // Parent-child messaging fields
    private User currentUser = null;
    private String linkedParentUid = null;

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
        setupSearchAndFilter();
        loadUserInfo();
        loadBudget();
        observeViewModel();

        // Back button
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        // Add expense FAB
        binding.fabAddExpense.setOnClickListener(v -> showAddExpenseDialog());

        // Set Budget button
        binding.btnSetBudget.setOnClickListener(v -> showSetBudgetDialog());

        // Analytics button
        binding.btnAnalytics.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_personal_to_analytics));

        // Send suggestion button (visible only for child accounts)
        binding.btnSendSuggestion.setOnClickListener(v -> showSendSuggestionToParentDialog());

        // Long press on card to set budget
        binding.cardNetBalance.setOnLongClickListener(v -> {
            showSetBudgetDialog();
            return true;
        });
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter();
        adapter.setCurrentUserId(FirebaseAuth.getInstance().getUid());
        adapter.setOnExpenseClickListener(new ExpenseAdapter.OnExpenseClickListener() {
            @Override
            public void onExpenseClick(Expense expense) {
                showExpenseOptionsDialog(expense);
            }

            @Override
            public void onExpenseLongClick(Expense expense) {
                showExpenseOptionsDialog(expense);
            }
        });
        binding.rvExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvExpenses.setAdapter(adapter);
    }

    private void showExpenseOptionsDialog(Expense expense) {
        new AlertDialog.Builder(getContext())
                .setTitle("Expense Options")
                .setItems(new CharSequence[]{"✏️ Edit", "🗑️ Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditExpenseDialog(expense);
                    } else {
                        showDeleteConfirmDialog(expense);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditExpenseDialog(Expense expense) {
        DialogAddExpenseBinding dialogBinding = DialogAddExpenseBinding.inflate(getLayoutInflater());

        // Pre-fill existing values
        dialogBinding.etAmount.setText(String.valueOf(expense.getAmount()));
        dialogBinding.actvCategory.setText(expense.getCategory());
        dialogBinding.etDescription.setText(expense.getDescription());

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categories);
        dialogBinding.actvCategory.setAdapter(catAdapter);
        dialogBinding.actvCategory.setOnClickListener(v -> dialogBinding.actvCategory.showDropDown());

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Edit Expense")
                .setView(dialogBinding.getRoot())
                .create();

        dialogBinding.btnSave.setOnClickListener(v -> {
            String amountStr = dialogBinding.etAmount.getText() != null ? dialogBinding.etAmount.getText().toString() : "";
            String category = dialogBinding.actvCategory.getText() != null ? dialogBinding.actvCategory.getText().toString() : "";
            String desc = dialogBinding.etDescription.getText() != null ? dialogBinding.etDescription.getText().toString() : "";

            if (amountStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(getContext(), "Amount and category required", Toast.LENGTH_SHORT).show();
                return;
            }

            expense.setAmount(Double.parseDouble(amountStr));
            expense.setCategory(category);
            expense.setDescription(desc);
            viewModel.updateExpense(expense);
            dialog.dismiss();
            Toast.makeText(getContext(), "Expense updated!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showDeleteConfirmDialog(Expense expense) {
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
                        case 0: // All Time
                            filterStartDate = null;
                            filterEndDate = null;
                            binding.btnFilterDate.setText("Date");
                            break;
                        case 1: // Today
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            cal.set(Calendar.MINUTE, 0);
                            cal.set(Calendar.SECOND, 0);
                            filterStartDate = cal.getTime();
                            filterEndDate = new Date();
                            binding.btnFilterDate.setText("Today");
                            break;
                        case 2: // This Week
                            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            cal.set(Calendar.MINUTE, 0);
                            filterStartDate = cal.getTime();
                            filterEndDate = new Date();
                            binding.btnFilterDate.setText("Week");
                            break;
                        case 3: // This Month
                            cal.set(Calendar.DAY_OF_MONTH, 1);
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            cal.set(Calendar.MINUTE, 0);
                            filterStartDate = cal.getTime();
                            filterEndDate = new Date();
                            binding.btnFilterDate.setText("Month");
                            break;
                        case 4: // Custom Range
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
            // Search filter
            if (!currentSearchQuery.isEmpty()) {
                boolean matchesSearch = expense.getCategory().toLowerCase().contains(currentSearchQuery) ||
                        (expense.getDescription() != null && expense.getDescription().toLowerCase().contains(currentSearchQuery));
                if (!matchesSearch) continue;
            }

            // Category filter
            if (!currentCategoryFilter.equals("All") && !expense.getCategory().equals(currentCategoryFilter)) {
                continue;
            }

            // Date filter
            if (filterStartDate != null && expense.getDate() != null) {
                if (expense.getDate().before(filterStartDate)) continue;
            }
            if (filterEndDate != null && expense.getDate() != null) {
                if (expense.getDate().after(filterEndDate)) continue;
            }

            filtered.add(expense);
        }

        adapter.setExpenses(filtered);
        binding.tvExpenseCount.setText(filtered.size() + " items");
    }

    private void loadUserInfo() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isAdded() && binding != null && snapshot.exists()) {
                        User user = snapshot.toObject(User.class);
                        if (user != null) {
                            currentUser = user;
                            if (user.getUsername() != null) {
                                binding.tvUserGreet.setText("Welcome, " + user.getUsername());
                            }
                            // Check if user has a linked parent
                            linkedParentUid = user.getLinkedParentUid();
                            if (linkedParentUid != null && !linkedParentUid.isEmpty()) {
                                binding.btnSendSuggestion.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
    }

    private void showSendSuggestionToParentDialog() {
        if (linkedParentUid == null || linkedParentUid.isEmpty()) {
            Toast.makeText(getContext(), "You are not linked to a parent account", Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.EditText etMessage = new android.widget.EditText(getContext());
        etMessage.setHint("Type your message here...");
        etMessage.setMinLines(3);
        etMessage.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(getContext())
                .setTitle("Send Message to Parent")
                .setView(etMessage)
                .setPositiveButton("Send", (d, w) -> {
                    String message = etMessage.getText().toString().trim();
                    if (!message.isEmpty()) {
                        sendMessageToParent("Message from Child", message, "MESSAGE");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendMessageToParent(String title, String message, String type) {
        if (linkedParentUid == null) return;

        String childName = (currentUser != null && currentUser.getUsername() != null)
            ? currentUser.getUsername() : "Your child";

        // Create alert for parent
        Alert alert = new Alert(
            UUID.randomUUID().toString(),
            linkedParentUid,
            title + " from " + childName,
            message,
            type
        );

        FirebaseFirestore.getInstance().collection("alerts")
            .document(alert.getId())
            .set(alert)
            .addOnSuccessListener(aVoid -> {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Message sent to parent!", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        // Also persist in messages collection for history
        Map<String, Object> msgData = new HashMap<>();
        msgData.put("senderId", FirebaseAuth.getInstance().getUid());
        msgData.put("receiverId", linkedParentUid);
        msgData.put("senderName", childName);
        msgData.put("title", title);
        msgData.put("message", message);
        msgData.put("type", type);
        msgData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        msgData.put("direction", "CHILD_TO_PARENT");

        FirebaseFirestore.getInstance().collection("parent_child_messages").add(msgData);
    }

    private void loadBudget() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("budgets")
                .document(uid + "_personal")
                .addSnapshotListener((value, error) -> {
                    if (isAdded() && binding != null && value != null && value.exists()) {
                        Budget budget = value.toObject(Budget.class);
                        if (budget != null) {
                            personalBudgetLimit = budget.getAmount();
                            binding.tvBudgetAmount.setText(String.format(Locale.getDefault(), "৳%.2f", personalBudgetLimit));
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

    private void updateBudgetProgress(List<Expense> expenses) {
        double total = 0;
        for (Expense e : expenses) {
            total += e.getAmount();
        }

        // Update total expenses
        binding.tvNetBalance.setText(String.format(Locale.getDefault(), "৳%.2f", total));

        // Update total budget
        binding.tvBudgetAmount.setText(String.format(Locale.getDefault(), "৳%.2f", personalBudgetLimit));

        // Calculate remaining budget
        double remaining = personalBudgetLimit - total;

        // Update budget progress
        int progress = (int) ((total / personalBudgetLimit) * 100);
        binding.pbBudget.setProgress(Math.min(progress, 100));

        // Update remaining display
        if (remaining >= 0) {
            binding.tvBudgetStatus.setText(String.format(Locale.getDefault(), "৳%.2f", remaining));
            binding.tvBudgetStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        } else {
            binding.tvBudgetStatus.setText(String.format(Locale.getDefault(), "-৳%.2f", Math.abs(remaining)));
            binding.tvBudgetStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        }

        // Update percentage text
        binding.tvBudgetPercent.setText(String.format(Locale.getDefault(), "%d%% of budget used", Math.min(progress, 100)));

        // Change progress bar color based on usage
        if (progress > 100) {
            binding.pbBudget.setProgressTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.expense_red)));
            binding.tvBudgetPercent.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        } else if (progress > 80) {
            binding.pbBudget.setProgressTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.secondary)));
            binding.tvBudgetPercent.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary));
        } else {
            binding.pbBudget.setProgressTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.income_green)));
            binding.tvBudgetPercent.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }

        if (total > personalBudgetLimit && !alertSentThisSession) {
            sendBudgetAlert(total, personalBudgetLimit);
            alertSentThisSession = true;
        }
    }

    private void sendBudgetAlert(double total, double limit) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        String message = String.format(Locale.getDefault(), "Personal budget exceeded! Total: ৳%.2f, Limit: ৳%.2f", total, limit);

        Alert alert = new Alert(UUID.randomUUID().toString(), uid, "Budget Warning", message, "BUDGET");
        FirebaseFirestore.getInstance().collection("alerts").document(alert.getId()).set(alert);

        FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(snapshot -> {
            if (isAdded()) {
                User user = snapshot.toObject(User.class);
                if (user != null && user.getLinkedParentUid() != null && user.getUsername() != null) {
                    String parentMsg = String.format(Locale.getDefault(), "Your child %s has exceeded their personal budget: ৳%.2f", user.getUsername(), total);
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
            String val = budgetBinding.etBudgetAmount.getText() != null ? budgetBinding.etBudgetAmount.getText().toString() : "";
            if (!val.isEmpty()) {
                double newLimit = Double.parseDouble(val);
                saveBudgetToFirestore(newLimit);
                dialog.dismiss();
                Toast.makeText(getContext(), "Budget updated!", Toast.LENGTH_SHORT).show();
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
            String amountStr = dialogBinding.etAmount.getText() != null ? dialogBinding.etAmount.getText().toString() : "";
            String category = dialogBinding.actvCategory.getText() != null ? dialogBinding.actvCategory.getText().toString() : "";
            String desc = dialogBinding.etDescription.getText() != null ? dialogBinding.etDescription.getText().toString() : "";

            if (amountStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(getContext(), "Amount and category required", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            Expense expense = new Expense(UUID.randomUUID().toString(), FirebaseAuth.getInstance().getUid(), amount, category, new Date(), desc);
            viewModel.addExpense(expense);
            dialog.dismiss();
            Toast.makeText(getContext(), "Expense added!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

