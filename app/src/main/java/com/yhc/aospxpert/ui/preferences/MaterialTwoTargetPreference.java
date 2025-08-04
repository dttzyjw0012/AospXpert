package com.yhc.aospxpert.ui.preferences;

import static com.yhc.aospxpert.ui.preferences.Utils.setBackgroundResource;
import static com.yhc.aospxpert.ui.preferences.Utils.setFirstAndLastItemMargin;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;

import com.yhc.aospxpert.R;

/**
 * The Base preference with two target areas divided by a vertical divider
 */
public class MaterialTwoTargetPreference extends MaterialPreference {

    public MaterialTwoTargetPreference(Context context, AttributeSet attrs,
                                       int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public MaterialTwoTargetPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public MaterialTwoTargetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MaterialTwoTargetPreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setLayoutResource(R.layout.custom_preference_two_target);
        final int secondTargetResId = getSecondTargetResId();
        if (secondTargetResId != 0) {
            setWidgetLayoutResource(secondTargetResId);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final View divider = holder.findViewById(R.id.two_target_divider);
        final View widgetFrame = holder.findViewById(android.R.id.widget_frame);
        final boolean shouldHideSecondTarget = shouldHideSecondTarget();
        if (divider != null) {
            divider.setVisibility(shouldHideSecondTarget ? View.GONE : View.VISIBLE);
        }
        if (widgetFrame != null) {
            widgetFrame.setVisibility(shouldHideSecondTarget ? View.GONE : View.VISIBLE);
        }

        setFirstAndLastItemMargin(holder);
        setBackgroundResource(this, holder);
    }

    protected boolean shouldHideSecondTarget() {
        return getSecondTargetResId() == 0;
    }

    protected int getSecondTargetResId() {
        return 0;
    }
}
