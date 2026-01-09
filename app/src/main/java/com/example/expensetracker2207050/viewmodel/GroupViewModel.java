package com.example.expensetracker2207050.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.expensetracker2207050.models.Expense;
import com.example.expensetracker2207050.models.Group;
import com.example.expensetracker2207050.repository.ExpenseRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupViewModel extends ViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final ExpenseRepository expenseRepository = new ExpenseRepository();

    private final MutableLiveData<Group> currentGroup = new MutableLiveData<>();
    private final MutableLiveData<List<Group>> userGroups = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Expense>> groupExpenses = new MutableLiveData<>(new ArrayList<>());

    private ListenerRegistration groupsListener;
    private ListenerRegistration expensesListener;
    private String selectedGroupId = null;

    public LiveData<Group> getCurrentGroup() {
        if (groupsListener == null) {
            loadGroups();
        }
        return currentGroup;
    }

    public LiveData<List<Group>> getUserGroups() {
        if (groupsListener == null) {
            loadGroups();
        }
        return userGroups;
    }

    public LiveData<List<Expense>> getGroupExpenses() {
        return groupExpenses;
    }

    public void selectGroup(String groupId) {
        selectedGroupId = groupId;
        List<Group> groups = userGroups.getValue();
        if (groups != null) {
            for (Group group : groups) {
                if (group.getId().equals(groupId)) {
                    currentGroup.setValue(group);
                    loadExpenses(groupId);
                    return;
                }
            }
        }
    }

    private void loadGroups() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        groupsListener = db.collection("groups")
                .whereArrayContains("memberIds", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null && !value.isEmpty()) {
                        List<Group> groups = value.toObjects(Group.class);
                        userGroups.setValue(groups);

                        // Auto-select first group if no group is selected, or restore selection
                        if (selectedGroupId != null) {
                            // Try to find the previously selected group
                            for (Group group : groups) {
                                if (group.getId().equals(selectedGroupId)) {
                                    currentGroup.setValue(group);
                                    loadExpenses(group.getId());
                                    return;
                                }
                            }
                        }
                        // Default to first group
                        Group firstGroup = groups.get(0);
                        selectedGroupId = firstGroup.getId();
                        currentGroup.setValue(firstGroup);
                        loadExpenses(firstGroup.getId());
                    } else {
                        userGroups.setValue(new ArrayList<>());
                        currentGroup.setValue(null);
                        groupExpenses.setValue(new ArrayList<>());
                    }
                });
    }

    private void loadExpenses(String groupId) {
        if (expensesListener != null) expensesListener.remove();
        expensesListener = expenseRepository.getGroupExpensesQuery(groupId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        List<Expense> list = value.toObjects(Expense.class);
                        // Sort by date descending in code to avoid composite index requirement
                        Collections.sort(list, (e1, e2) -> {
                            if (e1.getDate() == null || e2.getDate() == null) return 0;
                            return e2.getDate().compareTo(e1.getDate());
                        });
                        groupExpenses.setValue(list);
                    }
                });
    }

    public void addExpense(Expense expense) {
        expenseRepository.addExpense(expense);
    }

    public void updateExpense(Expense expense) {
        expenseRepository.updateExpense(expense);
    }

    public void deleteExpense(String expenseId) {
        expenseRepository.deleteExpense(expenseId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (groupsListener != null) groupsListener.remove();
        if (expensesListener != null) expensesListener.remove();
    }
}
