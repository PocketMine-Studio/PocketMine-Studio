package net.eqozqq.pocketminestudio;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PluginDetailsActivity extends BaseActivity {

    private String name, version, iconUrl, descUrl, dlUrl, author;
    private LinearLayout versionsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_details);

        name = getIntent().getStringExtra("name");
        version = getIntent().getStringExtra("version");
        iconUrl = getIntent().getStringExtra("iconUrl");
        descUrl = getIntent().getStringExtra("descUrl");
        dlUrl = getIntent().getStringExtra("dlUrl");
        author = getIntent().getStringExtra("author");

        TextView tvName = findViewById(R.id.plugin_name);
        TextView tvVersion = findViewById(R.id.plugin_version);
        TextView tvAuthor = findViewById(R.id.plugin_author);
        ImageView ivIcon = findViewById(R.id.plugin_icon);
        versionsList = findViewById(R.id.versions_list);
        WebView webDesc = findViewById(R.id.plugin_description);
        ProgressBar pbar = findViewById(R.id.loadingBar);

        tvName.setText(name);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(name);
        findViewById(R.id.nav_back).setOnClickListener(v -> finish());
        
        toolbar.getMenu().add(0, R.id.action_share, 0, "Open in browser").setIcon(R.drawable.ic_open_in_new_24px).setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share) {
                String url = "https://poggit.pmmp.io/p/" + name;
                android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                i.setData(android.net.Uri.parse(url));
                startActivity(i);
                return true;
            }
            return false;
        });
        tvVersion.setText("v" + version);
        tvAuthor.setText("by " + (author != null ? author : "Unknown"));

        if (iconUrl != null && !iconUrl.isEmpty()) {
            Glide.with(this).load(iconUrl).placeholder(R.drawable.ic_extension_24px_fill).into(ivIcon);
        }

        webDesc.setBackgroundColor(Color.TRANSPARENT);

        pbar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String markdown = "";
                if (descUrl != null && !descUrl.isEmpty()) {
                    URL url = new URL(descUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line).append("\n");
                    }
                    reader.close();
                    markdown = result.toString();
                } else {
                    markdown = "No description available.";
                }

                final String finalHtml = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><style>body{font-family: sans-serif; padding: 0; margin: 0; word-wrap: break-word; background: transparent;} @media (prefers-color-scheme: dark) { body { color: #FFFFFF; } } @media (prefers-color-scheme: light) { body { color: #000000; } } img{max-width: 100%; height: auto;}</style></head><body>"
                        + markdown + "</body></html>";
                runOnUiThread(() -> {
                    webDesc.loadDataWithBaseURL("https://poggit.pmmp.io/", finalHtml, "text/html", "UTF-8", null);
                });

                URL vUrl = new URL("https://poggit.pmmp.io/releases.json?name=" + name);
                HttpURLConnection vConn = (HttpURLConnection) vUrl.openConnection();
                vConn.setRequestMethod("GET");
                BufferedReader vReader = new BufferedReader(new InputStreamReader(vConn.getInputStream()));
                StringBuilder vResult = new StringBuilder();
                String vLine;
                while ((vLine = vReader.readLine()) != null) {
                    vResult.append(vLine);
                }
                vReader.close();

                JSONArray jsonArr = (JSONArray) JSONValue.parse(vResult.toString());

                runOnUiThread(() -> {
                    pbar.setVisibility(View.GONE);
                    if (jsonArr != null) {
                        for (Object obj : jsonArr) {
                            JSONObject json = (JSONObject) obj;
                            String vName = (String) json.get("version");
                            String vDl = (String) json.get("artifact_url") + "/" + name + ".phar";

                            JSONArray apiArr = (JSONArray) json.get("api");
                            String apiStr = "";
                            if (apiArr != null && apiArr.size() > 0) {
                                JSONObject api0 = (JSONObject) apiArr.get(0);
                                apiStr = "API: " + api0.get("from");
                            }

                            LinearLayout row = new LinearLayout(PluginDetailsActivity.this);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setPadding(30, 30, 30, 30);
                            row.setBackgroundResource(R.drawable.bg_rounded);
                            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                            rowParams.setMargins(0, 10, 0, 20);
                            row.setLayoutParams(rowParams);
                            row.setWeightSum(1);
                            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                            LinearLayout textContainer = new LinearLayout(PluginDetailsActivity.this);
                            textContainer.setOrientation(LinearLayout.VERTICAL);
                            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                            textContainer.setLayoutParams(textParams);

                            TextView tvV = new TextView(PluginDetailsActivity.this);
                            tvV.setText(vName);
                            tvV.setTextSize(16);
                            tvV.setTypeface(null, android.graphics.Typeface.BOLD);
                            textContainer.addView(tvV);

                            TextView tvA = new TextView(PluginDetailsActivity.this);
                            tvA.setText(apiStr);
                            textContainer.addView(tvA);

                            MaterialButton btn = new MaterialButton(PluginDetailsActivity.this);
                            btn.setText(getString(R.string.download));
                            btn.setOnClickListener(v -> downloadPlugin(vName, vDl));

                            row.addView(textContainer);
                            row.addView(btn);

                            versionsList.addView(row);
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    webDesc.loadData("Failed to load description.", "text/plain", "UTF-8");
                    pbar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void downloadPlugin(String targetVersion, String targetUrl) {
        if (targetUrl == null || targetUrl.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_no_download_url), Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(String.format(getString(R.string.dialog_download_title), name, targetVersion))
                .setMessage(getString(R.string.auto_java_do_you_want_to_download_this_v))
                .setPositiveButton(getString(R.string.btn_download), (dialog, which) -> {
                    startDownload(targetVersion, targetUrl);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void startDownload(String targetVersion, String targetUrl) {
        File pluginsDir = new File(ServerUtils.getDataDirectory() + "/plugins");
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs();
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        TextView tvMsg = dialogView.findViewById(R.id.progress_message);
        tvMsg.setText(getString(R.string.auto_java_downloading));

        androidx.appcompat.app.AlertDialog dlDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Downloading " + name)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        dlDialog.show();

        new Thread(() -> {
            try {
                URL url = new URL(targetUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream input = new BufferedInputStream(url.openStream());
                File dest = new File(pluginsDir, name + "_v" + targetVersion + ".phar");
                OutputStream output = new FileOutputStream(dest);

                byte[] data = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                runOnUiThread(() -> {
                    dlDialog.dismiss();
                    Toast.makeText(PluginDetailsActivity.this, getString(R.string.msg_plugin_downloaded),
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    dlDialog.dismiss();
                    Toast.makeText(PluginDetailsActivity.this, getString(R.string.msg_download_failed), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


}
