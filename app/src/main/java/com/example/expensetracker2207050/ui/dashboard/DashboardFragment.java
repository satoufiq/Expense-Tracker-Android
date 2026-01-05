package com.example.expensetracker2207050.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.expensetracker2207050.R;
import com.example.expensetracker2207050.databinding.FragmentDashboardBinding;
import com.example.expensetracker2207050.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().finishAffinity();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserInfo();

        binding.cardModeSelection.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_dashboard_to_modeSelection));

        binding.cardAlerts.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_dashboard_to_alerts));

        binding.cardLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_login);
        });
    }

    private void loadUserInfo() {
        if (mAuth.getCurrentUser() == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded()) {
                        binding.progressBar.setVisibility(View.GONE);
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && binding.tvUsername != null && binding.tvAccountType != null) {
                            binding.tvUsername.setText(user.getUsername());
                            binding.tvAccountType.setText(user.getAccountType());
                            binding.tvUsername.setVisibility(View.VISIBLE);
                            binding.tvAccountType.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
