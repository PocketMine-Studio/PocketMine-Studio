package net.eqozqq.pocketminestudio;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.nav_back).setOnClickListener(v -> finish());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        MaterialSwitch switchColoredGraph = findViewById(R.id.switch_colored_graph);
        MaterialSwitch switchOpenConsole = findViewById(R.id.switch_open_console);

        switchColoredGraph.setChecked(prefs.getBoolean("colored_graph", true));
        switchOpenConsole.setChecked(prefs.getBoolean("open_console_on_start", true));

        switchColoredGraph.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("colored_graph", isChecked);
            editor.apply();
            android.widget.Toast.makeText(this, getString(R.string.auto_java_restart_settings), android.widget.Toast.LENGTH_SHORT).show();
        });

        switchOpenConsole.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("open_console_on_start", isChecked);
            editor.apply();
        });

        AutoCompleteTextView languageSpinner = findViewById(R.id.language_spinner);
        String[] languages = {"System Default", "English", "Українська", "Español", "中文", "Русский", "日本語", "Polski", "Deutsch", "Français", "Português (Brasil)", "Italiano", "Türkçe", "Bahasa Indonesia"};
        String[] langCodes = {"", "en", "uk", "es", "zh", "ru", "ja", "pl", "de", "fr", "pt", "it", "tr", "id"};
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, languages);
        languageSpinner.setAdapter(adapter);

        SharedPreferences globalPrefs = getSharedPreferences("net.eqozqq.pocketminestudio_preferences", android.content.Context.MODE_PRIVATE);
        String currentLangCode = globalPrefs.getString("language_code", "");
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(currentLangCode)) {
                languageSpinner.setText(languages[i], false);
                break;
            }
        }

        languageSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCode = langCodes[position];
            if (!currentLangCode.equals(selectedCode)) {
                globalPrefs.edit().putString("language_code", selectedCode).apply();
                android.widget.Toast.makeText(this, getString(R.string.auto_java_restart_settings), android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_support).setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_support_title)
                .setMessage(R.string.dialog_support_message)
                .setPositiveButton(R.string.btn_patreon, (dialog, which) -> {
                    android.content.Intent browserIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://patreon.com/eqozqq"));
                    startActivity(browserIntent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        });
    }
}
