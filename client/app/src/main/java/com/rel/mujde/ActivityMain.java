package com.rel.mujde;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
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
            pref = null;
        }

        if (pref == null) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.module_not_enabled)
                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
            return;
        }

        initializeFragments();
        setupNavigation();
        setupBackPressHandling();
    }

    private void setupBackPressHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // If we're not on the home fragment, navigate to home first
                if (currentFragment != homeFragment) {
                    bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                    return;
                }
                
                // Check if we have unsaved changes in the apps fragment
                if (appsFragment != null && appsFragment.hasUnsavedChanges()) {
                    new AlertDialog.Builder(ActivityMain.this)
                            .setTitle(R.string.unsaved_changes_title)
                            .setMessage(R.string.unsaved_changes_message)
                            .setPositiveButton(R.string.save_and_exit, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Save changes and exit
                                    finish();
                                }
                            })
                            .setNegativeButton(R.string.discard_and_exit, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setNeutralButton(R.string.cancel, null)
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
        
        // Initialize with home fragment, but don't select it in the navigation yet
        loadFragment(homeFragment);
    }
    
    private void setupNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                
                if (itemId == R.id.navigation_home) {
                    loadFragment(homeFragment);
                    return true;
                } else if (itemId == R.id.navigation_scripts) {
                    loadFragment(scriptsFragment);
                    return true;
                } else if (itemId == R.id.navigation_apps) {
                    loadFragment(appsFragment);
                    return true;
                }
                
                return false;
            }
        });
        
        // Set Home tab as selected by default
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }
    
    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            currentFragment = fragment;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
            
            // Update the toolbar title based on the selected fragment
            if (fragment instanceof HomeFragment) {
                getSupportActionBar().setTitle(R.string.app_name);
            } else if (fragment instanceof ScriptsFragment) {
                getSupportActionBar().setTitle(R.string.title_scripts);
            } else if (fragment instanceof AppsFragment) {
                getSupportActionBar().setTitle(R.string.title_apps);
            }
        }
    }


}
