package net.eqozqq.pocketminestudio;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ManageWhitelistActivity extends BaseActivity {

    private LinearLayout listContainer;
    private List<String> whitelistedPlayers = new ArrayList<>();
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        ((android.widget.TextView) findViewById(R.id.toolbar_title)).setText(getString(R.string.auto_java_manage_whitelist));
        findViewById(R.id.nav_back).setOnClickListener(v -> finish());
        
        

        listContainer = findViewById(R.id.list_container);
        TextInputEditText searchInput = findViewById(R.id.search_input);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                searchQuery = s.toString().toLowerCase();
                renderList();
            }
        });

        com.google.android.material.floatingactionbutton.FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(v -> {
            android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_input, null);
            com.google.android.material.textfield.TextInputLayout til = dialogView.findViewById(R.id.dialog_til);
            til.setHint(getString(R.string.input_player_name_hint));
            com.google.android.material.textfield.TextInputEditText input = dialogView.findViewById(R.id.dialog_input);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.auto_java_add_to_whitelist))
                    .setView(dialogView)
                    .setPositiveButton(getString(R.string.btn_add), (dialog, which) -> {
                        String name = input.getText().toString().trim();
                        if (!name.isEmpty()) {
                            if (ServerUtils.isRunning()) {
                                ServerUtils.executeCMD("whitelist add \"" + name + "\"");
                                Toast.makeText(this, "Added " + name + " to whitelist.", Toast.LENGTH_SHORT).show();
                            } else {
                                try {
                                    java.io.File whitelistFile = new java.io.File(ServerUtils.getDataDirectory() + "/white-list.txt");
                                    java.io.FileWriter fw = new java.io.FileWriter(whitelistFile, true);
                                    fw.write("\n" + name);
                                    fw.close();
                                    Toast.makeText(this, "Added " + name + " to white-list.txt", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(this, getString(R.string.auto_java_failed_to_write_to_file), Toast.LENGTH_SHORT).show();
                                }
                            }
                            if (!whitelistedPlayers.contains(name.toLowerCase()) && !whitelistedPlayers.contains(name)) {
                                whitelistedPlayers.add(name);
                                renderList();
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.btn_cancel), null)
                    .show();
        });

        loadWhitelist();
    }

    private void loadWhitelist() {
        whitelistedPlayers.clear();
        try {
            File whitelistFile = new File(ServerUtils.getDataDirectory() + "/white-list.txt");
            if (whitelistFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(whitelistFile));
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty() && !line.startsWith("#")) {
                        whitelistedPlayers.add(line.trim());
                    }
                }
                br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        renderList();
    }

    private void renderList() {
        listContainer.removeAllViews();
        for (String player : whitelistedPlayers) {
            if (!searchQuery.isEmpty() && !player.toLowerCase().contains(searchQuery)) {
                continue;
            }

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(40, 40, 40, 40);
            item.setBackgroundResource(R.drawable.bg_rounded);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 20);
            item.setLayoutParams(params);
            item.setClickable(true);

            TextView tv = new TextView(this);
            tv.setText(player);
            tv.setTextSize(18);
            tv.setTextAppearance(this, android.R.style.TextAppearance_DeviceDefault_Medium);
            item.addView(tv);

            item.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.auto_java_whitelist_actions))
                        .setItems(new CharSequence[]{getString(R.string.whitelist_remove_action)}, (dialog, which) -> {
                            if (which == 0) {
                                if (ServerUtils.isRunning()) {
                                    ServerUtils.executeCMD("whitelist remove \"" + player + "\"");
                                    Toast.makeText(this, "Command sent to remove " + player, Toast.LENGTH_SHORT).show();
                                    listContainer.removeView(item);
                                } else {
                                    try {
                                        whitelistedPlayers.remove(player);
                                        java.io.File whitelistFile = new java.io.File(ServerUtils.getDataDirectory() + "/white-list.txt");
                                        java.io.FileWriter fw = new java.io.FileWriter(whitelistFile, false);
                                        for (String p : whitelistedPlayers) {
                                            fw.write(p + "\n");
                                        }
                                        fw.close();
                                        Toast.makeText(this, "Removed " + player + " from white-list.txt", Toast.LENGTH_SHORT).show();
                                        listContainer.removeView(item);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Toast.makeText(this, getString(R.string.auto_java_failed_to_remove_player_offlin), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        })
                        .show();
            });

            listContainer.addView(item);
        }
    }
}
