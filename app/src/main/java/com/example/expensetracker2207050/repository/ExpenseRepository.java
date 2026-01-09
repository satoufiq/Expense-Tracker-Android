package com.example.expensetracker2207050.repository;

import com.example.expensetracker2207050.models.Expense;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ExpenseRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public CollectionReference getExpensesCollection() {
        return db.collection("expenses");
    }

    public Task<Void> addExpense(Expense expense) {
        String id = expense.getId();
        if (id == null || id.isEmpty()) {
            id = getExpensesCollection().document().getId();
            expense.setId(id);
        }
        return getExpensesCollection().document(id).set(expense);
    }

    public Task<Void> updateExpense(Expense expense) {
        return getExpensesCollection().document(expense.getId()).set(expense);
    }

    public Task<Void> deleteExpense(String expenseId) {
        return getExpensesCollection().document(expenseId).delete();
    }

    public Query getPersonalExpensesQuery() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        // Removed orderBy to avoid index requirement; will sort in ViewModel
        return getExpensesCollection()
                .whereEqualTo("userId", uid);
    }

    public Query getGroupExpensesQuery(String groupId) {
        // Removed orderBy to avoid index requirement; will sort in ViewModel
        return getExpensesCollection()
                .whereEqualTo("groupId", groupId);
    }
}
