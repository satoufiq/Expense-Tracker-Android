package com.example.expensetracker2207050.ui.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.expensetracker2207050.R;
import com.example.expensetracker2207050.databinding.FragmentAnalyticsBinding;
import com.example.expensetracker2207050.models.Expense;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class AnalyticsFragment extends Fragment {

    private FragmentAnalyticsBinding binding;
    private FirebaseFirestore db;
    private String uid;
    private String groupId;
    private List<Expense> allExpenses = new ArrayList<>();
    private String currentPeriod = "monthly";

    private final int[] chartColors = {
            Color.parseColor("#7C3AED"),
            Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#3B82F6"),
            Color.parseColor("#EC4899")
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        // Get groupId if passed from group mode
        if (getArguments() != null) {
            groupId = getArguments().getString("groupId");
        }

        // Back button
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        setupCharts();
        setupTimePeriodToggle();
        loadExpenses();
    }

    private void setupCharts() {
        // Setup Pie Chart
        PieChart pieChart = binding.pieChart;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Categories");
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.setCenterTextSize(16f);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setTextColor(Color.WHITE);
        pieChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        pieChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(12f);

        // Setup Bar Chart
        BarChart barChart = binding.barChart;
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setFitBars(true);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setTextColor(Color.WHITE);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisLeft().setGridColor(Color.parseColor("#333333"));
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.animateY(1000);

        // Setup Line Chart
        LineChart lineChart = binding.lineChart;
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setTextColor(Color.WHITE);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getAxisLeft().setTextColor(Color.WHITE);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#333333"));
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.animateX(1000);
    }

    private void setupTimePeriodToggle() {
        // Set initial selection
        binding.toggleTime.check(R.id.btnMonthly);

        binding.btnDaily.setOnClickListener(v -> {
            currentPeriod = "daily";
            updateCharts();
        });

        binding.btnWeekly.setOnClickListener(v -> {
            currentPeriod = "weekly";
            updateCharts();
        });

        binding.btnMonthly.setOnClickListener(v -> {
            currentPeriod = "monthly";
            updateCharts();
        });

        binding.btnYearly.setOnClickListener(v -> {
            currentPeriod = "yearly";
            updateCharts();
        });
    }

    private void loadExpenses() {
        if (uid == null) return;

        if (groupId != null && !groupId.isEmpty()) {
            db.collection("expenses").whereEqualTo("groupId", groupId)
                    .addSnapshotListener((value, error) -> {
                        if (isAdded() && binding != null && value != null) {
                            allExpenses.clear();
                            for (QueryDocumentSnapshot doc : value) {
                                Expense expense = doc.toObject(Expense.class);
                                allExpenses.add(expense);
                            }
                            updateCharts();
                        }
                    });
        } else {
            db.collection("expenses").whereEqualTo("userId", uid)
                    .addSnapshotListener((value, error) -> {
                        if (isAdded() && binding != null && value != null) {
                            allExpenses.clear();
                            for (QueryDocumentSnapshot doc : value) {
                                Expense expense = doc.toObject(Expense.class);
                                if (expense.getGroupId() == null) {
                                    allExpenses.add(expense);
                                }
                            }
                            updateCharts();
                        }
                    });
        }
    }

    private void updateCharts() {
        if (allExpenses.isEmpty()) {
            showEmptyState();
            return;
        }

        List<Expense> filteredExpenses = filterExpensesByPeriod(allExpenses);

        if (filteredExpenses.isEmpty()) {
            showEmptyState();
            return;
        }

        updatePieChart(filteredExpenses);
        updateBarChart(filteredExpenses);
        updateLineChart(filteredExpenses);
    }

    private List<Expense> filterExpensesByPeriod(List<Expense> expenses) {
        Calendar cal = Calendar.getInstance();
        Date startDate;

        switch (currentPeriod) {
            case "daily":
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                startDate = cal.getTime();
                break;
            case "weekly":
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                startDate = cal.getTime();
                break;
            case "monthly":
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                startDate = cal.getTime();
                break;
            case "yearly":
                cal.set(Calendar.DAY_OF_YEAR, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                startDate = cal.getTime();
                break;
            default:
                startDate = new Date(0);
        }

        List<Expense> filtered = new ArrayList<>();
        for (Expense e : expenses) {
            if (e.getDate() != null && !e.getDate().before(startDate)) {
                filtered.add(e);
            }
        }
        return filtered;
    }

    private void showEmptyState() {
        binding.pieChart.clear();
        binding.pieChart.setCenterText("No Data");
        binding.pieChart.invalidate();

        binding.barChart.clear();
        binding.barChart.invalidate();

        binding.lineChart.clear();
        binding.lineChart.invalidate();
    }

    private void updatePieChart(List<Expense> expenses) {
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Expense e : expenses) {
            String cat = e.getCategory();
            categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0) + e.getAmount());
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(chartColors);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(binding.pieChart));
        binding.pieChart.setData(data);
        binding.pieChart.invalidate();
    }

    private void updateBarChart(List<Expense> expenses) {
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Expense e : expenses) {
            String cat = e.getCategory();
            categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0) + e.getAmount());
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Expenses");
        dataSet.setColors(chartColors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        binding.barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.barChart.getXAxis().setLabelCount(labels.size());
        binding.barChart.setData(data);
        binding.barChart.invalidate();
    }

    private void updateLineChart(List<Expense> expenses) {
        SimpleDateFormat sdf;
        switch (currentPeriod) {
            case "daily":
                sdf = new SimpleDateFormat("HH:00", Locale.getDefault());
                break;
            case "weekly":
                sdf = new SimpleDateFormat("EEE", Locale.getDefault());
                break;
            case "yearly":
                sdf = new SimpleDateFormat("MMM", Locale.getDefault());
                break;
            default:
                sdf = new SimpleDateFormat("dd", Locale.getDefault());
        }

        Map<String, Double> dailyTotals = new TreeMap<>();
        for (Expense e : expenses) {
            if (e.getDate() != null) {
                String dateKey = sdf.format(e.getDate());
                dailyTotals.put(dateKey, dailyTotals.getOrDefault(dateKey, 0.0) + e.getAmount());
            }
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Double> entry : dailyTotals.entrySet()) {
            entries.add(new Entry(index, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            index++;
        }

        if (entries.isEmpty()) return;

        LineDataSet dataSet = new LineDataSet(entries, "Trend");
        dataSet.setColor(chartColors[0]);
        dataSet.setCircleColor(chartColors[0]);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(chartColors[0]);
        dataSet.setFillAlpha(50);

        LineData data = new LineData(dataSet);
        binding.lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.lineChart.getXAxis().setLabelCount(Math.min(labels.size(), 7));
        binding.lineChart.setData(data);
        binding.lineChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

