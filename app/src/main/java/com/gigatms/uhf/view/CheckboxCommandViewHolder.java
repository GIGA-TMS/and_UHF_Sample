package com.gigatms.uhf.view;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TableLayout;

import com.gigatms.uhf.R;
import com.gigatms.uhf.command.CheckboxCommand;
import com.gigatms.uhf.command.Command;

public class CheckboxCommandViewHolder extends BaseCommandViewHolder {

    private static final String TAG = CheckboxCommandViewHolder.class.getSimpleName();
    private TableLayout mTableLayout;

    public CheckboxCommandViewHolder(@NonNull View itemView) {
        super(itemView);
        mTableLayout = itemView.findViewById(R.id.table);
    }

    public void bindView(Command command) {
        super.bindView(command);
        CheckboxCommand checkboxCommand = (CheckboxCommand) command;
        mTableLayout.removeAllViews();

        for (final Object object : checkboxCommand.getDataSet()) {
            final CheckBox checkBox = new CheckBox(itemView.getContext());
            checkBox.setText(((Enum) object).name());
            int ordinal = ((Enum) object).ordinal();
            checkBox.setId(ordinal);
            checkBox.setChecked(checkboxCommand.isOrdinalSelected(ordinal));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    checkboxCommand.addSelectedOrdinal(checkBox.getId());
                } else {
                    checkboxCommand.removeSelectedOrdinal(checkBox.getId());
                }
            });
            mTableLayout.addView(checkBox);
        }
    }
}
