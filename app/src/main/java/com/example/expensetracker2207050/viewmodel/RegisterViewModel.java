package com.example.expensetracker2207050.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RegisterViewModel extends ViewModel {

    private final MutableLiveData<Integer> passwordStrength = new MutableLiveData<>(0);
    private final MutableLiveData<String> strengthLabel = new MutableLiveData<>("Strength: Very Weak");

    public LiveData<Integer> getPasswordStrength() {
        return passwordStrength;
    }

    public LiveData<String> getStrengthLabel() {
        return strengthLabel;
    }

    public void onPasswordChanged(String password) {
        int strength = 0;
        if (password.length() >= 8) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[@#$%^&+=!].*")) strength++;

        passwordStrength.setValue(strength);

        String label = "Strength: ";
        switch (strength) {
            case 0: label += "Very Weak"; break;
            case 1: label += "Weak"; break;
            case 2: label += "Fair"; break;
            case 3: label += "Good"; break;
            case 4: label += "Strong"; break;
        }
        strengthLabel.setValue(label);
    }
}
