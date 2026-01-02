package com.example.expensetracker2207050.ui.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.expensetracker2207050.databinding.FragmentGroupBinding;

public class GroupFragment extends Fragment {

    private FragmentGroupBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGroupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Implement Group Mode logic (Groups, Invitations, Shared Expenses) here
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
