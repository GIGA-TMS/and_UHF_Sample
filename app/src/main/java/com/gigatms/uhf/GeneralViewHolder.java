package com.gigatms.uhf;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.gigatms.uhf.paramsData.CheckboxParamData;
import com.gigatms.uhf.paramsData.EditTextParamData;
import com.gigatms.uhf.paramsData.ParamData;
import com.gigatms.uhf.paramsData.SeekBarParamData;
import com.gigatms.uhf.paramsData.SpinnerParamData;
import com.gigatms.uhf.paramsData.TwoSpinnerParamData;

public class GeneralViewHolder extends RecyclerView.ViewHolder {
    private Button mBtnRight;
    private TextView mTvTitle;
    private Button mBtnLeft;
    private TableLayout mTableLayout;

    public GeneralViewHolder(@NonNull View itemView) {
        super(itemView);
        mBtnLeft = itemView.findViewById(R.id.btn_read);
        mBtnRight = itemView.findViewById(R.id.btn_write);
        mTvTitle = itemView.findViewById(R.id.tv_title);
        mTableLayout = itemView.findViewById(R.id.table);
    }

    public void bindView(GeneralCommandItem command) {
        mTvTitle.setText(command.getTitle());

        mBtnRight.setText(command.getRightBtnName());
        mBtnLeft.setText(command.getLeftBtnName());

        mBtnRight.setVisibility(command.hasRightBtn() ? View.VISIBLE : View.GONE);
        mBtnLeft.setVisibility(command.hasLeftBtn() ? View.VISIBLE : View.GONE);

        mBtnRight.setOnClickListener(command.getRightOnClickListener());
        mBtnLeft.setOnClickListener(command.getLeftOnClickListener());
        mTableLayout.removeAllViews();
        for (ParamData viewData : command.getViewDataArray()) {
            setView(viewData);
        }
    }

    void setView(ParamData viewData) {
        switch (viewData.getViewDataType()) {
            case SPINNER:
                setSpinnerView((SpinnerParamData) viewData);
                break;
            case CHECKBOX_LIST:
                setCheckboxView((CheckboxParamData) viewData);
                break;
            case EDIT_TEXT:
                setEditTextView((EditTextParamData) viewData);
                break;
            case SEEK_BAR:
                setSeekBarView((SeekBarParamData) viewData);
                break;
            case TWO_SPINNER:
                setTwoSpinnerView((TwoSpinnerParamData) viewData);
                break;
            default:
                break;
        }
    }

    private void setTwoSpinnerView(TwoSpinnerParamData twoSpinnerParamData) {
        Spinner mFirstSpinner = new Spinner(itemView.getContext());
        Spinner mSecondSpinner = new Spinner(itemView.getContext());
        final ArrayAdapter<Enum> firstArrayAdapter = new ArrayAdapter<>(itemView.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                twoSpinnerParamData.getFirstEnums());
        mFirstSpinner.setAdapter(firstArrayAdapter);
        mFirstSpinner.setSelection(firstArrayAdapter.getPosition(twoSpinnerParamData.getFirstSelected()), true);

        final ArrayAdapter<Enum> secondArrayAdapter = new ArrayAdapter<>(itemView.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                twoSpinnerParamData.getSecondEnums());
        mSecondSpinner.setAdapter(secondArrayAdapter);
        mSecondSpinner.setSelection(secondArrayAdapter.getPosition(twoSpinnerParamData.getSecondSelected()), true);

        mFirstSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                twoSpinnerParamData.setFirstSelected((Enum) mFirstSpinner.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSecondSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                twoSpinnerParamData.setSecondSelected((Enum) mSecondSpinner.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        TableRow tableRow = new TableRow(itemView.getContext());
        TableRow.LayoutParams cellParams1 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams1.weight = 1;
        mFirstSpinner.setLayoutParams(cellParams1);
        tableRow.addView(mFirstSpinner);
        mSecondSpinner.setLayoutParams(cellParams1);
        tableRow.addView(mSecondSpinner);
        mTableLayout.addView(tableRow);
    }

    private void setSeekBarView(SeekBarParamData seekBarParamData) {
        SeekBar seekBar = new SeekBar(itemView.getContext());
        TextView textView = new TextView(itemView.getContext());
        textView.setText(String.valueOf(seekBarParamData.getMinValue()));
        seekBar.setMax(seekBarParamData.getMaxValue() - seekBarParamData.getMinValue());
        seekBar.setProgress(seekBarParamData.getSelected() - seekBarParamData.getMinValue());
        textView.setText(String.valueOf(seekBarParamData.getSelected()));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    textView.setText(String.valueOf(progress + seekBarParamData.getMinValue()));
                    seekBarParamData.setSelected((byte) (progress + seekBarParamData.getMinValue()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        TableRow.LayoutParams cellParams1 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams1.weight = 13;
        cellParams1.bottomMargin = 16;
        seekBar.setLayoutParams(cellParams1);
        TableRow.LayoutParams cellParams2 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams2.weight = 1;
        cellParams2.bottomMargin = 20;
        textView.setLayoutParams(cellParams2);

        TableRow tableRow = new TableRow(itemView.getContext());
        tableRow.addView(seekBar);
        tableRow.addView(textView);
        mTableLayout.addView(tableRow);
    }

    private void setEditTextView(EditTextParamData editTextViewData) {
        EditText editText = new EditText(itemView.getContext());
        editText.setHint(editTextViewData.getHint());
        if (editText.getTag() instanceof TextWatcher) {
            editText.removeTextChangedListener((TextWatcher) editText.getTag());
        }
        editText.removeTextChangedListener(null);
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!TextUtils.isEmpty(editable.toString())) {
                    editTextViewData.setSelected(editable.toString());
                }
            }
        };
        editText.addTextChangedListener(watcher);
        editText.setTag(watcher);
        editText.setText(editTextViewData.getSelected());
        mTableLayout.addView(editText);
    }

    private void setCheckboxView(CheckboxParamData checkboxParamData) {
        for (final Object object : checkboxParamData.getDataSet()) {
            final CheckBox checkBox = new CheckBox(itemView.getContext());
            checkBox.setText(((Enum) object).name());
            int ordinal = ((Enum) object).ordinal();
            checkBox.setId(ordinal);
            checkBox.setChecked(checkboxParamData.isOrdinalSelected(ordinal));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    checkboxParamData.addSelectedOrdinal(checkBox.getId());
                } else {
                    checkboxParamData.removeSelectedOrdinal(checkBox.getId());
                }
            });
            mTableLayout.addView(checkBox);
        }
    }

    private void setSpinnerView(SpinnerParamData viewData) {
        Spinner spinner = new Spinner(itemView.getContext());
        final ArrayAdapter<Enum> arrayAdapter = new ArrayAdapter<>(itemView.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                viewData.getDataArray());
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(arrayAdapter.getPosition(viewData.getSelected()), true);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewData.setSelected(arrayAdapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mTableLayout.addView(spinner);
    }
}
