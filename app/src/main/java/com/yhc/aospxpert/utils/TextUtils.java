package com.yhc.aospxpert.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

public class TextUtils {

    /**
     * @noinspection SameParameterValue
     */
    @NonNull
    public static SpannableString getClickableText(Activity activity, String message, String link) {
        SpannableString spannableMessage = new SpannableString(message);

        int start = message.indexOf(link);
        int end = start + link.length();

        spannableMessage.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    activity.startActivity(intent);
                } catch (Exception exception) {
                    Log.e("IconPackRepo", "Browser not found");
                }
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        Linkify.addLinks(spannableMessage, Linkify.WEB_URLS);

        return spannableMessage;
    }
}
