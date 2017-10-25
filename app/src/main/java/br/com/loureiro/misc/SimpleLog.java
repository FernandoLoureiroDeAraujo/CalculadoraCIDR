package br.com.loureiro.misc;

import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

/**
 * Created by fernando on 31/07/17.
 */

public class SimpleLog {

    private static final String INFO = "INFO";
    private static final String ERROR = "ERROR";

    public static void showMessage(View view, String msg) {
        showMessage(view, msg, null);
    }

    public static void showMessage(View view, String msg, Throwable t) {
        error(msg, t);

        Snackbar.make(view, msg, Snackbar.LENGTH_LONG).setAction("", null).show();
    }

    public static void info(String msg) {
        Log.i(INFO, msg);
    }

    public static void error(String msg, Throwable t) {
        Log.e(ERROR, msg, t);
    }
}
