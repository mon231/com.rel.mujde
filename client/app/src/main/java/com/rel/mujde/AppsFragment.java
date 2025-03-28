package com.rel.mujde;

import static android.content.Context.MODE_WORLD_READABLE;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppsFragment extends Fragment implements SearchView.OnQueryTextListener {
    private SharedPreferences pref;
    private RecyclerView appListRecyclerView;
    private SearchView searchView;
    private ProgressBar loadingProgress;
    private List<ApplicationInfo> enabledApps = new ArrayList<>();
    private Map<String, List<String>> appScriptMappings = new HashMap<>();
    private Executor executor = Executors.newSingleThreadExecutor();
    private boolean hasUnsavedChanges = false;

    public AppsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apps, container, false);
        initializeViews(view);
        setupListeners();
        loadAppScriptMappings();
        loadEnabledApps();
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // TODO: cleanup

        super.onCreate(savedInstanceState);
        try {
            pref = getActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, MODE_WORLD_READABLE);
        } catch (Exception e) {
            pref = null;
        }

        if (pref == null) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.module_not_enabled)
                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getActivity().finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private void initializeViews(View view) {
        appListRecyclerView = view.findViewById(R.id.app_list);
        searchView = view.findViewById(R.id.search_view);
        loadingProgress = view.findViewById(R.id.loading_progress);
        appListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    private void setupListeners() {
        searchView.setSubmitButtonEnabled(false);
        searchView.setFocusable(true);
        searchView.setIconified(false);
        searchView.clearFocus();

        EditText searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchText != null) {
            searchText.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.white));
            searchText.setHintTextColor(ContextCompat.getColor(getActivity(), R.color.teal_200));
            try {
                Field cursorDrawableField = TextView.class.getDeclaredField("mCursorDrawableRes");
                cursorDrawableField.setAccessible(true);
                cursorDrawableField.set(searchText, R.drawable.white_cursor);
            } catch (Exception e) { }
        }

        searchView.setOnQueryTextListener(this);
    }

    private void loadAppScriptMappings() {
        // NOTE used in case of partial initialization (where Mujde's module isn't enabled)
        if (pref == null) {
            return;
        }

        appScriptMappings.clear();
        appScriptMappings.putAll(ScriptUtils.getAllAppScriptMappings(pref));
        hasUnsavedChanges = false;
    }

    private void loadEnabledApps() {
        loadingProgress.setVisibility(View.VISIBLE);
        appListRecyclerView.setVisibility(View.GONE);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<ApplicationInfo> enabledAppsList = getEnabledApps();
                    // TODO: cleanup
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (getActivity() == null) return;

                                enabledApps.clear();
                                enabledApps.addAll(enabledAppsList);

                                if (!enabledApps.isEmpty()) {
                                    AppListAdapter adapter = new AppListAdapter(
                                            getActivity(),
                                            enabledApps,
                                            new AppListAdapter.OnScriptSelectionChangedListener() {
                                                @Override
                                                public void onScriptSelectionChanged(String packageName, List<String> selectedScripts) {
                                                    // Update the app script mappings
                                                    if (selectedScripts.isEmpty()) {
                                                        appScriptMappings.remove(packageName);
                                                    } else {
                                                        appScriptMappings.put(packageName, new ArrayList<>(selectedScripts));
                                                    }

                                                    hasUnsavedChanges = true;
                                                }
                                            }
                                    );

                                    appListRecyclerView.setAdapter(adapter);
                                    loadingProgress.setVisibility(View.GONE);
                                    appListRecyclerView.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (getActivity() == null) return;

                                loadingProgress.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            }
        });
    }

    private List<ApplicationInfo> getEnabledApps() {
        PackageManager pm = getActivity().getPackageManager();
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(0);
        List<ApplicationInfo> filteredApps = new ArrayList<>();

        for (ApplicationInfo app : installedApps) {
            if (app.enabled && !app.packageName.equals(getActivity().getPackageName())) {
                filteredApps.add(app);
            }
        }

        filteredApps.sort((app1, app2) -> {
            String name1 = pm.getApplicationLabel(app1).toString().toLowerCase();
            String name2 = pm.getApplicationLabel(app2).toString().toLowerCase();
            return name1.compareTo(name2);
        });

        return filteredApps;
    }

    private void filterApps(String query) {
        AppListAdapter adapter = (AppListAdapter)appListRecyclerView.getAdapter();
        if (adapter == null) {
            return;
        }

        if (TextUtils.isEmpty(query)) {
            adapter.updateList(enabledApps);
            return;
        }

        List<ApplicationInfo> filteredApps = new ArrayList<>();
        String searchQuery = query.toLowerCase();

        for (ApplicationInfo appInfo : enabledApps) {
            String appName = getActivity().getPackageManager().getApplicationLabel(appInfo).toString().toLowerCase();
            String packageName = appInfo.packageName.toLowerCase();

            if (appName.contains(searchQuery) || packageName.contains(searchQuery)) {
                filteredApps.add(appInfo);
            }
        }

        adapter.updateList(filteredApps);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        filterApps(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        filterApps(newText);
        return true;
    }

    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_CODE_SELECT_SCRIPTS && resultCode == Activity.RESULT_OK && data != null) {
            // Reload the app script mappings from SharedPreferences
            loadAppScriptMappings();

            // Update the adapter
            if (appListRecyclerView.getAdapter() != null) {
                appListRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }
}
