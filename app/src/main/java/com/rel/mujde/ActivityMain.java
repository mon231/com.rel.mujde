package com.rel.mujde;

import android.content.Intent;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Process;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class ActivityMain extends AppCompatActivity {
    private SharedPreferences pref;
    private BottomNavigationView bottomNavigationView;
    private Fragment currentFragment;
    private HomeFragment homeFragment;
    private ScriptsFragment scriptsFragment;
    private AppsFragment appsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            pref = getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, MODE_WORLD_READABLE);
        } catch (Exception e) {
            suggestPartialInitialization();
            return;
        }

        initializeFragments();
        setupNavigation();
        setupBackPressHandling();
    }

    private void suggestPartialInitialization()
    {
        new AlertDialog.Builder(this)
        .setMessage(R.string.module_not_enabled)
        .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // terminate app's process
                Process.killProcess(android.os.Process.myPid());

                final int EXIT_FAILURE = 1;
                System.exit(EXIT_FAILURE);
            }
        })
        .setCancelable(false)
        .show();
    }

    private void setupBackPressHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // move to home fragment if not already there
                if (currentFragment != homeFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                    return;
                }

                // popup unsaved changes dialog if needed
                if (appsFragment != null && appsFragment.hasUnsavedChanges()) {
                    new AlertDialog.Builder(ActivityMain.this)
                    .setTitle(R.string.unsaved_changes_title)
                    .setMessage(R.string.unsaved_changes_message)
                    .setPositiveButton(R.string.cancel, null)
                    .setNegativeButton(R.string.discard_and_exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
                } else {
                    finish();
                }
            }
        });
    }

    private void initializeFragments() {
        homeFragment = new HomeFragment();
        scriptsFragment = new ScriptsFragment();
        appsFragment = new AppsFragment();

        // NOTE to start from home fragment
        loadFragment(homeFragment);
    }

    private void setupNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        loadFragment(homeFragment);
                        break;

                    case R.id.navigation_scripts:
                        loadFragment(scriptsFragment);
                        break;

                    case R.id.navigation_apps:
                        loadFragment(appsFragment);
                        break;

                    default:
                        return false;
                }

                return true;
            }
        });

        // navbar highlight home option
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    private void loadFragment(Fragment fragment) {
        currentFragment = fragment;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();

        if (fragment instanceof HomeFragment) {
            getSupportActionBar().setTitle(R.string.app_name);
        } else if (fragment instanceof ScriptsFragment) {
            getSupportActionBar().setTitle(R.string.title_scripts);
        } else if (fragment instanceof AppsFragment) {
            getSupportActionBar().setTitle(R.string.title_apps);
        }
    }
}
