package net.eqozqq.pocketminestudio;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MacroManager {
    private static final String PREF_NAME = "macros_prefs";
    private static final String KEY_MACROS = "macros_list";
    private static final String[] DEFAULT_MACROS = {"ban", "unban", "kick", "op", "deop", "time set", "stop", "save-all"};

    public static List<String> getMacros(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_MACROS, null);
        if (json == null) {
            return new ArrayList<>(Arrays.asList(DEFAULT_MACROS));
        }
        List<String> macros = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                macros.add(array.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return new ArrayList<>(Arrays.asList(DEFAULT_MACROS));
        }
        return macros;
    }

    public static void saveMacros(Context context, List<String> macros) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        JSONArray array = new JSONArray();
        for (String m : macros) {
            array.put(m);
        }
        prefs.edit().putString(KEY_MACROS, array.toString()).apply();
    }
}
