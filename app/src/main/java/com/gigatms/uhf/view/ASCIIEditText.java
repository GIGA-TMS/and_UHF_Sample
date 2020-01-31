package com.gigatms.uhf.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import com.gigatms.tools.GLog;

public class ASCIIEditText extends AppCompatEditText {
    private static final String TAG = ASCIIEditText.class.getSimpleName();
    private TextWatcher mWatcher;

    public ASCIIEditText(Context context) {
        super(context);
        initListener();
    }

    public ASCIIEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initListener();
    }

    public ASCIIEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initListener();
    }

    private void initListener() {
        mWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                GLog.v(TAG, "afterTextChanged" + s.toString());
                if (getText() != null && (getText().toString().contains("\n") || getText().toString().contains("\r"))) {
                    String stringData = getText().toString()
                            .replaceAll("\n", "<CR>")
                            .replaceAll("\r", "<LF>");
                    boolean focused = hasFocus();
                    if (focused) {
                        clearFocus();
                    }
                    setText(stringData);
                    if (focused) {
                        requestFocus();
                    }
                    setSelection(getText().toString().length());
                }
            }
        };
        addTextChangedListener(mWatcher);
    }

    @Override
    public void removeTextChangedListener(TextWatcher watcher) {
        super.removeTextChangedListener(watcher);
        addTextChangedListener(mWatcher);
    }

    public void setText(byte[] ascii) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte value : ascii) {
            switch (value) {
                case 0x00:
                    stringBuilder.append("<NUL>");
                    break;
                case 0x01:
                    stringBuilder.append("<SOH>");
                    break;
                case 0x02:
                    stringBuilder.append("<STX>");
                    break;
                case 0x03:
                    stringBuilder.append("<ETX>");
                    break;
                case 0x04:
                    stringBuilder.append("<EOT>");
                    break;
                case 0x05:
                    stringBuilder.append("<ENQ>");
                    break;
                case 0x06:
                    stringBuilder.append("<ACK>");
                    break;
                case 0x07:
                    stringBuilder.append("<BEL>");
                    break;
                case 0x08:
                    stringBuilder.append("<BS>");
                    break;
                case 0x09:
                    stringBuilder.append("<TAB>");
                    break;
                case 0x0A:
                    stringBuilder.append("<LF>");
                    break;
                case 0x0B:
                    stringBuilder.append("<VT>");
                    break;
                case 0x0C:
                    stringBuilder.append("<FF>");
                    break;
                case 0x0D:
                    stringBuilder.append("<CR>");
                    break;
                case 0x0E:
                    stringBuilder.append("<SO>");
                    break;
                case 0x0F:
                    stringBuilder.append("<SI>");
                    break;
                case 0x10:
                    stringBuilder.append("<DLE>");
                    break;
                case 0x11:
                    stringBuilder.append("<DC1>");
                    break;
                case 0x12:
                    stringBuilder.append("<DC2>");
                    break;
                case 0x13:
                    stringBuilder.append("<DC3>");
                    break;
                case 0x14:
                    stringBuilder.append("<DC4>");
                    break;
                case 0x15:
                    stringBuilder.append("<NAK>");
                    break;
                case 0x16:
                    stringBuilder.append("<SYN>");
                    break;
                case 0x17:
                    stringBuilder.append("<ETB>");
                    break;
                case 0x18:
                    stringBuilder.append("<CAN>");
                    break;
                case 0x19:
                    stringBuilder.append("<EM>");
                    break;
                case 0x1A:
                    stringBuilder.append("<SUB>");
                    break;
                case 0x1B:
                    stringBuilder.append("<ESC>");
                    break;
                case 0x1C:
                    stringBuilder.append("<FS>");
                    break;
                case 0x1D:
                    stringBuilder.append("<GS>");
                    break;
                case 0x1E:
                    stringBuilder.append("<RS>");
                    break;
                case 0x1F:
                    stringBuilder.append("<US>");
                    break;
                case 0x7F:
                    stringBuilder.append("<DEL>");
                    break;
                default:
                    stringBuilder.append((char) value);
                    break;
            }
        }
        setText(stringBuilder.toString());
    }
}