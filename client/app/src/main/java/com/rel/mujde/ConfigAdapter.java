package com.rel.mujde;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ConfigAdapter extends RecyclerView.Adapter<ConfigAdapter.ViewHolder> {
    private List<Config> configs = new ArrayList<>();
    private OnConfigClickListener listener;

    public static class Config {
        String packageName;
        String message;

        Config(String packageName, String message) {
            this.packageName = packageName != null ? packageName : "";
            this.message = message != null ? message : "";
        }
    }

    public interface OnConfigClickListener {
        void onConfigLongClick(Config config);
    }

    public void setOnConfigClickListener(OnConfigClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_config, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Config config = configs.get(position);
        holder.packageName.setText(config.packageName);
        holder.message.setText(config.message);

        // Animate item
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(
            holder.itemView.getContext(),
            android.R.anim.slide_in_left
        ));
        
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onConfigLongClick(config);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return configs.size();
    }

    public List<Config> getConfigs() {
        return new ArrayList<>(configs);
    }

    public void setConfigs(List<Config> newConfigs) {
        if (newConfigs != null) {
            configs = new ArrayList<>(newConfigs);
        } else {
            configs = new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView packageName;
        TextView message;

        ViewHolder(View view) {
            super(view);
            packageName = view.findViewById(R.id.packageName);
            message = view.findViewById(R.id.message);
        }
    }
}
