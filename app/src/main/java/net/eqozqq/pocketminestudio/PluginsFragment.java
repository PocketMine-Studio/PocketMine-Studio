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
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class PluginsFragment extends Fragment {

    private LinearLayout list;
    private View pbar;
    private ScrollView scrollView;
    private View view;
    private java.util.List<JSONObject> allPlugins = new java.util.ArrayList<>();
    private Map<String, Integer> categoryCounts = new LinkedHashMap<>();
    private String selectedCategory = null;
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
            View dialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_filter_api, null);
            
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

    private String getFileNameFromUri(android.net.Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = requireActivity().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        if (result == null || result.isEmpty()) {
            result = "plugin.phar";
        }
        return result;
    }

    private void extractZip(InputStream is, File targetDir) throws Exception {
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            File newFile = new File(targetDir, entry.getName());
            if (entry.isDirectory()) {
                newFile.mkdirs();
            } else {
                newFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zis.closeEntry();
        }
        zis.close();
    }

    private void extractTarGz(InputStream is, File targetDir) throws Exception {
        TarArchiveInputStream tin = new TarArchiveInputStream(new GzipCompressorInputStream(is));
        TarArchiveEntry entry;
        while ((entry = tin.getNextTarEntry()) != null) {
            File newFile = new File(targetDir, entry.getName());
            if (entry.isDirectory()) {
                newFile.mkdirs();
            } else {
                newFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = tin.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
        }
        tin.close();
    }

    private void extractTar(InputStream is, File targetDir) throws Exception {
        TarArchiveInputStream tin = new TarArchiveInputStream(is);
        TarArchiveEntry entry;
        while ((entry = tin.getNextTarEntry()) != null) {
            File newFile = new File(targetDir, entry.getName());
            if (entry.isDirectory()) {
                newFile.mkdirs();
            } else {
                newFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = tin.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
        }
        tin.close();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2345 && resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri uri = data.getData();
            try {
                String fileName = getFileNameFromUri(uri);
                File pluginsDir = new File(ServerUtils.getDataDirectory() + "/plugins/");
                if (!pluginsDir.exists()) {
                    pluginsDir.mkdirs();
                }

                String lower = fileName.toLowerCase();
                if (lower.endsWith(".zip")) {
                    try (InputStream is = requireActivity().getContentResolver().openInputStream(uri)) {
                        extractZip(is, pluginsDir);
                    }
                } else if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
                    try (InputStream is = requireActivity().getContentResolver().openInputStream(uri)) {
                        extractTarGz(is, pluginsDir);
                    }
                } else if (lower.endsWith(".tar")) {
                    try (InputStream is = requireActivity().getContentResolver().openInputStream(uri)) {
                        extractTar(is, pluginsDir);
                    }
                } else {
                    File destFile = new File(pluginsDir, fileName);
                    try (InputStream is = requireActivity().getContentResolver().openInputStream(uri);
                         OutputStream os = new FileOutputStream(destFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) > 0) {
                            os.write(buffer, 0, length);
                        }
                    }
                }

                Toast.makeText(requireActivity(), String.format(getString(R.string.msg_plugin_imported), fileName), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireActivity(), getString(R.string.msg_failed_to_import_plugin), Toast.LENGTH_SHORT).show();
            }
        }
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
                        buildCategories();
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

    private void buildCategories() {
        categoryCounts.clear();
        String allTitle = getString(R.string.category_all);
        categoryCounts.put(allTitle, allPlugins.size());
        for (JSONObject plugin : allPlugins) {
            JSONArray cats = (JSONArray) plugin.get("categories");
            if (cats != null) {
                for (int i = 0; i < cats.size(); i++) {
                    JSONObject catObj = (JSONObject) cats.get(i);
                    if (catObj != null) {
                        String cName = (String) catObj.get("category_name");
                        if (cName != null && !cName.trim().isEmpty()) {
                            cName = cName.trim();
                            categoryCounts.put(cName, categoryCounts.getOrDefault(cName, 0) + 1);
                        }
                    }
                }
            }
        }
    }

    private boolean isPluginInCategory(JSONObject plugin, String category) {
        if (category == null || category.equalsIgnoreCase("All") || category.equals(getString(R.string.category_all))) {
            return true;
        }
        JSONArray cats = (JSONArray) plugin.get("categories");
        if (cats != null) {
            for (int i = 0; i < cats.size(); i++) {
                JSONObject catObj = (JSONObject) cats.get(i);
                if (catObj != null) {
                    String cName = (String) catObj.get("category_name");
                    if (cName != null && cName.equalsIgnoreCase(category)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void renderCategories() {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            list.removeAllViews();

            View searchLayout = view.findViewById(R.id.filter_layout);
            if (searchLayout != null) {
                searchLayout.setVisibility(View.GONE);
            }

            TextView header = new TextView(requireActivity());
            header.setText(getString(R.string.title_activity_category));
            header.setTextSize(22);
            header.setTypeface(null, android.graphics.Typeface.BOLD);
            header.setPadding(30, 20, 30, 30);
            list.addView(header);

            LinearLayout currentGridRow = null;
            int count = 0;

            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                String catName = entry.getKey();
                int catCount = entry.getValue();

                if (count % 2 == 0) {
                    currentGridRow = new LinearLayout(requireActivity());
                    currentGridRow.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    rowParams.setMargins(36, 14, 36, 14);
                    currentGridRow.setLayoutParams(rowParams);
                    list.addView(currentGridRow);
                }

                LinearLayout card = new LinearLayout(requireActivity());
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(android.view.Gravity.CENTER);
                card.setPadding(36, 36, 36, 36);
                card.setClickable(true);

                boolean isAll = catName.equalsIgnoreCase("All") || catName.equals(getString(R.string.category_all));

                android.util.TypedValue bgAttr = new android.util.TypedValue();
                requireActivity().getTheme().resolveAttribute(isAll ? com.google.android.material.R.attr.colorSecondaryContainer : com.google.android.material.R.attr.colorSurfaceContainerHigh, bgAttr, true);

                android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
                shape.setColor(bgAttr.data);
                shape.setCornerRadius(64f);
                card.setBackground(shape);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                if (count % 2 == 0) {
                    cardParams.setMargins(0, 0, 14, 0);
                } else {
                    cardParams.setMargins(14, 0, 0, 0);
                }
                card.setLayoutParams(cardParams);

                android.util.TypedValue outValue = new android.util.TypedValue();
                requireActivity().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                card.setForeground(requireActivity().getDrawable(outValue.resourceId));

                TextView tvTitle = new TextView(requireActivity());
                tvTitle.setText(catName);
                tvTitle.setTextSize(18);
                tvTitle.setGravity(android.view.Gravity.CENTER);
                tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                
                android.util.TypedValue titleAttr = new android.util.TypedValue();
                requireActivity().getTheme().resolveAttribute(isAll ? com.google.android.material.R.attr.colorOnSecondaryContainer : com.google.android.material.R.attr.colorOnSurface, titleAttr, true);
                tvTitle.setTextColor(titleAttr.data);
                card.addView(tvTitle);

                TextView tvCount = new TextView(requireActivity());
                tvCount.setText(catCount + " " + getString(R.string.unit_plugins));
                tvCount.setTextSize(13);
                tvCount.setGravity(android.view.Gravity.CENTER);
                
                android.util.TypedValue countAttr = new android.util.TypedValue();
                requireActivity().getTheme().resolveAttribute(isAll ? com.google.android.material.R.attr.colorOnSecondaryContainer : com.google.android.material.R.attr.colorOnSurfaceVariant, countAttr, true);
                tvCount.setTextColor(countAttr.data);
                tvCount.setPadding(0, 8, 0, 0);
                card.addView(tvCount);

                card.setOnClickListener(v -> {
                    selectedCategory = catName;
                    filterAndDisplayPlugins();
                });

                if (currentGridRow != null) {
                    currentGridRow.addView(card);
                }
                count++;
            }
        });
    }

    private void filterAndDisplayPlugins() {
        if (getActivity() == null) return;
        if (selectedCategory == null) {
            renderCategories();
            return;
        }

        requireActivity().runOnUiThread(() -> {
            list.removeAllViews();

            View searchLayout = view.findViewById(R.id.filter_layout);
            if (searchLayout != null) {
                searchLayout.setVisibility(View.VISIBLE);
            }

            LinearLayout topBar = new LinearLayout(requireActivity());
            topBar.setOrientation(LinearLayout.HORIZONTAL);
            topBar.setGravity(android.view.Gravity.CENTER_VERTICAL);
            topBar.setPadding(20, 10, 20, 20);

            MaterialButton btnBack = new MaterialButton(requireActivity(), null, com.google.android.material.R.attr.borderlessButtonStyle);
            btnBack.setText(selectedCategory);
            btnBack.setTextSize(16);
            btnBack.setIconResource(R.drawable.ic_arrow_back_24px);
            btnBack.setOnClickListener(v -> {
                selectedCategory = null;
                searchQuery = "";
                minApiQuery = "";
                maxApiQuery = "";
                com.google.android.material.textfield.TextInputEditText searchInput = view.findViewById(R.id.search_input);
                if (searchInput != null) {
                    searchInput.setText("");
                }
                renderCategories();
            });
            topBar.addView(btnBack);
            list.addView(topBar);

            for (JSONObject plugin : allPlugins) {
                if (!isPluginInCategory(plugin, selectedCategory)) {
                    continue;
                }

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
                    JSONArray api = (JSONArray) plugin.get("api");
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
                    Intent intent = new Intent(requireActivity(), PluginDetailsActivity.class);
                    intent.putExtra("name", name);
                    intent.putExtra("version", version);
                    intent.putExtra("iconUrl", iconUrl);
                    intent.putExtra("descUrl", descUrl);
                    intent.putExtra("dlUrl", dlUrl);
                    intent.putExtra("author", author);
                    startActivity(intent);
                });

                ImageView icon = new ImageView(requireActivity());
                int size = (int) (64 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(size, size);
                iconParams.setMargins(0, 0, 30, 0);
                icon.setLayoutParams(iconParams);
                Glide.with(requireActivity()).load(iconUrl).placeholder(R.drawable.ic_extension_24px_fill).into(icon);
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