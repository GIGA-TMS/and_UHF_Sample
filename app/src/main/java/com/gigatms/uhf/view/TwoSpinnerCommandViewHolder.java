package com.gigatms.uhf.view;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.gigatms.uhf.R;
import com.gigatms.uhf.command.Command;
import com.gigatms.uhf.command.TwoSpinnerCommand;

public class TwoSpinnerCommandViewHolder extends BaseCommandViewHolder {
    private Spinner mFirstSpinner;
    private Spinner mSecondSpinner;
    private String TAG = TwoSpinnerCommandViewHolder.class.getSimpleName();

    public TwoSpinnerCommandViewHolder(@NonNull View itemView) {
        super(itemView);
        mFirstSpinner = itemView.findViewById(R.id.spn_first);
        mSecondSpinner = itemView.findViewById(R.id.spn_second);
    }

    public void bindView(Command command) {
        super.bindView(command);
        TwoSpinnerCommand twoSpinnerCommand = (TwoSpinnerCommand) command;
        final ArrayAdapter<Enum> firstArrayAdapter = new ArrayAdapter<>(itemView.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                twoSpinnerCommand.getFirstEnums());
        mFirstSpinner.setAdapter(firstArrayAdapter);
        mFirstSpinner.setSelection(firstArrayAdapter.getPosition(twoSpinnerCommand.getFirstSelected()), true);

        final ArrayAdapter<Enum> secondArrayAdapter = new ArrayAdapter<>(itemView.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                twoSpinnerCommand.getSecondEnums());
        mSecondSpinner.setAdapter(secondArrayAdapter);
        mSecondSpinner.setSelection(secondArrayAdapter.getPosition(twoSpinnerCommand.getSecondSelected()), true);

        mFirstSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                twoSpinnerCommand.setFirstSelected((Enum) mFirstSpinner.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSecondSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                twoSpinnerCommand.setSecondSelected((Enum) mSecondSpinner.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
