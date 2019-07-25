package com.gigatms.ts800.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;

import com.gigatms.ts800.R;
import com.gigatms.ts800.command.CheckboxCommand;
import com.gigatms.ts800.command.Command;

import java.util.HashSet;
import java.util.Set;

public class CheckboxCommandViewHolder extends BaseCommandViewHolder {

    private TableLayout mTableLayout;
    private CheckboxCommand mCommand;

    public CheckboxCommandViewHolder(@NonNull View itemView) {
        super(itemView);
        mTableLayout = itemView.findViewById(R.id.table);
    }

    public void bindView(Command command, Context context) {
        super.bindView(command);
        mCommand = (CheckboxCommand) command;
        mTableLayout.removeAllViews();
        final Set<Integer> set = new HashSet<>();

        for (final Object object : mCommand.getSet()) {
            LayoutParams rowParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            TableRow tableRow = new TableRow(context);
            tableRow.setLayoutParams(rowParams);
            final CheckBox checkBox = new CheckBox(context);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        set.add(checkBox.getId());
                    } else {
                        set.remove(checkBox.getId());
                    }
                    mCommand.setSelected(set);
                }
            });
            checkBox.setText(((Enum) object).name());
            checkBox.setId(((Enum) object).ordinal());
            tableRow.addView(checkBox);
            mTableLayout.addView(tableRow);
        }
        setSelectedCheckbox(mCommand.getSelected());

        mCommand.setOnGetValue(new CheckboxCommand.OnGetValue() {
            @Override
            public void onGetValue(final Set values) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mCommand.setSelected(values);
                        setSelectedCheckbox(values);
                    }
                });
            }
        });
    }

    void setSelectedCheckbox(Set values) {
        Set<Integer> ordinals = new HashSet<>();
        for (Object object : values) {
            ordinals.add(((Enum) object).ordinal());
        }
        for (int i = 0; i < mTableLayout.getChildCount(); i++) {
            TableRow tableRow = (TableRow) mTableLayout.getChildAt(i);
            CheckBox checkbox = (CheckBox) tableRow.getChildAt(0);
            int id = checkbox.getId();
            checkbox.setChecked(ordinals.contains(id));
        }
    }
}
