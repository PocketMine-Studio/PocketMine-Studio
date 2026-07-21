package net.eqozqq.pocketminestudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class PluginsFragment extends Fragment {

    private LinearLayout list;
    private ProgressBar pbar;
    private ScrollView scrollView;
    private View view;
    private java.util.List<org.json.simple.JSONObject> allPlugins = new java.util.ArrayList<>();
    private String searchQuery = "";
    private String minApiQuery = "";
    private String maxApiQuery = "";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_plugins_list, container, false);

        list = view.findViewById(R.id.plugins_list);
        pbar = view.findViewById(R.id.loadingBar);
        scrollView = view.findViewById(R.id.scrollView);
        com.google.android.material.textfield.TextInputEditText searchInput = view.findViewById(R.id.search_input);
        if (searchInput != null) {
            searchInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) {
                    searchQuery = s.toString().toLowerCase();
                    filterAndDisplayPlugins();
                }
            });
        }
        
        view.findViewById(R.id.btn_filter).setOnClickListener(v -> {
            android.view.View dialogView = android.view.LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_filter_api, null);
            
            com.google.android.material.textfield.TextInputEditText minInput = dialogView.findViewById(R.id.dialog_min_input);
            minInput.setHint(getString(R.string.hint_min_api));
            minInput.setText(minApiQuery);

            com.google.android.material.textfield.TextInputEditText maxInput = dialogView.findViewById(R.id.dialog_max_input);
            maxInput.setHint(getString(R.string.hint_max_api));
            maxInput.setText(maxApiQuery);

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(R.string.auto_java_filter_by_api))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.btn_apply), (dialog, which) -> {
                    minApiQuery = minInput.getText().toString();
                    maxApiQuery = maxInput.getText().toString();
                    filterAndDisplayPlugins();
                })
                .setNegativeButton(getString(R.string.btn_clear), (dialog, which) -> {
                    minApiQuery = "";
                    maxApiQuery = "";
                    filterAndDisplayPlugins();
                })
                .show();
        });
        loadPlugins();
        return view;
    }

    private void loadPlugins() {
        pbar.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                URL url = new URL("https://poggit.pmmp.io/releases.json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                final JSONArray pluginsArray = (JSONArray) JSONValue.parse(result.toString());

                requireActivity().runOnUiThread(() -> {
                    if (pluginsArray != null) {
                        HashMap<String, JSONObject> uniquePlugins = new HashMap<>();
                        for (int i = 0; i < pluginsArray.size(); i++) {
                            JSONObject plugin = (JSONObject) pluginsArray.get(i);
                            String name = (String) plugin.get("name");
                            if (!uniquePlugins.containsKey(name)) {
                                uniquePlugins.put(name, plugin);
                            }
                        }
                        allPlugins.clear();
                        allPlugins.addAll(uniquePlugins.values());
                        filterAndDisplayPlugins();
                    }
                    pbar.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);
                });

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        pbar.setVisibility(View.GONE);
                        Toast.makeText(requireActivity(), getString(R.string.msg_failed_to_load_plugins), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void filterAndDisplayPlugins() {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            list.removeAllViews();
            for (JSONObject plugin : allPlugins) {
                String name = (String) plugin.get("name");
                String version = (String) plugin.get("version");
                String tagline = (String) plugin.get("tagline");
                String iconUrl = (String) plugin.get("icon_url");
                String descUrl = (String) plugin.get("description_url");
                String dlUrl = (String) plugin.get("artifact_url");
                String repoName = (String) plugin.get("repo_name");
                String author = repoName != null && repoName.contains("/") ? repoName.split("/")[0] : "Unknown";
                
                String apiVersion = "";
                String minApi = "";
                String maxApi = "";
                try {
                    org.json.simple.JSONArray api = (org.json.simple.JSONArray) plugin.get("api");
                    if (api != null && api.size() > 0) {
                        JSONObject firstApi = (JSONObject) api.get(0);
                        minApi = (String) firstApi.get("from");
                        maxApi = (String) firstApi.get("to");
                        apiVersion = "API: " + minApi + " - " + maxApi;
                    }
                } catch (Exception e) {}

                if (!searchQuery.isEmpty() && !name.toLowerCase().contains(searchQuery) && !(tagline != null && tagline.toLowerCase().contains(searchQuery))) {
                    continue;
                }
                
                if (!minApiQuery.isEmpty()) {
                    if (minApi.isEmpty() || minApi.compareTo(minApiQuery) < 0) {
                        continue;
                    }
                }
                
                if (!maxApiQuery.isEmpty()) {
                    if (maxApi.isEmpty() || maxApi.compareTo(maxApiQuery) > 0) {
                        continue;
                    }
                }

                LinearLayout item = new LinearLayout(requireActivity());
                item.setOrientation(LinearLayout.HORIZONTAL);
                item.setPadding(30, 30, 30, 30);
                item.setBackgroundResource(R.drawable.bg_rounded);
                LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                itemParams.setMargins(20, 10, 20, 20);
                item.setLayoutParams(itemParams);
                item.setClickable(true);
                
                item.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(requireActivity(), PluginDetailsActivity.class);
                    intent.putExtra("name", name);
                    intent.putExtra("version", version);
                    intent.putExtra("iconUrl", iconUrl);
                    intent.putExtra("descUrl", descUrl);
                    intent.putExtra("dlUrl", dlUrl);
                    intent.putExtra("author", author);
                    startActivity(intent);
                });

                android.widget.ImageView icon = new android.widget.ImageView(requireActivity());
                int size = (int) (64 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(size, size);
                iconParams.setMargins(0, 0, 30, 0);
                icon.setLayoutParams(iconParams);
                com.bumptech.glide.Glide.with(requireActivity()).load(iconUrl).placeholder(R.drawable.ic_extension_24px_fill).into(icon);
                item.addView(icon);

                LinearLayout textLayout = new LinearLayout(requireActivity());
                textLayout.setOrientation(LinearLayout.VERTICAL);
                textLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

                TextView tvName = new TextView(requireActivity());
                tvName.setText(name);
                tvName.setTextSize(18);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                textLayout.addView(tvName);

                if (!apiVersion.isEmpty()) {
                    TextView tvApi = new TextView(requireActivity());
                    tvApi.setText(apiVersion);
                    tvApi.setTextSize(12);
                    tvApi.setTextColor(android.graphics.Color.GRAY);
                    textLayout.addView(tvApi);
                }

                if (tagline != null) {
                    TextView tvDesc = new TextView(requireActivity());
                    tvDesc.setText(tagline);
                    tvDesc.setTextSize(14);
                    textLayout.addView(tvDesc);
                }

                item.addView(textLayout);
                list.addView(item);
            }
        });
    }
}