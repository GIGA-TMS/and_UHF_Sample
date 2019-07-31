package com.gigatms.uhf.command;

import com.gigatms.tools.GLog;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static com.gigatms.uhf.CommandRecyclerViewAdapter.TABLE;

public class CheckboxCommand<E extends Enum<E>> extends Command {
    private static final String TAG = CheckboxCommand.class.getSimpleName();
    private Set<E> mData;
    private Set<Integer> mSelectedOrdinal;

    public CheckboxCommand(String title, Class<E> enumData) {
        super(title);
        mData = EnumSet.allOf(enumData);
        mSelectedOrdinal = new HashSet<>();
    }

    public CheckboxCommand(String title, String leftBtnName, String rightBtnName, Class<E> enumData) {
        super(title, leftBtnName, rightBtnName);
        mData = EnumSet.allOf(enumData);
        mSelectedOrdinal = new HashSet<>();
    }

    @Override
    public int getViewType() {
        return TABLE;
    }

    public Set<E> getDataSet() {
        return mData;
    }

    public void setSelected(Set<E> set) {
        GLog.d(TAG, "Selected " + Arrays.toString(set.toArray()));
        mSelectedOrdinal = new HashSet<>();
        for (E e : set) {
            GLog.d(TAG, e.name() + " ordinal " + e.ordinal());
            mSelectedOrdinal.add(e.ordinal());
        }
        GLog.d(TAG, "Selected Ordinal" + Arrays.toString(mSelectedOrdinal.toArray()));
    }

    public Set<E> getSelected() {
        Set<E> set = new HashSet<>();
        for (E e : mData) {
            if (mSelectedOrdinal.contains(e.ordinal())) {
                set.add(e);
            }
        }
        GLog.d(TAG, "Selected " + Arrays.toString(set.toArray()));
        return set;
    }

    public Set<Integer> getSelectedOrdinal() {
        return mSelectedOrdinal;
    }

    public boolean isOrdinalSelected(int ordinal) {
        GLog.d(TAG, ordinal + ": " + mSelectedOrdinal.contains(ordinal));
        return mSelectedOrdinal.contains(ordinal);
    }

    public void addSelectedOrdinal(int ordinal) {
        mSelectedOrdinal.add(ordinal);
    }

    public void removeSelectedOrdinal(int ordinal) {
        getSelectedOrdinal().remove(ordinal);
    }
}
