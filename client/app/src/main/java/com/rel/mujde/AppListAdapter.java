package com.rel.mujde;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {
    private final Context context;
    private List<ApplicationInfo> appList;
    private final OnScriptSelectionChangedListener onScriptSelectionChangedListener;

    public AppListAdapter(
        Context context,
        List<ApplicationInfo> appList,
        OnScriptSelectionChangedListener onScriptSelectionChangedListener) {
        this.context = context;
        this.appList = appList;
        this.onScriptSelectionChangedListener = onScriptSelectionChangedListener;
    }

    public interface OnScriptSelectionChangedListener {
        void onScriptSelectionChanged(String packageName, List<String> selectedScripts);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView appIcon;
        public TextView appName;
        public TextView packageName;

        public ViewHolder(View view) {
            super(view);
            appIcon = view.findViewById(R.id.app_icon);
            appName = view.findViewById(R.id.app_name);
            packageName = view.findViewById(R.id.package_name);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ApplicationInfo app = appList.get(position);
        PackageManager packageManager = context.getPackageManager();

        holder.appIcon.setImageDrawable(app.loadIcon(packageManager));
        holder.appName.setText(app.loadLabel(packageManager));
        holder.packageName.setText(app.packageName);

        // Set item click listener to launch script selection activity
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(context instanceof Activity)) {
                    return;
                }

                Intent intent = new Intent(context, ScriptSelectionActivity.class);
                intent.putExtra(Constants.INTENT_REQUEST_PACKAGE_NAME, app.packageName);

                ((Activity)context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_SCRIPTS);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public void updateList(List<ApplicationInfo> newList) {
        this.appList = newList;
        notifyDataSetChanged();
    }
}
