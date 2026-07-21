package net.eqozqq.pocketminestudio;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManagePlayersActivity extends BaseActivity {

    private LinearLayout listContainer;
    private List<String> allPlayers = new ArrayList<>();
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        ((android.widget.TextView) findViewById(R.id.toolbar_title)).setText(getString(R.string.auto_java_manage_players));
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

        loadPlayers();
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
                        if (lastDot > 0) {
                            name = name.substring(0, lastDot);
                        }
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
    
    private void showReasonDialog(String title, java.util.function.Consumer<String> onConfirm) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_input, null);
        com.google.android.material.textfield.TextInputLayout til = dialogView.findViewById(R.id.dialog_til);
        til.setHint("Reason (leave empty for unban)");
        com.google.android.material.textfield.TextInputEditText input = dialogView.findViewById(R.id.dialog_input);
        
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
