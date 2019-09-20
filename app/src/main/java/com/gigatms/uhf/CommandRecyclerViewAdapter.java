package com.gigatms.uhf;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class CommandRecyclerViewAdapter extends RecyclerView.Adapter<GeneralViewHolder> {
    private static final String TAG = CommandRecyclerViewAdapter.class.getSimpleName();
    private List<GeneralCommandItem> mCommands;

    CommandRecyclerViewAdapter() {
        mCommands = new ArrayList<>();
    }

    @NonNull
    @Override
    public GeneralViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.command_table, viewGroup, false);
        return new GeneralViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GeneralViewHolder viewHolder, int position) {
        GeneralCommandItem command = mCommands.get(position);
        command.setPosition(position);
        viewHolder.bindView(command);
    }

    @Override
    public int getItemCount() {
        return mCommands.size();
    }

    void add(GeneralCommandItem command) {
        mCommands.add(command);
    }

    public void clear() {
        notifyItemRangeRemoved(0, getItemCount());
        mCommands.clear();
    }
}
