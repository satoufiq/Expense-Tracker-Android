package com.example.expensetracker2207050.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.expensetracker2207050.R;
import com.example.expensetracker2207050.databinding.FragmentModeSelectionBinding;
import com.example.expensetracker2207050.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ModeSelectionFragment extends Fragment {

    private FragmentModeSelectionBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentModeSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        checkAccountType();

        binding.cardPersonal.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_modeSelection_to_personal));
        binding.cardGroup.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_modeSelection_to_group));
        binding.cardParent.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_modeSelection_to_parent));
    }

    private void checkAccountType() {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null && "Parent".equals(user.getAccountType())) {
                        binding.cardParent.setVisibility(View.VISIBLE);
                    } else {
                        binding.cardParent.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
