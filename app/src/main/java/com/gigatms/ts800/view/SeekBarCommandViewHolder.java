package com.gigatms.ts800.view;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.gigatms.ts800.R;
import com.gigatms.ts800.command.Command;
import com.gigatms.ts800.command.SeekBarCommand;

public class SeekBarCommandViewHolder extends BaseCommandViewHolder {

    private static final String TAG = SeekBarCommandViewHolder.class.getSimpleName();
    private TextView mTvValue;
    private SeekBar mSeekBar;
    private SeekBarCommand mCommand;

    public SeekBarCommandViewHolder(@NonNull View itemView) {
        super(itemView);
        mTvValue = itemView.findViewById(R.id.tv_value);
        mSeekBar = itemView.findViewById(R.id.seekBar);
    }

    public void bindView(final Command command) {
        super.bindView(command);
        mCommand = (SeekBarCommand) command;
        mBtnLeft.setVisibility(mCommand.hasLeftBtn() ? View.VISIBLE : View.GONE);
        mTvValue.setText(String.valueOf(mCommand.getMinValue()));
        mSeekBar.setMax(mCommand.getMaxValue() - mCommand.getMinValue());
        setSeekBarValue(mCommand.getSelected());
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mTvValue.setText(String.valueOf(progress + mCommand.getMinValue()));
                    mCommand.setSelected((byte) (progress + mCommand.getMinValue()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mCommand.setOnGetValue(new SeekBarCommand.OnGetValues() {
            @Override
            public void onGetValue(final int value) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        setSeekBarValue(value);
                        mCommand.setSelected(value);
                    }
                });
            }
        });
    }

    private void setSeekBarValue(int selected) {
        mSeekBar.setProgress(selected - mCommand.getMinValue());
        mTvValue.setText(String.valueOf(selected));
    }
}
