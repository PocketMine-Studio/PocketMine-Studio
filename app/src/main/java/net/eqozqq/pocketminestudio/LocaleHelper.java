package net.eqozqq.pocketminestudio;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {

    private static final String SELECTED_LANGUAGE = "language_code";

    public static Context onAttach(Context context) {
        String lang = getPersistedData(context, "");
        return setLocale(context, lang);
    }

    public static Context setLocale(Context context, String language) {
        if (language.isEmpty()) {
            language = Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage();
        }

        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

    private static String getPersistedData(Context context, String defaultLanguage) {
        SharedPreferences preferences = context.getSharedPreferences("net.eqozqq.pocketminestudio_preferences", Context.MODE_PRIVATE);
        return preferences.getString(SELECTED_LANGUAGE, defaultLanguage);
    }
}
