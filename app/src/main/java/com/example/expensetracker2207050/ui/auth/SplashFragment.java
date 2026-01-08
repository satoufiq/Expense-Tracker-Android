package com.example.expensetracker2207050.ui.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.expensetracker2207050.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Apply animations to splash elements
        View iconContainer = view.findViewById(R.id.splashIconContainer);
        View appTitle = view.findViewById(R.id.tvAppTitle);
        View subtitle = view.findViewById(R.id.llSubtitle);

        Animation fadeInScale = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_scale);
        Animation slideUpFadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_fade_in);

        iconContainer.startAnimation(fadeInScale);
        appTitle.startAnimation(slideUpFadeIn);
        
        Animation subtitleAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up_fade_in);
        subtitleAnim.setStartOffset(500);
        subtitle.startAnimation(subtitleAnim);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && getView() != null) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    Navigation.findNavController(view).navigate(R.id.action_splash_to_dashboard);
                } else {
                    Navigation.findNavController(view).navigate(R.id.action_splash_to_login);
                }
            }
        }, 2500);
    }
}
