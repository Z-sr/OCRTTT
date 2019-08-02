package com.example.ocrttt;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class MyTextView extends ViewGroup {

    public MyTextView(Context context) {
        super(context);
        init(context);
    }
    public MyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }
}
