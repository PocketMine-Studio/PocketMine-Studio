package net.eqozqq.pocketminestudio;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.Menu;
import android.view.SubMenu;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_plugins) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new PluginsFragment())
                        .commit();
                return true;
            }

            Fragment selectedFragment = null;
            if (item.getItemId() == R.id.nav_server) {
                selectedFragment = new ServerFragment();
            } else if (item.getItemId() == R.id.nav_files) {
                selectedFragment = new FilesFragment();
            } else if (item.getItemId() == R.id.nav_options) {
                selectedFragment = new ConfigFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_server);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, ServerFragment.CONSOLE_CODE, 0, "Console")
                .setIcon(R.drawable.ic_terminal_2_24px)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        SubMenu sub = menu.addSubMenu(getString(R.string.abs_settings));
        sub.setIcon(R.drawable.ic_settings_24px);

        sub.add(0, ServerFragment.VERSION_MANAGER_CODE, 0, getString(R.string.abs_version_manager));
        sub.add(0, ServerFragment.FORCE_CLOSE_CODE, 0, getString(R.string.abs_force_close));
        sub.add(0, ServerFragment.ABOUT_US_CODE, 0, getString(R.string.abs_about));
        sub.add(0, 1001, 0, "App Settings");

        sub.getItem().setShowAsAction(
                MenuItem.SHOW_AS_ACTION_IF_ROOM
                        | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home || item.getItemId() == 0) {
            return false;
        }

        if (item.getItemId() == ServerFragment.VERSION_MANAGER_CODE) {
            startActivity(new Intent(this, VersionManagerActivity.class));
        } else if (item.getItemId() == ServerFragment.FORCE_CLOSE_CODE) {
            if (ServerFragment.btn_runServer != null) {
                ServerFragment.btn_runServer.setEnabled(true);
            }
            if (ServerFragment.btn_stopServer != null) {
                ServerFragment.btn_stopServer.setEnabled(false);
            }
            ServerUtils.stopServer();
            if (ServerFragment.servInt != null) {
                stopService(ServerFragment.servInt);
            }
            ServerFragment.isStarted = false;
        } else if (item.getItemId() == ServerFragment.ABOUT_US_CODE) {
            startActivity(new Intent(this, About.class));
        } else if (item.getItemId() == ServerFragment.CONSOLE_CODE) {
            startActivity(new Intent(this, LogActivity.class));
        } else if (item.getItemId() == 1001) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
            builder.setTitle(getString(R.string.auto_java_app_settings));
            LinearLayout settingsLayout = new LinearLayout(this);
            settingsLayout.setOrientation(LinearLayout.VERTICAL);
            settingsLayout.setPadding(30, 30, 30, 30);
            
            android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
            
            final CheckBox cb = new CheckBox(this);
            cb.setText(getString(R.string.auto_java_colored_console));
            cb.setChecked(prefs.getBoolean("colored_console", true));
            
            final CheckBox cbGraph = new CheckBox(this);
            cbGraph.setText(getString(R.string.auto_text_colored_graphs));
            cbGraph.setChecked(prefs.getBoolean("colored_graph", true));
            
            final CheckBox cbStart = new CheckBox(this);
            cbStart.setText(getString(R.string.auto_text_open_console_on_star));
            cbStart.setChecked(prefs.getBoolean("open_console_on_start", true));
            
            settingsLayout.addView(cb);
            settingsLayout.addView(cbGraph);
            settingsLayout.addView(cbStart);
            builder.setView(settingsLayout);
            
            builder.setPositiveButton("Save", (d, w) -> {
                android.content.SharedPreferences.Editor spe = prefs.edit();
                spe.putBoolean("colored_console", cb.isChecked());
                spe.putBoolean("colored_graph", cbGraph.isChecked());
                spe.putBoolean("open_console_on_start", cbStart.isChecked());
                spe.apply();
            });
            builder.show();
        }

        return super.onOptionsItemSelected(item);
    }
}
