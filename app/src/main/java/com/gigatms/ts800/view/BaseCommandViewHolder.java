package com.gigatms.ts800.view;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gigatms.ts800.R;
import com.gigatms.ts800.command.Command;

public class BaseCommandViewHolder extends RecyclerView.ViewHolder {
    private Button mBtnRight;
    private TextView mTvTitle;
    Button mBtnLeft;

    public BaseCommandViewHolder(@NonNull View itemView) {
        super(itemView);
        mBtnLeft = itemView.findViewById(R.id.btn_read);
        mBtnRight = itemView.findViewById(R.id.btn_write);
        mTvTitle = itemView.findViewById(R.id.tv_title);
    }

    public void bindView(final Command command) {
        mTvTitle.setText(command.getTitle());

        mBtnRight.setText(command.getRightBtnName());
        mBtnLeft.setText(command.getLeftBtnName());

        mBtnRight.setVisibility(command.hasRightBtn() ? View.VISIBLE : View.GONE);
        mBtnLeft.setVisibility(command.hasLeftBtn() ? View.VISIBLE : View.GONE);

        mBtnRight.setOnClickListener(command.getRightOnClickListener());
        mBtnLeft.setOnClickListener(command.getLeftOnClickListener());
    }
}
