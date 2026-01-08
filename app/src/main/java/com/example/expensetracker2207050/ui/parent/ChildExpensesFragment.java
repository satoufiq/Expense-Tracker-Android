package com.example.expensetracker2207050.ui.parent;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.expensetracker2207050.R;
import com.example.expensetracker2207050.databinding.FragmentChildExpensesBinding;
import com.example.expensetracker2207050.models.Budget;
import com.example.expensetracker2207050.models.Expense;
import com.example.expensetracker2207050.ui.personal.ExpenseAdapter;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
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

    private List<Expense> allExpenses = new ArrayList<>();
    private List<Expense> filteredExpenses = new ArrayList<>();

    private String currentSearchQuery = "";
    private String currentCategoryFilter = "All";
    private String currentDateFilter = "This Month";
    private boolean sortNewestFirst = true;

    private double childBudget = 0.0;

    private final int[] chartColors = {
            Color.parseColor("#7C3AED"),
            Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#3B82F6"),
            Color.parseColor("#EC4899")
    };

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
        setupCharts();
        setupSearchAndFilters();
        loadChildBudget();
        loadChildExpenses();

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter();
        binding.rvChildExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvChildExpenses.setAdapter(adapter);
    }

    private void setupCharts() {
        // Setup Pie Chart
        PieChart pieChart = binding.pieChart;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Categories");
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.setCenterTextSize(14f);
        pieChart.setRotationEnabled(true);
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setTextColor(Color.WHITE);
        pieChart.getLegend().setTextSize(10f);
        pieChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        pieChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(10f);

        // Setup Bar Chart
        BarChart barChart = binding.barChart;
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setFitBars(true);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setTextColor(Color.WHITE);
        barChart.getXAxis().setTextSize(10f);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setGranularity(1f);
        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisLeft().setGridColor(Color.parseColor("#333333"));
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.animateY(1000);
    }

    private void setupSearchAndFilters() {
        // Search functionality
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Category filter chips
        binding.chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                binding.chipAll.setChecked(true);
                return;
            }

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipAll) {
                currentCategoryFilter = "All";
            } else if (checkedId == R.id.chipFood) {
                currentCategoryFilter = "Food";
            } else if (checkedId == R.id.chipTransport) {
                currentCategoryFilter = "Transport";
            } else if (checkedId == R.id.chipShopping) {
                currentCategoryFilter = "Shopping";
            } else if (checkedId == R.id.chipEntertainment) {
                currentCategoryFilter = "Entertainment";
            } else if (checkedId == R.id.chipBills) {
                currentCategoryFilter = "Bills";
            } else if (checkedId == R.id.chipOther) {
                currentCategoryFilter = "Other";
            }
            applyFilters();
        });

        // Date filter button
        binding.btnDateFilter.setOnClickListener(v -> showDateFilterMenu());

        // Sort order button
        binding.btnSortOrder.setOnClickListener(v -> {
            sortNewestFirst = !sortNewestFirst;
            binding.btnSortOrder.setText(sortNewestFirst ? "Newest First" : "Oldest First");
            applyFilters();
        });
    }

    private void showDateFilterMenu() {
        PopupMenu popup = new PopupMenu(requireContext(), binding.btnDateFilter);
        popup.getMenu().add("All Time");
        popup.getMenu().add("Today");
        popup.getMenu().add("This Week");
        popup.getMenu().add("This Month");
        popup.getMenu().add("This Year");
        popup.getMenu().add("Custom Range...");

        popup.setOnMenuItemClickListener(item -> {
            String selection = item.getTitle().toString();
            if (selection.equals("Custom Range...")) {
                showCustomDateRangePicker();
            } else {
                currentDateFilter = selection;
                binding.btnDateFilter.setText(selection);
                applyFilters();
            }
            return true;
        });
        popup.show();
    }

    private void showCustomDateRangePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog startPicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String startDate = String.format(Locale.getDefault(), "%d/%d/%d", month + 1, dayOfMonth, year);
                    currentDateFilter = "Custom:" + startDate;
                    binding.btnDateFilter.setText("Custom");
                    applyFilters();
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        startPicker.setTitle("Select Start Date");
        startPicker.show();
    }

    private void loadChildBudget() {
        if (childUid == null) return;

        db.collection("budgets").document(childUid + "_personal")
                .addSnapshotListener((value, error) -> {
                    if (isAdded() && binding != null && value != null && value.exists()) {
                        Budget budget = value.toObject(Budget.class);
                        if (budget != null) {
                            childBudget = budget.getAmount();
                            updateSummaryCards();
                        }
                    }
                });
    }

    private void loadChildExpenses() {
        if (childUid == null) return;

        db.collection("expenses")
                .whereEqualTo("userId", childUid)
                .addSnapshotListener((value, error) -> {
                    if (isAdded() && binding != null && value != null) {
                        allExpenses.clear();
                        List<Expense> allList = value.toObjects(Expense.class);
                        for (Expense e : allList) {
                            // Only include personal expenses (not group)
                            if (e.getGroupId() == null || e.getGroupId().isEmpty()) {
                                allExpenses.add(e);
                            }
                        }
                        applyFilters();
                        updateCharts();
                    }
                });
    }

    private void applyFilters() {
        filteredExpenses.clear();

        Date startDate = getStartDateForFilter();

        for (Expense expense : allExpenses) {
            // Date filter
            if (startDate != null && expense.getDate() != null && expense.getDate().before(startDate)) {
                continue;
            }

            // Category filter
            if (!currentCategoryFilter.equals("All") &&
                    !expense.getCategory().equalsIgnoreCase(currentCategoryFilter)) {
                continue;
            }

            // Search filter
            if (!currentSearchQuery.isEmpty()) {
                String description = expense.getDescription() != null ? expense.getDescription().toLowerCase() : "";
                String category = expense.getCategory() != null ? expense.getCategory().toLowerCase() : "";
                String amount = String.valueOf(expense.getAmount());

                if (!description.contains(currentSearchQuery) &&
                        !category.contains(currentSearchQuery) &&
                        !amount.contains(currentSearchQuery)) {
                    continue;
                }
            }

            filteredExpenses.add(expense);
        }

        // Sort
        Collections.sort(filteredExpenses, (e1, e2) -> {
            if (e1.getDate() == null || e2.getDate() == null) return 0;
            return sortNewestFirst ? e2.getDate().compareTo(e1.getDate()) : e1.getDate().compareTo(e2.getDate());
        });

        // Update UI
        adapter.setExpenses(filteredExpenses);
        binding.tvResultCount.setText(filteredExpenses.size() + " results");

        // Show/hide empty state
        if (filteredExpenses.isEmpty()) {
            binding.llEmptyState.setVisibility(View.VISIBLE);
            binding.rvChildExpenses.setVisibility(View.GONE);
        } else {
            binding.llEmptyState.setVisibility(View.GONE);
            binding.rvChildExpenses.setVisibility(View.VISIBLE);
        }

        updateSummaryCards();
    }

    private Date getStartDateForFilter() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        switch (currentDateFilter) {
            case "Today":
                return cal.getTime();
            case "This Week":
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                return cal.getTime();
            case "This Month":
                cal.set(Calendar.DAY_OF_MONTH, 1);
                return cal.getTime();
            case "This Year":
                cal.set(Calendar.DAY_OF_YEAR, 1);
                return cal.getTime();
            case "All Time":
            default:
                return null;
        }
    }

    private void updateSummaryCards() {
        if (binding == null) return;

        // Calculate total spent from ALL expenses (not filtered) for accurate summary
        double totalSpent = 0;
        for (Expense e : allExpenses) {
            totalSpent += e.getAmount();
        }

        double remaining = childBudget - totalSpent;

        binding.tvTotalSpent.setText(String.format(Locale.getDefault(), "৳%.0f", totalSpent));
        binding.tvBudget.setText(String.format(Locale.getDefault(), "৳%.0f", childBudget));

        if (remaining >= 0) {
            binding.tvRemaining.setText(String.format(Locale.getDefault(), "৳%.0f", remaining));
            binding.tvRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        } else {
            binding.tvRemaining.setText(String.format(Locale.getDefault(), "-৳%.0f", Math.abs(remaining)));
            binding.tvRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        }

        // Calculate top category
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Expense e : allExpenses) {
            String cat = e.getCategory() != null ? e.getCategory() : "Other";
            categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0) + e.getAmount());
        }

        String topCategory = "N/A";
        double maxAmount = 0;
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue() > maxAmount) {
                maxAmount = entry.getValue();
                topCategory = entry.getKey();
            }
        }

        binding.tvTopCategory.setText(topCategory);
        binding.tvTopCategoryAmount.setText(String.format(Locale.getDefault(), "৳%.0f", maxAmount));
    }

    private void updateCharts() {
        if (binding == null || allExpenses.isEmpty()) {
            showEmptyCharts();
            return;
        }

        // Calculate category totals
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Expense e : allExpenses) {
            String cat = e.getCategory() != null ? e.getCategory() : "Other";
            categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0) + e.getAmount());
        }

        updatePieChart(categoryTotals);
        updateBarChart(categoryTotals);
    }

    private void showEmptyCharts() {
        binding.pieChart.clear();
        binding.pieChart.setCenterText("No Data");
        binding.pieChart.invalidate();

        binding.barChart.clear();
        binding.barChart.invalidate();
    }

    private void updatePieChart(Map<String, Double> categoryTotals) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(chartColors);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(binding.pieChart));
        binding.pieChart.setData(data);
        binding.pieChart.invalidate();
    }

    private void updateBarChart(Map<String, Double> categoryTotals) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Spending");
        dataSet.setColors(chartColors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);

        binding.barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.barChart.getXAxis().setLabelCount(labels.size());
        binding.barChart.setData(data);
        binding.barChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

