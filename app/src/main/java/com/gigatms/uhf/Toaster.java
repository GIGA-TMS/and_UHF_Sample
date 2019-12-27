package com.gigatms.uhf;

import android.content.Context;
import android.widget.Toast;

public class Toaster {
    private static Toast toast;

    public static void showToast(Context context, String text, int duration) {
        if (toast != null) {
            toast.cancel();
        }
        if (context != null) {
            toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }
}
