package com.gigatms.ts800.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.gigatms.ts800.R;
import com.gigatms.ts800.command.Command;
import com.gigatms.ts800.command.TwoSpinnerCommand;

public class TwoSpinnerCommandViewHolder extends BaseCommandViewHolder {
    private Spinner mFirstSpinner;
    private Spinner mSecondSpinner;
    private TwoSpinnerCommand mCommand;
    private String TAG = TwoSpinnerCommandViewHolder.class.getSimpleName();

    public TwoSpinnerCommandViewHolder(@NonNull View itemView) {
        super(itemView);
        mFirstSpinner = itemView.findViewById(R.id.spn_first);
        mSecondSpinner = itemView.findViewById(R.id.spn_second);
    }

    public void bindView(Command command, final Context context) {
        super.bindView(command);
        mCommand = (TwoSpinnerCommand) command;
        final ArrayAdapter<Enum> firstArrayAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item,
                mCommand.getFirstEnums());
        mFirstSpinner.setAdapter(firstArrayAdapter);
        mFirstSpinner.setSelection(firstArrayAdapter.getPosition(mCommand.getFirstSelected()), true);

        final ArrayAdapter<Enum> secondArrayAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item,
                mCommand.getSecondEnums());
        mSecondSpinner.setAdapter(secondArrayAdapter);
        mSecondSpinner.setSelection(secondArrayAdapter.getPosition(mCommand.getSecondSelected()), true);

        mCommand.setOnGetValues(new TwoSpinnerCommand.OnGetValues() {
            @Override
            public void onGetValues(final Enum first, final Enum second) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mFirstSpinner.setSelection(firstArrayAdapter.getPosition(first));
                        mSecondSpinner.setSelection(secondArrayAdapter.getPosition(second));
                    }
                });
            }
        });

        mFirstSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCommand.setFirstSelected((Enum) mFirstSpinner.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSecondSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCommand.setSecondSelected((Enum) mSecondSpinner.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
