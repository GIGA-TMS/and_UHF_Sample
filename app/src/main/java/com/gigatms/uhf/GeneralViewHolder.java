package com.gigatms.uhf;

import android.text.Editable;
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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gigatms.uhf.paramsData.ASCIIEditTextParamData;
import com.gigatms.uhf.paramsData.CheckBoxParamData;
import com.gigatms.uhf.paramsData.CheckboxListParamData;
import com.gigatms.uhf.paramsData.EditTextParamData;
import com.gigatms.uhf.paramsData.EditTextTitleParamData;
import com.gigatms.uhf.paramsData.EventTypesParamData;
import com.gigatms.uhf.paramsData.InterchangeableParamData;
import com.gigatms.uhf.paramsData.ParamData;
import com.gigatms.uhf.paramsData.SeekBarParamData;
import com.gigatms.uhf.paramsData.SeekBarTitleParamData;
import com.gigatms.uhf.paramsData.SpinnerParamData;
import com.gigatms.uhf.paramsData.SpinnerTitleParamData;
import com.gigatms.uhf.paramsData.TwoSpinnerParamData;
import com.gigatms.uhf.paramsData.TwoSpinnerTitleParamData;
import com.gigatms.uhf.view.ASCIIEditText;
import com.gigatms.tools.GLog;

import java.util.List;

public class GeneralViewHolder extends RecyclerView.ViewHolder {
    private static final String TAG = GeneralViewHolder.class.getSimpleName();
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
                setCheckboxListView((CheckboxListParamData) viewData);
                break;
            case CHECKBOX:
                setCheckboxView((CheckBoxParamData) viewData);
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
            case SPINNER_WITH_TITLE:
                setSpinnerTitleView((SpinnerTitleParamData) viewData);
                break;
            case SEEK_BAR_WITH_TITLE:
                setSeekBarTitleView((SeekBarTitleParamData) viewData);
                break;
            case TWO_SPINNER_WITH_TITLE:
                setTwoSpinnerTitleView((TwoSpinnerTitleParamData) viewData);
                break;
            case EDIT_TEXT_WITH_TITLE:
                setEditTextTitleView((EditTextTitleParamData) viewData);
                break;
            case TS100_EVENT_TYPES_PARAM:
                setEventTypeView((EventTypesParamData) viewData);
                break;
            case INTERCHANGEABLE_VIEW:
                setInterchangeableView((InterchangeableParamData) viewData);
                break;
            case ASCII_EDIT_TEXT:
                setAsciiEditTextView((ASCIIEditTextParamData) viewData);
                break;
            default:
                break;
        }
    }

    private void setEditTextTitleView(EditTextTitleParamData editTextTitleParamData) {
        EditText editText = new EditText(itemView.getContext());
        TextView title = new TextView(itemView.getContext());
        title.setText(editTextTitleParamData.getTitle());
        editText.setHint(editTextTitleParamData.getHint());
        if (editText.getTag() instanceof TextWatcher) {
            editText.removeTextChangedListener((TextWatcher) editText.getTag());
        }
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                editTextTitleParamData.setSelected(editable.toString());
            }
        };
        editText.addTextChangedListener(watcher);
        editText.setTag(watcher);
        editText.setText(editTextTitleParamData.getSelected());

        TableRow tableRow = new TableRow(itemView.getContext());
        TableRow.LayoutParams cellParams1 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams1.weight = 1;
        cellParams1.leftMargin = 10;

        TableRow.LayoutParams cellParams2 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams2.weight = 3;
        title.setLayoutParams(cellParams1);
        tableRow.addView(title);
        editText.setLayoutParams(cellParams2);
        tableRow.addView(editText);
        mTableLayout.addView(tableRow);
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
                    seekBarParamData.setSelected(progress + seekBarParamData.getMinValue());
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
        cellParams1.weight = 25;
        cellParams1.bottomMargin = 16;
        seekBar.setLayoutParams(cellParams1);
        TableRow.LayoutParams cellParams2 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams2.weight = 3;
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
                editTextViewData.setSelected(editable.toString());
            }
        };
        editText.addTextChangedListener(watcher);
        editText.setTag(watcher);
        editText.setText(editTextViewData.getSelected());
        mTableLayout.addView(editText);
    }

    private void setAsciiEditTextView(ASCIIEditTextParamData asciiEditTextParamData) {
        ASCIIEditText asciiEditText = new ASCIIEditText(itemView.getContext());
        asciiEditText.setHint(asciiEditTextParamData.getHint());
        asciiEditText.setOnTextChangedListener(text -> {
            GLog.v(TAG, "onTextChanged" + text);
            asciiEditTextParamData.setSelected(text);
        });
        asciiEditText.setText(asciiEditTextParamData.getSelected());
        mTableLayout.addView(asciiEditText);
    }

    private void setCheckboxListView(CheckboxListParamData checkboxListParamData) {
        for (final Object object : checkboxListParamData.getDataSet()) {
            final CheckBox checkBox = new CheckBox(itemView.getContext());
            checkBox.setText(((Enum) object).name());
            int ordinal = ((Enum) object).ordinal();
            checkBox.setId(ordinal);
            checkBox.setChecked(checkboxListParamData.isOrdinalSelected(ordinal));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    checkboxListParamData.addSelectedOrdinal(checkBox.getId());
                } else {
                    checkboxListParamData.removeSelectedOrdinal(checkBox.getId());
                }
            });
            mTableLayout.addView(checkBox);
        }
    }

    private void setCheckboxView(CheckBoxParamData checkBoxParamData) {
        final CheckBox checkBox = new CheckBox(itemView.getContext());
        checkBox.setText(checkBoxParamData.getTitle());
        checkBox.setChecked(checkBoxParamData.isChecked());
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> checkBoxParamData.setChecked(isChecked));
        mTableLayout.addView(checkBox);
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

    private void setSpinnerTitleView(SpinnerTitleParamData viewData) {
        TextView textView = new TextView(itemView.getContext());
        textView.setText(viewData.getTitle());
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
        TableRow tableRow = new TableRow(itemView.getContext());
        TableRow.LayoutParams cellParams1 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams1.weight = 1;
        cellParams1.leftMargin = 10;

        TableRow.LayoutParams cellParams2 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams2.weight = 3;
        textView.setLayoutParams(cellParams1);
        tableRow.addView(textView);
        spinner.setLayoutParams(cellParams2);
        tableRow.addView(spinner);
        mTableLayout.addView(tableRow);
    }

    private void setSeekBarTitleView(SeekBarTitleParamData seekBarTitleParamData) {
        SeekBar seekBar = new SeekBar(itemView.getContext());
        TextView value = new TextView(itemView.getContext());
        TextView title = new TextView(itemView.getContext());
        title.setText(seekBarTitleParamData.getTitle());
        value.setText(String.valueOf(seekBarTitleParamData.getMinValue()));
        seekBar.setMax(seekBarTitleParamData.getMaxValue() - seekBarTitleParamData.getMinValue());
        seekBar.setProgress(seekBarTitleParamData.getSelected() - seekBarTitleParamData.getMinValue());
        value.setText(String.valueOf(seekBarTitleParamData.getSelected()));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    value.setText(String.valueOf(progress + seekBarTitleParamData.getMinValue()));
                    seekBarTitleParamData.setSelected(progress + seekBarTitleParamData.getMinValue());
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
        cellParams1.topMargin = 24;
        cellParams1.weight = 11;
        cellParams1.bottomMargin = 16;
        seekBar.setLayoutParams(cellParams1);
        TableRow.LayoutParams cellParams2 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams2.topMargin = 24;
        cellParams2.weight = 1;
        cellParams2.bottomMargin = 16;
        value.setLayoutParams(cellParams2);
        TableRow.LayoutParams cellParams3 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams3.topMargin = 24;
        cellParams3.weight = 4;
        cellParams3.leftMargin = 16;
        title.setLayoutParams(cellParams3);

        TableRow tableRow = new TableRow(itemView.getContext());
        tableRow.addView(title);
        tableRow.addView(seekBar);
        tableRow.addView(value);
        mTableLayout.addView(tableRow);
    }

    private void setTwoSpinnerTitleView(TwoSpinnerTitleParamData twoSpinnerTitleParamData) {
        TextView title = new TextView(itemView.getContext());
        title.setText(twoSpinnerTitleParamData.getTitle());
        Spinner mFirstSpinner = new Spinner(itemView.getContext());
        Spinner mSecondSpinner = new Spinner(itemView.getContext());
        final ArrayAdapter<Enum> firstArrayAdapter = new ArrayAdapter<>(itemView.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                twoSpinnerTitleParamData.getFirstEnums());
        mFirstSpinner.setAdapter(firstArrayAdapter);
        mFirstSpinner.setSelection(firstArrayAdapter.getPosition(twoSpinnerTitleParamData.getFirstSelected()), true);

        final ArrayAdapter<Enum> secondArrayAdapter = new ArrayAdapter<>(itemView.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                twoSpinnerTitleParamData.getSecondEnums());
        mSecondSpinner.setAdapter(secondArrayAdapter);
        mSecondSpinner.setSelection(secondArrayAdapter.getPosition(twoSpinnerTitleParamData.getSecondSelected()), true);

        mFirstSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                twoSpinnerTitleParamData.setFirstSelected((Enum) mFirstSpinner.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSecondSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                twoSpinnerTitleParamData.setSecondSelected((Enum) mSecondSpinner.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        TableRow tableRow = new TableRow(itemView.getContext());
        TableRow.LayoutParams cellParams1 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams1.weight = 6;
        TableRow.LayoutParams cellParams2 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams2.weight = 4;
        cellParams2.leftMargin = 16;
        title.setLayoutParams(cellParams2);
        tableRow.addView(title);
        mFirstSpinner.setLayoutParams(cellParams1);
        tableRow.addView(mFirstSpinner);
        mSecondSpinner.setLayoutParams(cellParams1);
        tableRow.addView(mSecondSpinner);
        mTableLayout.addView(tableRow);
    }

    private void setEventTypeView(EventTypesParamData eventTypeParamData) {
        Spinner eventTypes = new Spinner(itemView.getContext());
        final ArrayAdapter<String> firstArrayAdapter = new ArrayAdapter<>(itemView.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                eventTypeParamData.getFirstChoices());
        eventTypes.setAdapter(firstArrayAdapter);

        eventTypes.setSelection(firstArrayAdapter.getPosition(eventTypeParamData.getFirstSelect()), true);

        eventTypes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                eventTypeParamData.setFirstSelect((String) eventTypes.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mTableLayout.addView(eventTypes);


        if (eventTypeParamData.getMiddleChoices() != null) {
            Spinner middleSpinner = new Spinner(itemView.getContext());
            final ArrayAdapter<String> middleAdapter = new ArrayAdapter<>(itemView.getContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    eventTypeParamData.getMiddleChoices());
            middleSpinner.setAdapter(middleAdapter);

            middleSpinner.setSelection(middleAdapter.getPosition(eventTypeParamData.getMiddleSelect()), true);

            middleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    eventTypeParamData.setMiddleSelect((String) middleSpinner.getItemAtPosition(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            mTableLayout.addView(middleSpinner);
        }

        for (final String data : eventTypeParamData.getLastChoices()) {
            final CheckBox checkBox = new CheckBox(itemView.getContext());
            checkBox.setText(data);
            checkBox.setChecked(eventTypeParamData.getLastSelect().contains(data));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    eventTypeParamData.getLastSelect().add(checkBox.getText().toString());
                } else {
                    eventTypeParamData.getLastSelect().remove(checkBox.getText().toString());
                }
            });
            mTableLayout.addView(checkBox);
        }
    }

    private void setInterchangeableView(InterchangeableParamData interchangeableParamData) {
        TextView textView = new TextView(itemView.getContext());
        textView.setText(interchangeableParamData.getTitle());
        Spinner spinner = new Spinner(itemView.getContext());
        final ArrayAdapter<Enum> arrayAdapter = new ArrayAdapter<>(itemView.getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                interchangeableParamData.getDataArray());
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(arrayAdapter.getPosition(interchangeableParamData.getSelected()), true);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                interchangeableParamData.setSelected(arrayAdapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        TableRow tableRow = new TableRow(itemView.getContext());
        TableRow.LayoutParams cellParams1 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams1.weight = 1;
        cellParams1.leftMargin = 10;

        TableRow.LayoutParams cellParams2 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams2.weight = 3;
        textView.setLayoutParams(cellParams1);
        tableRow.addView(textView);
        spinner.setLayoutParams(cellParams2);
        tableRow.addView(spinner);
        mTableLayout.addView(tableRow);
        List<ParamData> paramDataList = interchangeableParamData.getParamData();
        for (ParamData paramData : paramDataList) {
            setView(paramData);
        }
    }
}
