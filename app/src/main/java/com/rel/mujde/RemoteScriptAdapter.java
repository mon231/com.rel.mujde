package com.rel.mujde;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.rel.mujde.api.model.Script;

import java.util.List;

public class RemoteScriptAdapter extends RecyclerView.Adapter<RemoteScriptAdapter.MyAdapter> {

    //private final Context context;
    private final List<Script> scripts;
    private final ScriptActionListener listener;

    private final LayoutInflater inflater;

    public interface ScriptActionListener {
        void onDownloadScript(Script script);
        void onDeleteScript(Script script);
    }

    public RemoteScriptAdapter(Context context, List<Script> scripts, ScriptActionListener listener) {
        //super(context, R.layout.item_remote_script, scripts);
        this.inflater = LayoutInflater.from(context);  // Correct way to store context
        //this.context = context;
        this.scripts = scripts;
        this.listener = listener;
    }
    @NonNull
    @Override
    public MyAdapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_remote_script, parent, false);
        //View view = LayoutInflater.from(context).inflate(R.layout.item_remote_script, parent, false);
        return new MyAdapter(view); // Using the adapter as the view itself
    }

    @Override
    public void onBindViewHolder(@NonNull MyAdapter holder, int position) {
        Script script = scripts.get(position);

        TextView textScriptName = holder.itemView.findViewById(R.id.text_remote_script_name);
        TextView textScriptId = holder.itemView.findViewById(R.id.text_remote_script_id);
        TextView textLastModified = holder.itemView.findViewById(R.id.text_remote_script_modified);
        Button btnDownload = holder.itemView.findViewById(R.id.btn_download_script);
        Button btnDelete = holder.itemView.findViewById(R.id.btn_delete_script);

        textScriptName.setText(script.getScriptName());
        textScriptId.setText("ID: " + script.getScriptId());
        textLastModified.setText("Last Modified: " + script.getLastModified());

        btnDownload.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadScript(script);
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteScript(script);
            }
        });
    }

    @Override
    public int getItemCount() {
        return scripts.size();
    }

    // No ViewHolder, just an empty adapter class for structure
    public static class MyAdapter extends RecyclerView.ViewHolder {
        public MyAdapter(@NonNull View itemView) {
            super(itemView);
        }
    }
}
/*
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_remote_script, parent, false);
        }

        Script script = scripts.get(position);

        TextView textScriptName = view.findViewById(R.id.text_remote_script_name);
        TextView textScriptId = view.findViewById(R.id.text_remote_script_id);
        TextView textLastModified = view.findViewById(R.id.text_remote_script_modified);
        Button btnDownload = view.findViewById(R.id.btn_download_script);
        Button btnDelete = view.findViewById(R.id.btn_delete_script);

        textScriptName.setText(script.getScriptName());
        textScriptId.setText("ID: " + script.getScriptId());
        textLastModified.setText("Last Modified: " + script.getLastModified());

        // Set up button click listeners
        btnDownload.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadScript(script);
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteScript(script);
            }
        });

        return view;
    }
}
*/