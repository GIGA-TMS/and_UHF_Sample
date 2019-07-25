package com.gigatms.ts800.view;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.gigatms.ts800.R;
import com.gigatms.ts800.command.Command;
import com.gigatms.ts800.command.EditTextCommand;

public class EditTextCommandViewHolder extends BaseCommandViewHolder {

    private EditText mEditText;
    private EditTextCommand mCommand;

    public EditTextCommandViewHolder(@NonNull View itemView) {
        super(itemView);
        mEditText = itemView.findViewById(R.id.editText);
    }

    public void bindView(final Command command) {
        super.bindView(command);
        mCommand = (EditTextCommand) command;
        mBtnLeft.setText(mCommand.getLeftBtnName());
        mEditText.setHint(mCommand.getHint());
        mEditText.setText(mCommand.getSelected());
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCommand.setSelected(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mCommand.setOnGetValue(new EditTextCommand.OnGetValue() {
            @Override
            public void onGetValue(final String value) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mEditText.setText(value);
                    }
                });
            }
        });
    }
}
