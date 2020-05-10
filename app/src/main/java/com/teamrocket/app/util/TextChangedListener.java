package com.teamrocket.app.util;

import android.text.Editable;
import android.text.TextWatcher;

public class TextChangedListener implements TextWatcher {

    private Listener listener;

    public TextChangedListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        listener.onTextChanged(count);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    public interface Listener {
        void onTextChanged(int length);
    }
}
