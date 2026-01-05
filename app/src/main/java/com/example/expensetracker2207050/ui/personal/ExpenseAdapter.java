package com.example.expensetracker2207050.ui.personal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expensetracker2207050.databinding.ItemExpenseBinding;
import com.example.expensetracker2207050.models.Expense;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenses = new ArrayList<>();
    private final Map<String, String> userNameCache = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemExpenseBinding binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ExpenseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        holder.bind(expenses.get(position));
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final ItemExpenseBinding binding;

        public ExpenseViewHolder(ItemExpenseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Expense expense) {
            binding.tvCategory.setText(expense.getCategory());
            binding.tvDescription.setText(expense.getDescription());
            binding.tvAmount.setText(String.format(Locale.getDefault(), "$%.2f", expense.getAmount()));
            if (expense.getDate() != null) {
                binding.tvDate.setText(dateFormat.format(expense.getDate()));
            }

            String uid = expense.getContributorId() != null ? expense.getContributorId() : expense.getUserId();

            if (expense.getGroupId() != null && !expense.getGroupId().isEmpty()) {
                binding.tvContributor.setVisibility(View.VISIBLE);
                if (userNameCache.containsKey(uid)) {
                    binding.tvContributor.setText("By: " + userNameCache.get(uid));
                } else {
                    binding.tvContributor.setText("By: Loading...");
                    fetchUserName(uid);
                }
            } else {
                binding.tvContributor.setVisibility(View.GONE);
            }
        }

        private void fetchUserName(String uid) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString("username");
                        if (name != null) {
                            userNameCache.put(uid, name);
                            notifyItemChanged(getAdapterPosition());
                        }
                    });
        }
    }
}
