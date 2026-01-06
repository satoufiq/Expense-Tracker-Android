package com.example.expensetracker2207050.ui.parent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.expensetracker2207050.databinding.FragmentChildExpensesBinding;
import com.example.expensetracker2207050.models.Expense;
import com.example.expensetracker2207050.ui.personal.ExpenseAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChildExpensesFragment extends Fragment {

    private FragmentChildExpensesBinding binding;
    private FirebaseFirestore db;
    private ExpenseAdapter adapter;
    private String childUid;
    private String childName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            childUid = getArguments().getString("childUid");
            childName = getArguments().getString("childName");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChildExpensesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        binding.tvChildName.setText("Viewing: " + childName);

        setupRecyclerView();
        loadChildExpenses();
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter();
        binding.rvChildExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvChildExpenses.setAdapter(adapter);
    }

    private void loadChildExpenses() {
        if (childUid == null) return;

        db.collection("expenses")
                .whereEqualTo("userId", childUid)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<Expense> allList = value.toObjects(Expense.class);
                        List<Expense> personalList = new ArrayList<>();
                        for (Expense e : allList) {
                            if (e.getGroupId() == null || e.getGroupId().isEmpty()) {
                                personalList.add(e);
                            }
                        }
                        Collections.sort(personalList, (e1, e2) -> {
                            if (e1.getDate() == null || e2.getDate() == null) return 0;
                            return e2.getDate().compareTo(e1.getDate());
                        });
                        adapter.setExpenses(personalList);
                        calculateSummary(personalList);
                    }
                });
    }

    private void calculateSummary(List<Expense> expenses) {
        double total = 0;
        Map<String, Double> categoryTotals = new HashMap<>();

        for (Expense e : expenses) {
            total += e.getAmount();
            categoryTotals.put(e.getCategory(), categoryTotals.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
        }

        binding.tvTotalSpent.setText(String.format(Locale.getDefault(), "Total Spent: $%.2f", total));

        String topCategory = "N/A";
        double maxAmount = -1;
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue() > maxAmount) {
                maxAmount = entry.getValue();
                topCategory = entry.getKey();
            }
        }
        binding.tvTopCategory.setText("Top Category: " + topCategory);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
