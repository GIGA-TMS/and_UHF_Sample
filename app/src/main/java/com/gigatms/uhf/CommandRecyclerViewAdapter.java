package com.gigatms.uhf;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gigatms.uhf.command.Command;
import com.gigatms.uhf.view.BaseCommandViewHolder;
import com.gigatms.uhf.view.CheckboxCommandViewHolder;
import com.gigatms.uhf.view.EditTextCommandViewHolder;
import com.gigatms.uhf.view.SeekBarCommandViewHolder;
import com.gigatms.uhf.view.SpinnerViewHolder;
import com.gigatms.uhf.view.TwoSpinnerCommandViewHolder;

import java.util.ArrayList;
import java.util.List;

public class CommandRecyclerViewAdapter extends RecyclerView.Adapter<BaseCommandViewHolder> {
    public static final int BASE = 0;
    public static final int EDIT_TEXT = 1;
    public static final int SPINNER = 2;
    public static final int TWO_SPINNER = 3;
    public static final int TABLE = 4;
    public static final int SEEK_BAR = 5;
    private static final String TAG = CommandRecyclerViewAdapter.class.getSimpleName();
    private List<Command> mCommands;

    CommandRecyclerViewAdapter() {
        mCommands = new ArrayList<>();
    }

    @NonNull
    @Override
    public BaseCommandViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        switch (viewType) {
            case SEEK_BAR:
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.command_seekbar, viewGroup, false);
                return new SeekBarCommandViewHolder(view);
            case EDIT_TEXT:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.command_edittext, viewGroup, false);
                return new EditTextCommandViewHolder(view);
            case SPINNER:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.command_spinner, viewGroup, false);
                return new SpinnerViewHolder(view);
            case TWO_SPINNER:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.command_two_spinner, viewGroup, false);
                return new TwoSpinnerCommandViewHolder(view);
            case TABLE:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.command_checkbox, viewGroup, false);
                return new CheckboxCommandViewHolder(view);
            case BASE:
            default:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.command_base, viewGroup, false);
                return new BaseCommandViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BaseCommandViewHolder viewHolder, int position) {
        Command command = mCommands.get(position);
        command.setPosition(position);
        viewHolder.bindView(command);
    }


    @Override
    public int getItemViewType(int position) {
        return mCommands.get(position).getViewType();
    }

    @Override
    public int getItemCount() {
        return mCommands.size();
    }

    void add(Command command) {
        mCommands.add(command);
    }

    public void clear() {
        notifyItemRangeRemoved(0, getItemCount());
        mCommands.clear();
    }
}
