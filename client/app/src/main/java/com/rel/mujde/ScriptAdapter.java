package com.rel.mujde;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class ScriptAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> scripts;
    private final ScriptActionListener listener;

    public interface ScriptActionListener {
        void onEditScript(String scriptName);
        void onDeleteScript(String scriptName);
    }

    public ScriptAdapter(@NonNull Context context, List<String> scripts, ScriptActionListener listener) {
        super(context, R.layout.item_script, scripts);
        this.context = context;
        this.scripts = scripts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_script, parent, false);

            holder = new ViewHolder();
            holder.scriptNameTextView = convertView.findViewById(R.id.text_script_name);
            holder.editButton = convertView.findViewById(R.id.btn_edit_script);
            holder.deleteButton = convertView.findViewById(R.id.btn_delete_script);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        final String scriptName = scripts.get(position);
        holder.scriptNameTextView.setText(scriptName);

        holder.editButton.setOnClickListener(e -> {
            if (listener != null) {
                listener.onEditScript(scriptName);
            }
        });

        holder.deleteButton.setOnClickListener(e -> {
            if (listener != null) {
                listener.onDeleteScript(scriptName);
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView scriptNameTextView;
        ImageButton editButton;
        ImageButton deleteButton;
    }
}
