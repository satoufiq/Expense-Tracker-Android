package com.example.expensetracker2207050.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.expensetracker2207050.models.Expense;
import com.example.expensetracker2207050.repository.ExpenseRepository;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PersonalViewModel extends ViewModel {
    private final ExpenseRepository repository = new ExpenseRepository();
    private final MutableLiveData<List<Expense>> personalExpenses = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration listenerRegistration;

    public LiveData<List<Expense>> getPersonalExpenses() {
        if (listenerRegistration == null) {
            loadExpenses();
        }
        return personalExpenses;
    }

    private void loadExpenses() {
        listenerRegistration = repository.getPersonalExpensesQuery()
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        List<Expense> allList = value.toObjects(Expense.class);
                        List<Expense> filteredList = new ArrayList<>();
                        for (Expense e : allList) {
                            // Filter out group expenses
                            if (e.getGroupId() == null || e.getGroupId().isEmpty()) {
                                filteredList.add(e);
                            }
                        }
                        // Sort by date descending
                        Collections.sort(filteredList, (e1, e2) -> {
                            if (e1.getDate() == null || e2.getDate() == null) return 0;
                            return e2.getDate().compareTo(e1.getDate());
                        });
                        personalExpenses.setValue(filteredList);
                    }
                });
    }

    public void addExpense(Expense expense) {
        repository.addExpense(expense);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
