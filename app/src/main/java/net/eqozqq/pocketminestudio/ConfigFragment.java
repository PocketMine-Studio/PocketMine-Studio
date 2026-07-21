package net.eqozqq.pocketminestudio;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import net.eqozqq.pocketminestudio.R;

import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class ConfigFragment extends Fragment {

	private Boolean install = false;

	public LinkedHashMap<String, String> values = null;
	public String ram = "64";

	public View view;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.activity_config, container, false);
		
		install = requireActivity().getIntent().getBooleanExtra("install", false);

		final CheckBox showAdvanced = (CheckBox) view.findViewById(R.id.config_advanced);
		final TextView advancedLabel = (TextView) view.findViewById(R.id.config_advanced_label);
		final LinearLayout advanced = (LinearLayout) view.findViewById(R.id.config_advanced_layout);
		advanced.setVisibility(View.GONE);
		advancedLabel.setVisibility(View.GONE);
		showAdvanced.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton sender, boolean checked) {
				advanced.setVisibility(checked ? View.VISIBLE : View.GONE);
				advancedLabel.setVisibility(checked ? View.VISIBLE : View.GONE);
			}
		});

		final Button saveBtn = (Button) view.findViewById(R.id.config_save);
		saveBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				save();
				
			}
		});

		final Button cancelBtn = (Button) view.findViewById(R.id.config_cancel);
		cancelBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
			}
		});
		cancelBtn.setVisibility(install ? View.GONE : View.VISIBLE);

		final TextView spawnprotect = (TextView) view.findViewById(R.id.config_spawnprotect);
		final MaterialSwitch spawnprotect_toggle = (MaterialSwitch) view.findViewById(R.id.config_spawnprotect_enable);
		spawnprotect_toggle
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton sender,
							boolean checked) {
						if (checked) {
							spawnprotect.setEnabled(true);
							spawnprotect.setText(getString(R.string.auto_java_16));
						} else {
							spawnprotect.setEnabled(false);
							spawnprotect.setText(getString(R.string.auto_java_1));
						}
					}
				});
		 // no
																							// need
																							// to
																							// disable

		final MaterialButton gamemode_survival = (MaterialButton) view.findViewById(R.id.config_survival);
		final MaterialButton gamemode_creative = (MaterialButton) view.findViewById(R.id.config_creative);
		final MaterialButton gamemode_adventure = (MaterialButton) view.findViewById(R.id.config_adventure);
		final MaterialButton gamemode_spectator = (MaterialButton) view.findViewById(R.id.config_spectator);

		final MaterialButton ram64 = (MaterialButton) view.findViewById(R.id.config_ram64);
		final MaterialButton ram128 = (MaterialButton) view.findViewById(R.id.config_ram128);
		final MaterialButton ram256 = (MaterialButton) view.findViewById(R.id.config_ram256);
		final MaterialButton ramCustom = (MaterialButton) view.findViewById(R.id.config_ramCustom);

		final MaterialButton difficulty_peaceful = (MaterialButton) view.findViewById(R.id.config_peaceful);
		final MaterialButton difficulty_easy = (MaterialButton) view.findViewById(R.id.config_easy);
		final MaterialButton difficulty_normal = (MaterialButton) view.findViewById(R.id.config_normal);
		final MaterialButton difficulty_hard = (MaterialButton) view.findViewById(R.id.config_hard);

		OnClickListener gamemodeListener = new OnClickListener() {
			@Override public void onClick(View v) { setGamemode(v); }
		};
		gamemode_survival.setOnClickListener(gamemodeListener);
		gamemode_creative.setOnClickListener(gamemodeListener);
		gamemode_adventure.setOnClickListener(gamemodeListener);
		gamemode_spectator.setOnClickListener(gamemodeListener);

		OnClickListener ramListener = new OnClickListener() {
			@Override public void onClick(View v) { setRAM(v); }
		};
		ram64.setOnClickListener(ramListener);
		ram128.setOnClickListener(ramListener);
		ram256.setOnClickListener(ramListener);
		ramCustom.setOnClickListener(ramListener);

		OnClickListener difficultyListener = new OnClickListener() {
			@Override public void onClick(View v) { setDifficulty(v); }
		};
		difficulty_peaceful.setOnClickListener(difficultyListener);
		difficulty_easy.setOnClickListener(difficultyListener);
		difficulty_normal.setOnClickListener(difficultyListener);
		difficulty_hard.setOnClickListener(difficultyListener);

		final SeekBar viewDistance = (SeekBar) view.findViewById(R.id.config_distance);
		final TextView viewDistanceValue = (TextView) view.findViewById(R.id.config_distance_value);
		final TextView viewDistanceWarning = (TextView) view.findViewById(R.id.config_distance_warning);
		viewDistance.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			Boolean displayingWarning = true;

			@Override
			public void onStopTrackingTouch(SeekBar bar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar bar) {
			}

			@Override
			public void onProgressChanged(SeekBar bar, int progress,
					boolean fromUser) {
				int val = progress + 3;
				viewDistanceValue.setText("" + val);
				if (val > 10 && !displayingWarning) {
					viewDistanceWarning.setVisibility(View.VISIBLE);
					displayingWarning = true;
				} else if (val <= 10 && displayingWarning) {
					viewDistanceWarning.setVisibility(View.GONE);
					displayingWarning = false;
				}
			}
		});

		readFile();
		setValue(R.id.config_name, "server-name", "Minecraft: PE Server");
		setValue(R.id.config_port, "server-port", "19132");

		gamemode_survival.setChecked(false);
		gamemode_creative.setChecked(false);
		gamemode_adventure.setChecked(false);
		gamemode_spectator.setChecked(false);
		if (values.containsKey("gamemode")) {
			String gamemode = values.get("gamemode");
			if (gamemode.equals("0")) {
				gamemode_survival.setChecked(true);
			} else if (gamemode.equals("1")) {
				gamemode_creative.setChecked(true);
			} else if (gamemode.equals("2")) {
				gamemode_adventure.setChecked(true);
			} else if (gamemode.equals("3")) {
				gamemode_spectator.setChecked(true);
			} else {
				gamemode_survival.setChecked(true);
			}
		} else {
			gamemode_survival.setChecked(true);
		}

		setValue(R.id.config_players, "max-players", "20");
		if (values.containsKey("spawn-protection")
				&& !values.get("spawn-protection").equals("-1")) {

			try {
				spawnprotect.setText(Integer.parseInt(values
						.get("spawn-protection")) + ""); // no, that isn't a
															// nonsense
				spawnprotect_toggle.setChecked(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			spawnprotect_toggle.setChecked(false);
		}
		setValue(R.id.config_whitelist, "whitelist", false);

        setValue(R.id.config_spawn_animals, "spawn-animals", true);
        setValue(R.id.config_spawn_mobs, "spawn-mobs", true);
        setValue(R.id.config_force_gamemode, "force-gamemode", false);
        setValue(R.id.config_xbox_auth, "xbox-auth", true);
        setValue(R.id.config_last_update, "last-update", false);
        setValue(R.id.config_enable_ipv6, "enable-ipv6", true);
        setValue(R.id.config_portv6, "server-portv6", "19133");
        setValue(R.id.config_language, "language", "eng");
        setValue(R.id.config_server_type, "server-type", "normal");
        setValue(R.id.config_whitelist, "white-list", false);

		setValue(R.id.config_query, "enable-query", true);
		setValue(R.id.config_rcon, "enable-rcon", false);
		setValue(R.id.config_desc, "description",
				"Server made using PocketMine-MP");
		setValue(R.id.config_motd, "motd", "Welcome @player to server!");
		// setValue(R.id.config_ip, "server-ip", "");
		// *server-type
		ram64.setChecked(false);
		ram128.setChecked(false);
		ram256.setChecked(false);
		ramCustom.setChecked(false);
		if (values.containsKey("memory-limit")) {
			ram = values.get("memory-limit");
			if (ram.endsWith("M")) {
				ram = ram.substring(0, ram.length() - 1);
			}
			if (ram.equals("64")) {
				ram64.setChecked(true);
			} else if (ram.equals("128")) {
				ram128.setChecked(true);
			} else if (ram.equals("256")) {
				ram256.setChecked(true);
			} else {
				try {
					Integer.parseInt(ram);
					ramCustom.setChecked(true);
				} catch (Exception e) {
					ram = "128";
					ram128.setChecked(true);
				}
			}
		} else {
			ram128.setChecked(true);
			ram = "128";
		}

		// *last-update
		setValue(R.id.config_achievements, "announce-player-achievements", true);

		if (values.containsKey("view-distance")) {
			try {
				int v = Integer.parseInt(values.get("view-distance")) - 3;
				if (v >= 0 && v <= 13) {
					viewDistance.setProgress(v);
				}
			} catch (Exception e) {
				e.printStackTrace();
				viewDistance.setProgress(7); // 10-3 = 7
			}
		} else {
			viewDistance.setProgress(7); // 10-3 = 7
		}
		setValue(R.id.config_fly, "allow-flight", false);
		// *spawn-monsters
		// *spawn-mobs
		setValue(R.id.config_hardcore, "hardcore", false);
		setValue(R.id.config_pvp, "pvp", true);

		difficulty_peaceful.setChecked(false);
		difficulty_easy.setChecked(false);
		difficulty_normal.setChecked(false);
		difficulty_hard.setChecked(false);
		if (values.containsKey("difficulty")) {
			String difficulty = values.get("difficulty");
			if (difficulty.equals("0")) {
				difficulty_peaceful.setChecked(true);
			} else if (difficulty.equals("1")) {
				difficulty_easy.setChecked(true);
			} else if (difficulty.equals("2")) {
				difficulty_normal.setChecked(true);
			} else if (difficulty.equals("3")) {
				difficulty_hard.setChecked(true);
			} else {
				difficulty_easy.setChecked(true);
			}
		} else {
			difficulty_easy.setChecked(true);
		}

		setValue(R.id.config_generator_settings, "generator-settings", "");
		setValue(R.id.config_level_name, "level-name", "world");
		setValue(R.id.config_level_seed, "level-seed", "");
		setValue(R.id.config_level_type, "level-type", "DEFAULT");
		setValue(R.id.config_rcon_password, "rcon.password",
				generatePassword(5, 50));
		setValue(R.id.config_autosave, "auto-save", true);
		return view;
	}

	private void setValue(int resId, String name, String defaultValue) {
		TextView tv = (TextView) view.findViewById(resId);
		if (values.containsKey(name)) {
			tv.setText(values.get(name));
		} else {
			tv.setText(defaultValue);
		}
	}

	private void setValue(int resId, String name, Boolean defaultValue) {
		CompoundButton tv = (CompoundButton) view.findViewById(resId);
		if (values.containsKey(name)) {
			tv.setChecked(values.get(name).equals("on"));
		} else {
			tv.setChecked(defaultValue);
		}
	}

	public void save() {
		final MaterialButton gamemode_survival = (MaterialButton) view.findViewById(R.id.config_survival);
		final MaterialButton gamemode_creative = (MaterialButton) view.findViewById(R.id.config_creative);
		final MaterialButton gamemode_adventure = (MaterialButton) view.findViewById(R.id.config_adventure);
		final MaterialButton gamemode_spectator = (MaterialButton) view.findViewById(R.id.config_spectator);

		final MaterialButton difficulty_peaceful = (MaterialButton) view.findViewById(R.id.config_peaceful);
		final MaterialButton difficulty_easy = (MaterialButton) view.findViewById(R.id.config_easy);
		final MaterialButton difficulty_normal = (MaterialButton) view.findViewById(R.id.config_normal);
		final MaterialButton difficulty_hard = (MaterialButton) view.findViewById(R.id.config_hard);

		final SeekBar viewDistance = (SeekBar) view.findViewById(R.id.config_distance);

		putValueString(R.id.config_name, "server-name");
		putValueString(R.id.config_port, "server-port");

		// find out gamemode
		String gamemode = "0";
		if (gamemode_survival.isChecked()) {
			gamemode = "0";
		} else if (gamemode_creative.isChecked()) {
			gamemode = "1";
		} else if (gamemode_adventure.isChecked()) {
			gamemode = "2";
		} else if (gamemode_spectator.isChecked()) {
			gamemode = "3";
		}
		values.put("gamemode", gamemode);

		putValueString(R.id.config_players, "max-players");
		putValueString(R.id.config_spawnprotect, "spawn-protection");
		putValueBool(R.id.config_whitelist, "white-list");

        putValueBool(R.id.config_spawn_animals, "spawn-animals");
        putValueBool(R.id.config_spawn_mobs, "spawn-mobs");
        putValueBool(R.id.config_force_gamemode, "force-gamemode");
        putValueBool(R.id.config_xbox_auth, "xbox-auth");
        putValueBool(R.id.config_last_update, "last-update");
        putValueBool(R.id.config_enable_ipv6, "enable-ipv6");
        putValueString(R.id.config_portv6, "server-portv6");
        putValueString(R.id.config_language, "language");
        putValueString(R.id.config_server_type, "server-type");
        // Also the user mentioned white-list in properties
        putValueBool(R.id.config_whitelist, "white-list");

		putValueBool(R.id.config_query, "enable-query");
		putValueBool(R.id.config_rcon, "enable-rcon");
		// putValueBool(R.id.config_usage, "send-usage");
		putValueString(R.id.config_desc, "description");
		putValueString(R.id.config_motd, "motd");
		// putValueString(R.id.config_ip, "server-ip");
		if (!values.containsKey("server-type"))
			values.put("server-type", "normal");
		values.put("memory-limit", ram + "M");
		if (!values.containsKey("last-update"))
			values.put("last-update", "off");
		putValueBool(R.id.config_achievements, "announce-player-achievements");
		values.put("view-distance", (viewDistance.getProgress() + 3) + "");
		putValueBool(R.id.config_fly, "allow-flight");
		if (!values.containsKey("spawn-animals"))
			values.put("spawn-animals", "on");
		if (!values.containsKey("spawn-mobs"))
			values.put("spawn-mobs", "on");
		putValueBool(R.id.config_hardcore, "hardcore");
		putValueBool(R.id.config_pvp, "pvp");

		String difficulty = "0";
		if (difficulty_peaceful.isChecked()) {
			difficulty = "0";
		} else if (difficulty_easy.isChecked()) {
			difficulty = "1";
		} else if (difficulty_normal.isChecked()) {
			difficulty = "2";
		} else if (difficulty_hard.isChecked()) {
			difficulty = "3";
		}
		values.put("difficulty", difficulty);

		putValueString(R.id.config_generator_settings, "generator-settings");
		putValueString(R.id.config_level_name, "level-name");
		if (values.get("level-name").equals("")) {
			values.put("level-name", "world");
		}
		putValueString(R.id.config_level_seed, "level-seed");
		putValueString(R.id.config_level_type, "level-type");
		putValueString(R.id.config_rcon_password, "rcon.password");
		putValueBool(R.id.config_autosave, "auto-save");

		try {
			PrintWriter writer = new PrintWriter(ServerUtils.getDataDirectory()
					+ "/server.properties");
			for (Map.Entry<String, String> entry : values.entrySet()) {
				writer.println(entry.getKey() + "=" + entry.getValue());
			}
			writer.flush();
			writer.close();
			Toast.makeText(requireActivity(), getString(R.string.auto_java_saved), Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(requireActivity(), getString(R.string.auto_java_saving_failed), Toast.LENGTH_SHORT).show();
		}
	}

	private void putValueString(int resId, String name) {
		TextView tv = (TextView) view.findViewById(resId);
		values.put(name, tv.getText().toString());
	}

	private void putValueBool(int resId, String name) {
		CompoundButton tv = (CompoundButton) view.findViewById(resId);
		values.put(name, tv.isChecked() ? "on" : "off");
	}

	public void setGamemode(View v) {
		final MaterialButton gamemode_survival = (MaterialButton) view.findViewById(R.id.config_survival);
		final MaterialButton gamemode_creative = (MaterialButton) view.findViewById(R.id.config_creative);
		final MaterialButton gamemode_adventure = (MaterialButton) view.findViewById(R.id.config_adventure);
		final MaterialButton gamemode_spectator = (MaterialButton) view.findViewById(R.id.config_spectator);
		gamemode_survival.setChecked(false);
		gamemode_creative.setChecked(false);
		gamemode_adventure.setChecked(false);
		gamemode_spectator.setChecked(false);
		MaterialButton btn = (MaterialButton) v;
		btn.setChecked(true);
	}

	public void setDifficulty(View v) {
		final MaterialButton difficulty_peaceful = (MaterialButton) view.findViewById(R.id.config_peaceful);
		final MaterialButton difficulty_easy = (MaterialButton) view.findViewById(R.id.config_easy);
		final MaterialButton difficulty_normal = (MaterialButton) view.findViewById(R.id.config_normal);
		final MaterialButton difficulty_hard = (MaterialButton) view.findViewById(R.id.config_hard);
		difficulty_peaceful.setChecked(false);
		difficulty_easy.setChecked(false);
		difficulty_normal.setChecked(false);
		difficulty_hard.setChecked(false);
		MaterialButton btn = (MaterialButton) v;
		btn.setChecked(true);
	}

	public void setRAM(View v) {
		final MaterialButton ram64 = (MaterialButton) view.findViewById(R.id.config_ram64);
		final MaterialButton ram128 = (MaterialButton) view.findViewById(R.id.config_ram128);
		final MaterialButton ram256 = (MaterialButton) view.findViewById(R.id.config_ram256);
		final MaterialButton ramCustom = (MaterialButton) view.findViewById(R.id.config_ramCustom);

		if (v == ram64 || v == ram128 || v == ram256) {
			ram64.setChecked(false);
			ram128.setChecked(false);
			ram256.setChecked(false);
			ramCustom.setChecked(false);
		}

		if (v == ram64) {
			ram64.setChecked(true);
			ram = "64";
		} else if (v == ram128) {
			ram128.setChecked(true);
			ram = "128";
		} else if (v == ram256) {
			ram256.setChecked(true);
			ram = "256";
		} else if (v == ramCustom) {
			android.view.View view = android.view.LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_input, null);
			final com.google.android.material.textfield.TextInputEditText input = view.findViewById(R.id.dialog_input);
			input.setInputType(InputType.TYPE_CLASS_NUMBER);
			new MaterialAlertDialogBuilder(requireActivity())
					.setTitle(getString(R.string.auto_java_custom))
					.setMessage(getString(R.string.auto_java_select_the_maximal_amount_of_r))
					.setView(view)
					.setPositiveButton("Done",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									try {
										String out = input.getText().toString();
										Integer.parseInt(out);
										ram = out;
										ram64.setChecked(false);
										ram128.setChecked(false);
										ram256.setChecked(false);
										ramCustom.setChecked(true);
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							})
					.setNegativeButton("Cancel", null).show();
		}
	}

	private void readFile() {
		values = new LinkedHashMap<String, String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					ServerUtils.getDataDirectory() + "/server.properties"));
			try {
				String line;

				while ((line = reader.readLine()) != null) {
					if (!line.startsWith("#")) {
						int iof = line.indexOf("=");
						if (iof == -1) {
							Log.e("Configuration parser", "Invalid entry: "
									+ line);
						} else {
							String name = line.substring(0, iof);
							String value = line.substring(iof + 1);
							Log.d("Configuration parser", "[Parsing] Name: "
									+ name + " Value: " + value);
							values.put(name, value);
						}
					}
				}
			} finally {
				reader.close();
			}
		} catch (FileNotFoundException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	

	private SecureRandom random = new SecureRandom();
	private char[] chars = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890"
			.toCharArray();

	public String generatePassword(int minLen, int maxLen) {
		int len = random.nextInt(maxLen - minLen) + minLen;
		StringBuilder b = new StringBuilder();

		for (int i = 0; i < len; i++) {
			b.append(chars[random.nextInt(chars.length)]);
		}

		return b.toString();
	}

}
