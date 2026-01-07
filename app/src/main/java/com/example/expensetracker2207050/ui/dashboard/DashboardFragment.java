package com.example.expensetracker2207050.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.expensetracker2207050.R;
import com.example.expensetracker2207050.databinding.FragmentDashboardBinding;
import com.example.expensetracker2207050.models.Budget;
import com.example.expensetracker2207050.models.Expense;
import com.example.expensetracker2207050.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private double totalBudget = 0.0;
    private double totalExpenses = 0.0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().finishAffinity();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserInfo();
        loadBudgetAndExpenses();

        binding.cardModeSelection.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_dashboard_to_modeSelection));

        binding.cardAlerts.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_dashboard_to_alerts));

        binding.cardLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_login);
        });
    }

    private void loadUserInfo() {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded() && binding != null) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getUsername() != null) {
                            String welcomeText = "Welcome back, " + user.getUsername();
                            binding.tvWelcome.setText(welcomeText);
                        }
                    }
                });
    }

    private void loadBudgetAndExpenses() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        // Load personal budget
        db.collection("budgets").document(uid + "_personal")
                .addSnapshotListener((value, error) -> {
                    if (isAdded() && binding != null && value != null && value.exists()) {
                        Budget budget = value.toObject(Budget.class);
                        if (budget != null) {
                            totalBudget = budget.getAmount();
                            updateDashboardDisplay();
                        }
                    }
                });

        // Load personal expenses (not group expenses)
        db.collection("expenses")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((value, error) -> {
                    if (isAdded() && binding != null && value != null) {
                        totalExpenses = 0;
                        for (QueryDocumentSnapshot doc : value) {
                            Expense expense = doc.toObject(Expense.class);
                            // Only count personal expenses (not group expenses)
                            if (expense.getGroupId() == null) {
                                totalExpenses += expense.getAmount();
                            }
                        }
                        updateDashboardDisplay();
                    }
                });
    }

    private void updateDashboardDisplay() {
        if (binding == null) return;

        // Update Total Budget
        binding.tvNetBalance.setText(String.format(Locale.getDefault(), "৳%.2f", totalBudget));

        // Update Total Expenses
        binding.tvExpenses.setText(String.format(Locale.getDefault(), "৳%.2f", totalExpenses));

        // Calculate and update Remaining
        double remaining = totalBudget - totalExpenses;

        if (remaining >= 0) {
            binding.tvRemaining.setText(String.format(Locale.getDefault(), "৳%.2f", remaining));
            binding.tvRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        } else {
            binding.tvRemaining.setText(String.format(Locale.getDefault(), "-৳%.2f", Math.abs(remaining)));
            binding.tvRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        }

        // Update budget status text
        if (totalBudget == 0) {
            binding.tvBudgetStatus.setText("Set your budget in Personal Mode");
        } else if (remaining < 0) {
            binding.tvBudgetStatus.setText("⚠️ You've exceeded your budget!");
            binding.tvBudgetStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        } else if (remaining < totalBudget * 0.2) {
            binding.tvBudgetStatus.setText("⚠️ Budget running low!");
            binding.tvBudgetStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary));
        } else {
            binding.tvBudgetStatus.setText("✓ Healthy tracking status");
            binding.tvBudgetStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

