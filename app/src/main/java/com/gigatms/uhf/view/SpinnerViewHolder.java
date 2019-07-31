package com.gigatms.uhf.view;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.gigatms.uhf.R;
import com.gigatms.uhf.command.Command;
import com.gigatms.uhf.command.SpinnerCommand;

public class SpinnerViewHolder extends BaseCommandViewHolder {
    private Spinner mSpinner;

    public SpinnerViewHolder(@NonNull View itemView) {
        super(itemView);
        mSpinner = itemView.findViewById(R.id.spinner);
    }

    public void bindView(final Command command) {
        super.bindView(command);
        SpinnerCommand spinnerCommand = (SpinnerCommand) command;
        mBtnLeft.setVisibility(spinnerCommand.hasLeftBtn() ? View.VISIBLE : View.GONE);
        final ArrayAdapter<Object> arrayAdapter = new ArrayAdapter<>(itemView.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                spinnerCommand.getEnum());
        mSpinner.setAdapter(arrayAdapter);
        mSpinner.setSelection(arrayAdapter.getPosition(spinnerCommand.getSelected()), true);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinnerCommand.setSelected((Enum) arrayAdapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
