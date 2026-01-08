package com.example.expensetracker2207050.ui.group;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.expensetracker2207050.R;
import com.example.expensetracker2207050.databinding.FragmentCompareAnalyticsBinding;
import com.example.expensetracker2207050.models.Expense;
import com.example.expensetracker2207050.models.GroupMember;
import com.example.expensetracker2207050.models.User;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CompareAnalyticsFragment extends Fragment {

    private FragmentCompareAnalyticsBinding binding;
    private FirebaseFirestore db;
    private String groupId;
    private String groupName;
    private List<Expense> allExpenses = new ArrayList<>();
    private Map<String, String> memberNames = new HashMap<>();
    private String currentPeriod = "monthly";
    private MemberRankingAdapter rankingAdapter;

    private final int[] chartColors = {
            Color.parseColor("#7C3AED"),
            Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#3B82F6"),
            Color.parseColor("#EC4899"),
            Color.parseColor("#06B6D4"),
            Color.parseColor("#8B5CF6")
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCompareAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            groupId = getArguments().getString("groupId");
            groupName = getArguments().getString("groupName");
        }

        binding.tvGroupName.setText(groupName != null ? groupName : "Group");

        setupRankingsList();
        setupCharts();
        setupTimePeriodToggle();
        loadExpenses();

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
    }

    private void setupRankingsList() {
        rankingAdapter = new MemberRankingAdapter();
        binding.rvRankings.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRankings.setAdapter(rankingAdapter);
    }

    private void setupCharts() {
        HorizontalBarChart hBarChart = binding.memberBarChart;
        hBarChart.getDescription().setEnabled(false);
        hBarChart.setDrawGridBackground(false);
        hBarChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        hBarChart.getXAxis().setTextColor(Color.WHITE);
        hBarChart.getXAxis().setDrawGridLines(false);
        hBarChart.getXAxis().setGranularity(1f);
        hBarChart.getAxisLeft().setTextColor(Color.WHITE);
        hBarChart.getAxisLeft().setDrawGridLines(true);
        hBarChart.getAxisLeft().setGridColor(Color.parseColor("#333333"));
        hBarChart.getAxisRight().setEnabled(false);
        hBarChart.getLegend().setEnabled(false);
        hBarChart.animateY(1000);

        PieChart pieChart = binding.contributionPieChart;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Share");
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.setCenterTextSize(16f);
        pieChart.setRotationEnabled(true);
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setTextColor(Color.WHITE);
        pieChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        pieChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(10f);

        BarChart transactionChart = binding.transactionBarChart;
        transactionChart.getDescription().setEnabled(false);
        transactionChart.setDrawGridBackground(false);
        transactionChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        transactionChart.getXAxis().setTextColor(Color.WHITE);
        transactionChart.getXAxis().setDrawGridLines(false);
        transactionChart.getAxisLeft().setTextColor(Color.WHITE);
        transactionChart.getAxisLeft().setDrawGridLines(true);
        transactionChart.getAxisLeft().setGridColor(Color.parseColor("#333333"));
        transactionChart.getAxisRight().setEnabled(false);
        transactionChart.getLegend().setEnabled(false);
        transactionChart.animateY(1000);

        BarChart categoryChart = binding.categoryBarChart;
        categoryChart.getDescription().setEnabled(false);
        categoryChart.setDrawGridBackground(false);
        categoryChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        categoryChart.getXAxis().setTextColor(Color.WHITE);
        categoryChart.getXAxis().setDrawGridLines(false);
        categoryChart.getAxisLeft().setTextColor(Color.WHITE);
        categoryChart.getAxisLeft().setDrawGridLines(true);
        categoryChart.getAxisLeft().setGridColor(Color.parseColor("#333333"));
        categoryChart.getAxisRight().setEnabled(false);
        categoryChart.getLegend().setEnabled(true);
        categoryChart.getLegend().setTextColor(Color.WHITE);
        categoryChart.animateY(1000);
    }

    private void setupTimePeriodToggle() {
        binding.toggleTime.check(R.id.btnMonthly);

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

        binding.btnAllTime.setOnClickListener(v -> {
            currentPeriod = "all";
            updateCharts();
        });
    }

    private void loadExpenses() {
        if (groupId == null || groupId.isEmpty()) return;

        db.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || binding == null || value == null) return;

                    allExpenses.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Expense expense = doc.toObject(Expense.class);
                        allExpenses.add(expense);
                    }
                    loadMemberNames();
                });
    }

    private void loadMemberNames() {
        List<String> uniqueIds = new ArrayList<>();
        for (Expense expense : allExpenses) {
            String contributorId = expense.getContributorId();
            if (contributorId == null) contributorId = expense.getUserId();
            if (!uniqueIds.contains(contributorId)) {
                uniqueIds.add(contributorId);
            }
        }

        if (uniqueIds.isEmpty()) {
            updateCharts();
            return;
        }

        final int[] loadedCount = {0};
        for (String uid : uniqueIds) {
            db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            User user = doc.toObject(User.class);
                            if (user != null && user.getUsername() != null) {
                                memberNames.put(uid, user.getUsername());
                            } else {
                                memberNames.put(uid, "User");
                            }
                        } else {
                            memberNames.put(uid, "User");
                        }
                        loadedCount[0]++;
                        if (loadedCount[0] == uniqueIds.size()) {
                            updateCharts();
                        }
                    })
                    .addOnFailureListener(e -> {
                        memberNames.put(uid, "User");
                        loadedCount[0]++;
                        if (loadedCount[0] == uniqueIds.size()) {
                            updateCharts();
                        }
                    });
        }
    }

    private void updateCharts() {
        List<Expense> filtered = filterExpensesByPeriod(allExpenses);

        if (filtered.isEmpty()) {
            showEmptyState();
            return;
        }

        updateMemberSpendingChart(filtered);
        updateContributionPieChart(filtered);
        updateTransactionCountChart(filtered);
        updateCategoryChart(filtered);
        updateRankings(filtered);
    }

    private List<Expense> filterExpensesByPeriod(List<Expense> expenses) {
        if (currentPeriod.equals("all")) {
            return new ArrayList<>(expenses);
        }

        Calendar cal = Calendar.getInstance();
        Date startDate;

        switch (currentPeriod) {
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
        binding.memberBarChart.clear();
        binding.memberBarChart.invalidate();
        binding.contributionPieChart.clear();
        binding.contributionPieChart.setCenterText("No Data");
        binding.contributionPieChart.invalidate();
        binding.transactionBarChart.clear();
        binding.transactionBarChart.invalidate();
        binding.categoryBarChart.clear();
        binding.categoryBarChart.invalidate();
        rankingAdapter.setMembers(new ArrayList<>());
    }

    private void updateMemberSpendingChart(List<Expense> expenses) {
        Map<String, Double> memberSpending = new HashMap<>();
        for (Expense e : expenses) {
            String contributorId = e.getContributorId();
            if (contributorId == null) contributorId = e.getUserId();
            memberSpending.put(contributorId, memberSpending.getOrDefault(contributorId, 0.0) + e.getAmount());
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Double> entry : memberSpending.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            String name = memberNames.getOrDefault(entry.getKey(), "User");
            if (name.length() > 10) name = name.substring(0, 10) + "..";
            labels.add(name);
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Spending");
        dataSet.setColors(chartColors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        binding.memberBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.memberBarChart.getXAxis().setLabelCount(labels.size());
        binding.memberBarChart.setData(data);
        binding.memberBarChart.invalidate();
    }

    private void updateContributionPieChart(List<Expense> expenses) {
        Map<String, Double> memberSpending = new HashMap<>();
        for (Expense e : expenses) {
            String contributorId = e.getContributorId();
            if (contributorId == null) contributorId = e.getUserId();
            memberSpending.put(contributorId, memberSpending.getOrDefault(contributorId, 0.0) + e.getAmount());
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : memberSpending.entrySet()) {
            String name = memberNames.getOrDefault(entry.getKey(), "User");
            entries.add(new PieEntry(entry.getValue().floatValue(), name));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(chartColors);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(binding.contributionPieChart));
        binding.contributionPieChart.setData(data);
        binding.contributionPieChart.invalidate();
    }

    private void updateTransactionCountChart(List<Expense> expenses) {
        Map<String, Integer> memberTransactions = new HashMap<>();
        for (Expense e : expenses) {
            String contributorId = e.getContributorId();
            if (contributorId == null) contributorId = e.getUserId();
            memberTransactions.put(contributorId, memberTransactions.getOrDefault(contributorId, 0) + 1);
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Integer> entry : memberTransactions.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            String name = memberNames.getOrDefault(entry.getKey(), "User");
            if (name.length() > 8) name = name.substring(0, 8) + "..";
            labels.add(name);
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Transactions");
        dataSet.setColors(chartColors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        binding.transactionBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.transactionBarChart.getXAxis().setLabelCount(labels.size());
        binding.transactionBarChart.setData(data);
        binding.transactionBarChart.invalidate();
    }

    private void updateCategoryChart(List<Expense> expenses) {
        Map<String, Map<String, Double>> memberCategorySpending = new HashMap<>();
        List<String> categories = new ArrayList<>();

        for (Expense e : expenses) {
            String contributorId = e.getContributorId();
            if (contributorId == null) contributorId = e.getUserId();
            String category = e.getCategory();

            if (!categories.contains(category)) categories.add(category);

            if (!memberCategorySpending.containsKey(contributorId)) {
                memberCategorySpending.put(contributorId, new HashMap<>());
            }
            Map<String, Double> catMap = memberCategorySpending.get(contributorId);
            catMap.put(category, catMap.getOrDefault(category, 0.0) + e.getAmount());
        }

        List<BarDataSet> dataSets = new ArrayList<>();
        int colorIndex = 0;
        for (Map.Entry<String, Map<String, Double>> memberEntry : memberCategorySpending.entrySet()) {
            List<BarEntry> entries = new ArrayList<>();
            for (int i = 0; i < categories.size(); i++) {
                double value = memberEntry.getValue().getOrDefault(categories.get(i), 0.0);
                entries.add(new BarEntry(i, (float) value));
            }
            String name = memberNames.getOrDefault(memberEntry.getKey(), "User");
            BarDataSet dataSet = new BarDataSet(entries, name);
            dataSet.setColor(chartColors[colorIndex % chartColors.length]);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueTextSize(8f);
            dataSets.add(dataSet);
            colorIndex++;
        }

        if (dataSets.isEmpty()) return;

        float groupSpace = 0.2f;
        float barSpace = 0.02f;
        float barWidth = (1f - groupSpace) / dataSets.size() - barSpace;

        BarData data = new BarData(dataSets.toArray(new BarDataSet[0]));
        data.setBarWidth(barWidth);

        binding.categoryBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(categories));
        binding.categoryBarChart.getXAxis().setCenterAxisLabels(true);
        binding.categoryBarChart.getXAxis().setLabelCount(categories.size());
        binding.categoryBarChart.setData(data);
        binding.categoryBarChart.groupBars(0f, groupSpace, barSpace);
        binding.categoryBarChart.invalidate();
    }

    private void updateRankings(List<Expense> expenses) {
        Map<String, Double> memberSpending = new HashMap<>();
        Map<String, Integer> memberTransactions = new HashMap<>();

        for (Expense e : expenses) {
            String contributorId = e.getContributorId();
            if (contributorId == null) contributorId = e.getUserId();
            memberSpending.put(contributorId, memberSpending.getOrDefault(contributorId, 0.0) + e.getAmount());
            memberTransactions.put(contributorId, memberTransactions.getOrDefault(contributorId, 0) + 1);
        }

        List<GroupMember> rankings = new ArrayList<>();
        for (Map.Entry<String, Double> entry : memberSpending.entrySet()) {
            GroupMember member = new GroupMember();
            member.setUid(entry.getKey());
            member.setUsername(memberNames.getOrDefault(entry.getKey(), "User"));
            member.setTotalSpent(entry.getValue());
            member.setTransactionCount(memberTransactions.getOrDefault(entry.getKey(), 0));
            rankings.add(member);
        }

        rankings.sort((m1, m2) -> Double.compare(m2.getTotalSpent(), m1.getTotalSpent()));
        rankingAdapter.setMembers(rankings);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

