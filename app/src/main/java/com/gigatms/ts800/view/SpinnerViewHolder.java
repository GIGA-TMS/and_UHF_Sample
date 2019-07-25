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
import com.gigatms.ts800.command.SpinnerCommand;

public class SpinnerViewHolder extends BaseCommandViewHolder {
    private Spinner mSpinner;
    private SpinnerCommand mCommand;

    public SpinnerViewHolder(@NonNull View itemView) {
        super(itemView);
        mSpinner = itemView.findViewById(R.id.spinner);
    }

    public void bindView(final Command command, Context context) {
        super.bindView(command);
        mCommand = (SpinnerCommand) command;
        mBtnLeft.setVisibility(mCommand.hasLeftBtn() ? View.VISIBLE : View.GONE);
        final ArrayAdapter<Object> arrayAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item,
                mCommand.getEnum().getEnumConstants());
        mSpinner.setAdapter(arrayAdapter);
        mSpinner.setSelection(arrayAdapter.getPosition(mCommand.getSelected()), true);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCommand.setSelected((Enum) arrayAdapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mCommand.setOnGetValue(new SpinnerCommand.OnGetValue() {
            @Override
            public void onGetValue(final Enum value) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mSpinner.setSelection(arrayAdapter.getPosition(value), true);
                        mCommand.setSelected(value);
                    }
                });
            }
        });
    }
}
