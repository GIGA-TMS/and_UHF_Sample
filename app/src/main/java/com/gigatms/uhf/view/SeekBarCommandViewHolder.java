package com.gigatms.uhf.view;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.gigatms.uhf.R;
import com.gigatms.uhf.command.Command;
import com.gigatms.uhf.command.SeekBarCommand;

public class SeekBarCommandViewHolder extends BaseCommandViewHolder {

    private static final String TAG = SeekBarCommandViewHolder.class.getSimpleName();
    private TextView mTvValue;
    private SeekBar mSeekBar;

    public SeekBarCommandViewHolder(@NonNull View itemView) {
        super(itemView);
        mTvValue = itemView.findViewById(R.id.tv_value);
        mSeekBar = itemView.findViewById(R.id.seekBar);
    }

    public void bindView(final Command command) {
        super.bindView(command);
        SeekBarCommand seekBarCommand = (SeekBarCommand) command;
        mBtnLeft.setVisibility(seekBarCommand.hasLeftBtn() ? View.VISIBLE : View.GONE);
        mTvValue.setText(String.valueOf(seekBarCommand.getMinValue()));
        mSeekBar.setMax(seekBarCommand.getMaxValue() - seekBarCommand.getMinValue());
        setSeekBarValue(seekBarCommand);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mTvValue.setText(String.valueOf(progress + seekBarCommand.getMinValue()));
                    seekBarCommand.setSelected((byte) (progress + seekBarCommand.getMinValue()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setSeekBarValue(SeekBarCommand command) {
        mSeekBar.setProgress(command.getSelected() - command.getMinValue());
        mTvValue.setText(String.valueOf(command.getSelected()));
    }
}
