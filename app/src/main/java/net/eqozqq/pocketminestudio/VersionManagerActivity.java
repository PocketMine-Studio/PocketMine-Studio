package net.eqozqq.pocketminestudio;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import net.eqozqq.pocketminestudio.R;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VersionManagerActivity extends BaseActivity {
    private java.util.List<JSONObject> allVersions = new java.util.ArrayList<>();
    private java.util.List<JSONObject> filteredVersions = new java.util.ArrayList<>();
    private int currentPage = 1;
    private int itemsPerPage = 20;
    private String searchQuery = "";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.version_manager);
		start();
	}

	public String getPageContext(String url) throws IOException {
		URLConnection connection = new URL(url).openConnection();
		connection.setRequestProperty("User-Agent", "PocketMine-Studio");
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String str;
		while ((str = in.readLine()) != null) {
			sb.append(str);
		}
		in.close();
		return sb.toString();
	}

	private void start() {
		final android.widget.ProgressBar pbar = findViewById(R.id.loadingBar);
		final android.widget.ScrollView scrollView = findViewById(R.id.scrollView);
		final android.widget.Button skip = findViewById(R.id.skipBtn);
		
		skip.setOnClickListener(v -> finish());
		
		findViewById(R.id.prevBtn).setOnClickListener(v -> {
			if (currentPage > 1) {
				currentPage--;
				renderVersions();
			}
		});
		
		findViewById(R.id.nextBtn).setOnClickListener(v -> {
			if (currentPage * itemsPerPage < filteredVersions.size()) {
				currentPage++;
				renderVersions();
			}
		});
		
		com.google.android.material.textfield.TextInputEditText searchInput = findViewById(R.id.search_input);
		searchInput.addTextChangedListener(new android.text.TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void afterTextChanged(android.text.Editable s) {
				searchQuery = s.toString().toLowerCase();
				currentPage = 1;
				filterVersions();
				renderVersions();
			}
		});

		pbar.setVisibility(android.view.View.VISIBLE);
		scrollView.setVisibility(android.view.View.GONE);
		skip.setVisibility(android.view.View.GONE);

		new Thread(() -> {
			try {
				String jsonString = getPageContext("https://api.github.com/repos/pmmp/pocketmine-mp/releases?per_page=100");
				final org.json.simple.JSONArray versionsArray = (org.json.simple.JSONArray) org.json.simple.JSONValue.parse(jsonString);
				
				if (versionsArray != null) {
					for (int i = 0; i < versionsArray.size(); i++) {
						allVersions.add((org.json.simple.JSONObject) versionsArray.get(i));
					}
				}
				
				runOnUiThread(() -> {
					filterVersions();
					renderVersions();
					pbar.setVisibility(android.view.View.GONE);
					scrollView.setVisibility(android.view.View.VISIBLE);
					skip.setVisibility(ServerUtils.checkIfInstalled() ? android.view.View.VISIBLE : android.view.View.GONE);
				});
			} catch (Exception err) {
				err.printStackTrace();
				showToast("Cannot load version list. Retrying in 5 seconds...");
				try { Thread.sleep(5000); } catch (InterruptedException e) {}
				start();
			}
		}).start();

		android.widget.Button btnImport = findViewById(R.id.importCustomCoreBtn);
		if (btnImport != null) {
			btnImport.setOnClickListener(v -> {
				try {
					android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
					intent.setType("*/*");
					startActivityForResult(intent, 1234);
				} catch (Exception e) {
					android.widget.Toast.makeText(VersionManagerActivity.this, getString(R.string.auto_java_no_file_manager), android.widget.Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

	private void downloadVersion(final String pharUrl, final String shUrl, final String versionName) {
		File vdir = new File(ServerUtils.getDataDirectory() + "/versions/");
		if(!vdir.exists()){
			vdir.mkdirs();
		}
		
		String arch = System.getProperty("os.arch").toLowerCase();
		boolean isArm = arch.contains("aarch64") || arch.contains("arm");
		final String phpUrl = isArm ? "https://github.com/pmmp/PHP-Binaries/releases/download/pm5-php-8.2-latest/PHP-8.2-Android-arm64-PM5.tar.gz" : "https://github.com/pmmp/PHP-Binaries/releases/download/pm5-php-8.2-latest/PHP-8.2-Linux-x86_64-PM5.tar.gz";
		
		final VersionManagerActivity ctx = this;
		runOnUiThread(() -> {
			View dialogView = getLayoutInflater().inflate(R.layout.dialog_progress_horizontal, null);
			TextView tvMsg = dialogView.findViewById(R.id.progress_message);
			ProgressBar pBar = dialogView.findViewById(R.id.progress_bar);
			TextView tvPct = dialogView.findViewById(R.id.progress_percent);
			tvMsg.setText(getString(R.string.auto_java_please_wait));
			pBar.setMax(100);
			pBar.setProgress(0);
			tvPct.setText(getString(R.string.auto_java_0));
			final AlertDialog dlDialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
					.setTitle("Downloading " + versionName + "...")
					.setView(dialogView)
					.setCancelable(false)
					.create();
			dlDialog.show();
			
			dlDialog.getWindow().getDecorView().setTag(R.id.progress_bar, pBar);
			dlDialog.getWindow().getDecorView().setTag(R.id.progress_percent, tvPct);
			
			new Thread(() -> {
				try {
					File pharFile = new File(ServerUtils.getDataDirectory() + "/versions/PocketMine-MP.phar");
					downloadFile(pharUrl, pharFile, pBar, tvPct);
					File shFile = new File(ServerUtils.getDataDirectory() + "/versions/start.sh");
					downloadFile(shUrl, shFile, pBar, tvPct);
					File phpFile = new File(ServerUtils.getDataDirectory() + "/versions/php.tar.gz");
					downloadFile(phpUrl, phpFile, pBar, tvPct);
					
					dlDialog.dismiss();
					install(pharFile, shFile, phpFile);
				} catch (Exception e) {
					e.printStackTrace();
					showToast("Failed to download.");
					dlDialog.dismiss();
				}
			}).start();
		});
	}

	private void downloadFile(String address, File dest, final ProgressBar pb, final TextView tp) throws Exception {
		URL url = new URL(address);
		URLConnection connection = url.openConnection();
		connection.connect();
		int fileLength = connection.getContentLength();

		InputStream input = new BufferedInputStream(url.openStream());
		OutputStream output = new FileOutputStream(dest);

		byte[] data = new byte[1024];
		long total = 0;
		int count;
		int lastProgress = 0;
		while ((count = input.read(data)) != -1) {
			total += count;
			if (fileLength > 0) {
				int progress = (int) (total * 100 / fileLength);
				if (progress != lastProgress) {
					final int p = progress;
					runOnUiThread(() -> {
						if (pb != null) pb.setProgress(p);
						if (tp != null) tp.setText(p + "%");
					});
					lastProgress = progress;
				}
			}
			output.write(data, 0, count);
		}

		output.flush();
		output.close();
		input.close();
	}

	private void install(final File pharFile, final File shFile, final File phpFile) {
		final VersionManagerActivity ctx = this;
		runOnUiThread(() -> {
			View dialogView = getLayoutInflater().inflate(R.layout.dialog_progress, null);
			TextView tvMsg = dialogView.findViewById(R.id.progress_message);
			tvMsg.setText(getString(R.string.auto_java_please_wait));
			final AlertDialog iDialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
					.setTitle(getString(R.string.auto_java_installing))
					.setView(dialogView)
					.setCancelable(false)
					.create();
			iDialog.show();

			new Thread(() -> {
				try {
					delete(new File(ServerUtils.getDataDirectory() + "/src/"));
					delete(new File(ServerUtils.getAppDirectory() + "/php/"));
					new File(ServerUtils.getDataDirectory() + "/PocketMine-MP.phar").delete();
					new File(ServerUtils.getDataDirectory() + "/start.sh").delete();
					
					copyFile(pharFile, new File(ServerUtils.getDataDirectory() + "/PocketMine-MP.phar"));
					copyFile(shFile, new File(ServerUtils.getDataDirectory() + "/start.sh"));
					new File(ServerUtils.getDataDirectory() + "/start.sh").setExecutable(true);
					
					extractTarGz(phpFile, new File(ServerUtils.getAppDirectory() + "/php"));

					runOnUiThread(() -> {
						iDialog.dismiss();
						Intent ver = new Intent(VersionManagerActivity.this, HomeActivity.class);
						startActivity(ver);
						ctx.finish();
					});
				} catch (Exception e) {
					e.printStackTrace();
					showToast("Failed to install.");
					runOnUiThread(() -> iDialog.dismiss());
				}
			}).start();
		});
	}

	private void copyFile(File source, File dest) throws IOException {
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
		} finally {
			if (is != null) is.close();
			if (os != null) os.close();
		}
	}

	private void extractTarGz(File tarGzFile, File targetDirectory) throws IOException {
		TarArchiveInputStream tin = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(tarGzFile)));
		TarArchiveEntry entry;
		while ((entry = tin.getNextTarEntry()) != null) {
			File newFile = new File(targetDirectory, entry.getName());
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
				if (entry.getName().contains("bin/")) {
					newFile.setExecutable(true);
				}
			}
		}
		tin.close();
	}

	public void delete(File f) {
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			if (files != null) {
				for (File file : files) {
					delete(file);
				}
			}
		}
		f.delete();
	}

	public void showToast(final String msg) {
		runOnUiThread(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show());
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	private void filterVersions() {
		filteredVersions.clear();
		for (JSONObject obj : allVersions) {
			String name = (String) obj.get("name");
			String tag = (String) obj.get("tag_name");
			if (name == null) name = "";
			if (tag == null) tag = "";
			
			if (searchQuery.isEmpty() || name.toLowerCase().contains(searchQuery) || tag.toLowerCase().contains(searchQuery)) {
				filteredVersions.add(obj);
			}
		}
	}

	private void renderVersions() {
		final LinearLayout list = findViewById(R.id.versions_list);
		list.removeAllViews();
		
		int startIdx = (currentPage - 1) * itemsPerPage;
		int endIdx = Math.min(startIdx + itemsPerPage, filteredVersions.size());
		
		for (int i = startIdx; i < endIdx; i++) {
			JSONObject obj = filteredVersions.get(i);
			final String version = (String) obj.get("tag_name");
			JSONArray assets = (JSONArray) obj.get("assets");
			
			String pharUrl = "";
			String shUrl = "";
			if (assets != null) {
				for (int j = 0; j < assets.size(); j++) {
					JSONObject asset = (JSONObject) assets.get(j);
					String assetName = (String) asset.get("name");
					if (assetName != null) {
						if (assetName.equals("PocketMine-MP.phar")) {
							pharUrl = (String) asset.get("browser_download_url");
						} else if (assetName.equals("start.sh")) {
							shUrl = (String) asset.get("browser_download_url");
						}
					}
				}
			}
			
			if (shUrl.isEmpty() && !pharUrl.isEmpty()) {
				shUrl = "https://github.com/pmmp/pocketmine-mp/releases/download/" + version + "/start.sh";
			}
			
			final String fPharUrl = pharUrl;
			final String fShUrl = shUrl;

			String name = (String) obj.get("name");
			String body = (String) obj.get("body");
			String mcVersion = "";
			if (body != null) {
				String[] lines = body.split("\\n");
				for (String l : lines) {
					if (l.contains("For Minecraft:")) {
						mcVersion = l.replace("**", "").trim();
						break;
					}
				}
			}

			LinearLayout itemLayout = new LinearLayout(VersionManagerActivity.this);
			itemLayout.setOrientation(LinearLayout.VERTICAL);
			itemLayout.setBackgroundResource(R.drawable.bg_rounded);
			itemLayout.setPadding(40, 40, 40, 40);
			LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			itemParams.setMargins(20, 10, 20, 20);
			itemLayout.setLayoutParams(itemParams);
			
			TextView tvName = new TextView(VersionManagerActivity.this);
			tvName.setText(name != null && !name.isEmpty() ? name : version);
			tvName.setTextSize(18);
			tvName.setTypeface(null, android.graphics.Typeface.BOLD);
			android.util.TypedValue typedValue = new android.util.TypedValue();
			getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
			tvName.setTextColor(getResources().getColor(typedValue.resourceId));
			itemLayout.addView(tvName);
			
			if (!mcVersion.isEmpty()) {
				TextView tvMc = new TextView(VersionManagerActivity.this);
				tvMc.setText(mcVersion);
				tvMc.setTextSize(14);
				tvMc.setTextColor(android.graphics.Color.GRAY);
				tvMc.setPadding(0, 10, 0, 20);
				itemLayout.addView(tvMc);
			}
			
			itemLayout.setClickable(true);
			android.util.TypedValue outValue = new android.util.TypedValue();
			getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
			itemLayout.setForeground(getDrawable(outValue.resourceId));
			
			final String finalName = name != null && !name.isEmpty() ? name : version;
			itemLayout.setOnClickListener(v -> {
				new com.google.android.material.dialog.MaterialAlertDialogBuilder(VersionManagerActivity.this)
						.setTitle("Install " + finalName)
						.setMessage(getString(R.string.auto_java_are_you_sure_you_want_to_insta))
						.setPositiveButton("Install", (dialog, which) -> downloadVersion(fPharUrl, fShUrl, version))
						.setNegativeButton("Cancel", null)
						.show();
			});
			
			list.addView(itemLayout);
		}
		
		android.widget.ImageButton prevBtn = findViewById(R.id.prevBtn);
		android.widget.ImageButton nextBtn = findViewById(R.id.nextBtn);
		TextView pageText = findViewById(R.id.pageText);
		pageText.setText(String.valueOf(currentPage));
		prevBtn.setEnabled(currentPage > 1);
		nextBtn.setEnabled(endIdx < filteredVersions.size());
	}
}
