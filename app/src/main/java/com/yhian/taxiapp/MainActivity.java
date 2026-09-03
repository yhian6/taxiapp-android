package com.yhian.taxiapp;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.yhian.taxiapp.fragments.HistoryFragment;
import com.yhian.taxiapp.fragments.HomeFragment;
import com.yhian.taxiapp.fragments.PointsFragment;
import com.yhian.taxiapp.fragments.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private boolean focusRewardsOnNextPointsOpen;
@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(getColor(R.color.background_light));
        window.setNavigationBarColor(getColor(R.color.background_light));
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        showFragment(new HomeFragment());

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                showFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_points) {
                if (focusRewardsOnNextPointsOpen) {
                    showFragment(PointsFragment.newInstance(true));
                    focusRewardsOnNextPointsOpen = false;
                } else {
                    showFragment(new PointsFragment());
                }
                return true;
            } else if (itemId == R.id.nav_history) {
                showFragment(new HistoryFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                showFragment(new ProfileFragment());
                return true;
            }

            return false;
        });
    }


    public void openPointsRewards() {
        focusRewardsOnNextPointsOpen = true;
        if (bottomNavigation != null && bottomNavigation.getSelectedItemId() != R.id.nav_points) {
            bottomNavigation.setSelectedItemId(R.id.nav_points);
            return;
        }

        showFragment(PointsFragment.newInstance(true));
        focusRewardsOnNextPointsOpen = false;
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}




