package com.rel.mujde;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ScriptCheckboxAdapter extends RecyclerView.Adapter<ScriptCheckboxAdapter.ViewHolder> {

    private final List<String> availableScripts;
    private final List<String> selectedScripts;

    public ScriptCheckboxAdapter(List<String> availableScripts, List<String> selectedScripts) {
        this.availableScripts = availableScripts;
        this.selectedScripts = selectedScripts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_script_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String scriptName = availableScripts.get(position);
        holder.scriptCheckbox.setText(scriptName);
        holder.scriptCheckbox.setChecked(selectedScripts.contains(scriptName));

        holder.scriptCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedScripts.contains(scriptName)) {
                    selectedScripts.add(scriptName);
                }
            } else {
                selectedScripts.remove(scriptName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return availableScripts.size();
    }

    public List<String> getSelectedScripts() {
        return new ArrayList<>(selectedScripts);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox scriptCheckbox;

        ViewHolder(View view) {
            super(view);
            scriptCheckbox = view.findViewById(R.id.script_checkbox);
        }
    }
}
