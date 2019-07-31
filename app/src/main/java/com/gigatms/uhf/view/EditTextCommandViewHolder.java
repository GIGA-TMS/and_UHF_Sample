package com.gigatms.uhf.view;

import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.gigatms.uhf.R;
import com.gigatms.uhf.command.Command;
import com.gigatms.uhf.command.EditTextCommand;

public class EditTextCommandViewHolder extends BaseCommandViewHolder {

    private EditText mEditText;

    public EditTextCommandViewHolder(@NonNull View itemView) {
        super(itemView);
        mEditText = itemView.findViewById(R.id.editText);
    }

    public void bindView(final Command command) {
        super.bindView(command);
        EditTextCommand editTextCommand = (EditTextCommand) command;
        mBtnLeft.setText(editTextCommand.getLeftBtnName());
        mEditText.setHint(editTextCommand.getHint());
        mEditText.setText(editTextCommand.getSelected());
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editTextCommand.setSelected(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}
