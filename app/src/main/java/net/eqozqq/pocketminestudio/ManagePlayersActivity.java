package net.eqozqq.pocketminestudio;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManagePlayersActivity extends BaseActivity {

    private LinearLayout listContainer;
    private List<String> allPlayers = new ArrayList<>();
    private List<String> whitelistedPlayers = new ArrayList<>();
    private String searchQuery = "";
    private int currentTab = 0;
    private FloatingActionButton fabAdd;
    private TextView toolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getString(R.string.server_players));
        findViewById(R.id.nav_back).setOnClickListener(v -> finish());

        listContainer = findViewById(R.id.list_container);
        TextInputEditText searchInput = findViewById(R.id.search_input);
        fabAdd = findViewById(R.id.fab_add);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        if (tabLayout != null) {
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.addTab(tabLayout.newTab().setText(R.string.server_players));
            tabLayout.addTab(tabLayout.newTab().setText(R.string.title_activity_whitelist));

            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    currentTab = tab.getPosition();
                    switchTab(currentTab);
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });
        }

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                searchQuery = s.toString().toLowerCase();
                renderList();
            }
        });

        fabAdd.setOnClickListener(v -> showAddWhitelistDialog());

        int initialTab = getIntent().getIntExtra("selected_tab", 0);
        if (tabLayout != null && initialTab != 0) {
            TabLayout.Tab tab = tabLayout.getTabAt(initialTab);
            if (tab != null) tab.select();
        } else {
            switchTab(0);
        }
    }

    private void switchTab(int tabPosition) {
        currentTab = tabPosition;
        if (currentTab == 0) {
            toolbarTitle.setText(getString(R.string.auto_java_manage_players));
            fabAdd.setVisibility(View.GONE);
            loadPlayers();
        } else {
            toolbarTitle.setText(getString(R.string.auto_java_manage_whitelist));
            fabAdd.setVisibility(View.VISIBLE);
            loadWhitelist();
        }
    }

    private void loadPlayers() {
        allPlayers.clear();
        try {
            File playersDir = new File(ServerUtils.getAppDirectory() + "/players");
            if (playersDir.exists() && playersDir.isDirectory()) {
                File[] files = playersDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        String name = f.getName();
                        int lastDot = name.lastIndexOf('.');
                        if (lastDot > 0) name = name.substring(0, lastDot);
                        allPlayers.add(name);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (ServerFragment.players != null) {
            for (String p : ServerFragment.players) {
                String cleanName = p.trim().replaceAll("\u00a7[0-9a-fk-or]", "");
                if (!cleanName.isEmpty() && !allPlayers.contains(cleanName)) {
                    allPlayers.add(cleanName);
                }
            }
        }

        renderList();
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

    private boolean isOnline(String player) {
        if (ServerFragment.players == null) return false;
        for (String p : ServerFragment.players) {
            String cleanName = p.trim().replaceAll("\u00a7[0-9a-fk-or]", "");
            if (cleanName.equalsIgnoreCase(player)) return true;
        }
        return false;
    }

    private void renderList() {
        listContainer.removeAllViews();
        if (currentTab == 0) {
            renderPlayersList();
        } else {
            renderWhitelistList();
        }
    }

    private void renderPlayersList() {
        List<String> sorted = new ArrayList<>(allPlayers);
        Collections.sort(sorted, (p1, p2) -> {
            boolean o1 = isOnline(p1);
            boolean o2 = isOnline(p2);
            if (o1 && !o2) return -1;
            if (!o1 && o2) return 1;
            return p1.compareToIgnoreCase(p2);
        });

        for (String player : sorted) {
            if (!searchQuery.isEmpty() && !player.toLowerCase().contains(searchQuery)) {
                continue;
            }

            boolean online = isOnline(player);

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(40, 40, 40, 40);
            item.setBackgroundResource(R.drawable.bg_rounded);
            item.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 20);
            item.setLayoutParams(params);
            item.setClickable(true);

            View statusCircle = new View(this);
            int size = (int) (12 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(size, size);
            circleParams.setMargins(0, 0, 20, 0);
            statusCircle.setLayoutParams(circleParams);
            
            android.graphics.drawable.GradientDrawable circleDrawable = new android.graphics.drawable.GradientDrawable();
            circleDrawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circleDrawable.setColor(online ? android.graphics.Color.parseColor("#00E676") : android.graphics.Color.GRAY);
            statusCircle.setBackground(circleDrawable);
            item.addView(statusCircle);

            TextView tv = new TextView(this);
            tv.setText(player);
            tv.setTextSize(18);
            tv.setTextAppearance(this, android.R.style.TextAppearance_DeviceDefault_Medium);
            item.addView(tv);

            item.setOnClickListener(v -> {
                String[] actions = {"Ban/Unban", "Kick", "Add to Whitelist", "Remove from Whitelist", "Op", "Deop"};
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Player Actions: " + player)
                        .setItems(actions, (dialog, which) -> {
                            if (!ServerUtils.isRunning()) {
                                Toast.makeText(this, getString(R.string.auto_java_start_the_server_to_perform_ac), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            switch (which) {
                                case 0:
                                    showReasonDialog("Ban/Unban " + player, reason -> {
                                        if (reason.isEmpty()) {
                                            ServerUtils.executeCMD("pardon \"" + player + "\"");
                                            Toast.makeText(this, "Sent pardon " + player, Toast.LENGTH_SHORT).show();
                                        } else {
                                            ServerUtils.executeCMD("ban \"" + player + "\" " + reason);
                                            Toast.makeText(this, "Sent ban " + player, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    break;
                                case 1:
                                    if (!online) {
                                        Toast.makeText(this, getString(R.string.auto_java_player_is_not_online), Toast.LENGTH_SHORT).show();
                                        break;
                                    }
                                    showReasonDialog("Kick " + player, reason -> {
                                        ServerUtils.executeCMD("kick \"" + player + "\" " + reason);
                                        Toast.makeText(this, "Sent kick " + player, Toast.LENGTH_SHORT).show();
                                    });
                                    break;
                                case 2:
                                    ServerUtils.executeCMD("whitelist add \"" + player + "\"");
                                    Toast.makeText(this, "Sent whitelist add " + player, Toast.LENGTH_SHORT).show();
                                    break;
                                case 3:
                                    ServerUtils.executeCMD("whitelist remove \"" + player + "\"");
                                    Toast.makeText(this, "Sent whitelist remove " + player, Toast.LENGTH_SHORT).show();
                                    break;
                                case 4:
                                    new MaterialAlertDialogBuilder(this)
                                        .setTitle(getString(R.string.auto_java_confirm_op))
                                        .setMessage("Are you sure you want to give operator to " + player + "?")
                                        .setPositiveButton("Yes", (d, w) -> {
                                            ServerUtils.executeCMD("op \"" + player + "\"");
                                        })
                                        .setNegativeButton("No", null)
                                        .show();
                                    break;
                                case 5:
                                    new MaterialAlertDialogBuilder(this)
                                        .setTitle(getString(R.string.auto_java_confirm_deop))
                                        .setMessage("Are you sure you want to take operator from " + player + "?")
                                        .setPositiveButton("Yes", (d, w) -> {
                                            ServerUtils.executeCMD("deop \"" + player + "\"");
                                        })
                                        .setNegativeButton("No", null)
                                        .show();
                                    break;
                            }
                        })
                        .show();
            });

            listContainer.addView(item);
        }
    }

    private void renderWhitelistList() {
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
                                        File whitelistFile = new File(ServerUtils.getDataDirectory() + "/white-list.txt");
                                        FileWriter fw = new FileWriter(whitelistFile, false);
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

    private void showAddWhitelistDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_input, null);
        TextInputLayout til = dialogView.findViewById(R.id.dialog_til);
        til.setHint(getString(R.string.input_player_name_hint));
        TextInputEditText input = dialogView.findViewById(R.id.dialog_input);
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
                                File whitelistFile = new File(ServerUtils.getDataDirectory() + "/white-list.txt");
                                FileWriter fw = new FileWriter(whitelistFile, true);
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
    }

    private void showReasonDialog(String title, java.util.function.Consumer<String> onConfirm) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_input, null);
        TextInputLayout til = dialogView.findViewById(R.id.dialog_til);
        til.setHint("Reason (leave empty for unban)");
        TextInputEditText input = dialogView.findViewById(R.id.dialog_input);

        new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Submit", (dialog, which) -> {
                if (onConfirm != null) onConfirm.accept(input.getText() != null ? input.getText().toString() : "");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
