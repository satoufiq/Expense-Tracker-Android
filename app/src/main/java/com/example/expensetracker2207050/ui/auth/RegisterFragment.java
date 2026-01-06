package com.example.expensetracker2207050.ui.auth;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.expensetracker2207050.R;
import com.example.expensetracker2207050.databinding.FragmentRegisterBinding;
import com.example.expensetracker2207050.models.User;
import com.example.expensetracker2207050.viewmodel.RegisterViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        observeViewModel();
        setupPasswordWatcher();

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvLogin.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());
    }

    private void observeViewModel() {
        viewModel.getPasswordStrength().observe(getViewLifecycleOwner(), strength -> {
            binding.pbPasswordStrength.setProgress(strength);
            int color;
            switch (strength) {
                case 1: color = Color.RED; break;
                case 2: color = Color.YELLOW; break;
                case 3: color = Color.BLUE; break;
                case 4: color = Color.GREEN; break;
                default: color = Color.GRAY; break;
            }
            binding.pbPasswordStrength.setProgressTintList(ColorStateList.valueOf(color));
        });

        viewModel.getStrengthLabel().observe(getViewLifecycleOwner(), label -> {
            binding.tvStrengthLabel.setText(label);
        });
    }

    private void setupPasswordWatcher() {
        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onPasswordChanged(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void registerUser() {
        String username = binding.etUsername.getText() != null ? binding.etUsername.getText().toString().trim() : "";
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";
        String confirmPassword = binding.etConfirmPassword.getText() != null ? binding.etConfirmPassword.getText().toString().trim() : "";
        String accountType = binding.rbParent.isChecked() ? "Parent" : "Normal";

        if (TextUtils.isEmpty(username)) {
            binding.etUsername.setError("Username required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email required");
            return;
        }
        if (password.length() < 6) {
            binding.etPassword.setError("Password too short");
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            return;
        }

        binding.btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser() != null ? authResult.getUser().getUid() : null;
                    if (uid == null) {
                        if (isAdded() && binding != null) {
                            binding.btnRegister.setEnabled(true);
                            Toast.makeText(getContext(), "Registration failed", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    User user = new User(uid, username, email, accountType);

                    // Auth success, now save to Firestore
                    db.collection("users").document(uid).set(user)
                            .addOnSuccessListener(aVoid -> {
                                if (isAdded() && binding != null) {
                                    Navigation.findNavController(requireView()).navigate(R.id.action_register_to_dashboard);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded() && binding != null) {
                                    binding.btnRegister.setEnabled(true);
                                    Toast.makeText(getContext(), "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && binding != null) {
                        binding.btnRegister.setEnabled(true);
                        Toast.makeText(getContext(), "Auth Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
