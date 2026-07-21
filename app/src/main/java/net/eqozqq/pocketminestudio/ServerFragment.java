/**
 * This file is part of DroidPHP
 *
 * (c) 2013 Shushant Kumar
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with requireActivity() source code.
 */
package net.eqozqq.pocketminestudio;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.net.Inet4Address;


import net.eqozqq.pocketminestudio.R;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;

/**
 * Activity to Home Screen
 */

import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class ServerFragment extends Fragment {

	// private final String TAG = "com.github.com.DroidPHP";
	final static int PROJECT_CODE = 143;
	final static int VERSION_MANAGER_CODE = PROJECT_CODE + 1;
	final static int FILE_MANAGER_CODE = VERSION_MANAGER_CODE + 1;
	final static int PROPERTIES_EDITOR_CODE = FILE_MANAGER_CODE + 1;
	final static int PLUGINS_CODE = PROPERTIES_EDITOR_CODE + 1;
	final static int FORCE_CLOSE_CODE = PLUGINS_CODE + 1;
	final static int ABOUT_US_CODE = FORCE_CLOSE_CODE + 1;
	final static int CONSOLE_CODE = ABOUT_US_CODE + 1;
	final static int DEV_CODE = CONSOLE_CODE + 1;
	public static HashMap<String, String> server;
	public static SharedPreferences prefs;

	private Context mContext;
	public static ServerFragment ha = null;

	public static Boolean statsShown = false;
	public static String online = "Unknown";
	public static String ram = "Unknown";
	public static String download = "Unknown";
	public static String upload = "Unknown";
	public static String tps = "Unknown";
	public static String[] players = null;
	
	public static ArrayList<Entry> entriesDownload = new ArrayList<>();
	public static ArrayList<Entry> entriesUpload = new ArrayList<>();
	public static ArrayList<Entry> entriesRam = new ArrayList<>();
	public static int graphTime = 0;
	
	public static String publicIpString = null;
	public static boolean isPublicIpBlurred = true;

	/**
	 * Buttons for managing server state
	 */
	public static Button btn_runServer;
	public static Button btn_stopServer;
	public static Intent servInt;
	public static Boolean isStarted = false;

	public static boolean restartRequested = false;
	public static LayoutInflater inflater;

	public View view;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		inflater = getLayoutInflater();

		ha = this;
		view = inflater.inflate(R.layout.home, container, false);
		mContext = requireActivity();
		
		// requireActivity().startService(new Intent(mContext, ServerService.class));
		prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());

		ServerUtils.setContext(mContext);
		AssetExtractor.extractAssets(requireActivity());
		ServerUtils.startSchedulerThread();

		btn_runServer = (Button) view.findViewById(R.id.RunTime_Http);
		btn_stopServer = (Button) view.findViewById(R.id.RunTime_Http_Kill);

		btn_runServer.setEnabled(true);
		btn_runServer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isStarted) {
					if (ServerUtils.isRunning()) {
						btn_runServer.setText(ha.getString(R.string.msg_stopping_btn));
						btn_runServer.setEnabled(false);
						btn_stopServer.setEnabled(false);
						LogActivity.log(ha.getString(R.string.msg_log_stopping_server));
						ServerUtils.executeCMD("stop");
					}
				} else {
					btn_runServer.setEnabled(false);
					servInt = new Intent(mContext, ServerService.class);
					requireActivity().startService(servInt);
					isStarted = true;
					showStats(true);
					ServerUtils.runServer();
					String msg = ha.getString(R.string.msg_unable_start_server);
					if (ServerUtils.isRunning()) {
						msg = ha.getString(R.string.msg_server_running);
						if (prefs != null && prefs.getBoolean("open_console_on_start", true)) {
							startActivity(new Intent(mContext, LogActivity.class));
						}
					}
					android.widget.Toast.makeText(mContext, msg,
												  android.widget.Toast.LENGTH_LONG).show();
					updateButtonsState();
				}
			}
		});

		btn_stopServer.setEnabled(isStarted);
		btn_stopServer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (ServerUtils.isRunning()) {
					btn_runServer.setText(ha.getString(R.string.msg_stopping_btn));
					btn_runServer.setEnabled(false);
					btn_stopServer.setEnabled(false);
					LogActivity.log(ha.getString(R.string.msg_log_restarting_server));
					restartRequested = true;
					ServerUtils.executeCMD("stop");
				}
			}
		});

		updateButtonsState();

		if (isStarted) {
			showStats(false);
		} else {
		    showStats(true);
		    hideStats();
		}
		
        ha.view.findViewById(R.id.btn_console).setOnClickListener(v -> {
            startActivity(new android.content.Intent(ha.requireActivity(), LogActivity.class));
        });

        ha.view.findViewById(R.id.btn_settings).setOnClickListener(v -> {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(mContext, v);
            popup.getMenu().add(0, 1, 0, ha.getString(R.string.abs_settings));
            popup.getMenu().add(0, 2, 0, ha.getString(R.string.abs_about));
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    startActivity(new android.content.Intent(ha.requireActivity(), SettingsActivity.class));
                    return true;
                } else if (item.getItemId() == 2) {
                    startActivity(new android.content.Intent(ha.requireActivity(), About.class));
                    return true;
                }
                return false;
            });
            popup.show();
        });

		ha.view.findViewById(R.id.btn_version_manager).setOnClickListener(v -> {
			startActivity(new android.content.Intent(ha.requireActivity(), VersionManagerActivity.class));
		});

		ha.view.findViewById(R.id.btn_scheduler).setOnClickListener(v -> {
			startActivity(new android.content.Intent(ha.requireActivity(), SchedulerActivity.class));
		});
		
        ha.view.findViewById(R.id.btn_manage_players).setOnClickListener(v -> {
            startActivity(new android.content.Intent(ha.requireActivity(), ManagePlayersActivity.class));
        });
        ha.view.findViewById(R.id.btn_manage_whitelist).setOnClickListener(v -> {
            startActivity(new android.content.Intent(ha.requireActivity(), ManageWhitelistActivity.class));
        });
        
        ha.view.findViewById(R.id.btn_manage_worlds).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(ha.requireActivity(), ManageGridActivity.class);
            intent.putExtra("path", ServerUtils.getDataDirectory() + "/worlds");
            intent.putExtra("title", "Worlds");
            startActivity(intent);
        });
        
        ha.view.findViewById(R.id.btn_manage_plugins).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(ha.requireActivity(), ManageGridActivity.class);
            intent.putExtra("path", ServerUtils.getDataDirectory() + "/plugins");
            intent.putExtra("title", "Plugins");
            startActivity(intent);
        });
return view;
	}

	public static void updateButtonsState() {
		if (ha != null && ha.getActivity() != null) {
			ha.getActivity().runOnUiThread(() -> {
				if (btn_runServer != null) {
					btn_runServer.setEnabled(true);
					if (isStarted) {
						btn_runServer.setText(ha.getString(R.string.server_kill));
						((com.google.android.material.button.MaterialButton) btn_runServer).setIconResource(R.drawable.ic_stop_circle_24px);
					} else {
						btn_runServer.setText(ha.getString(R.string.server_online));
						((com.google.android.material.button.MaterialButton) btn_runServer).setIconResource(R.drawable.ic_play_circle_24px);
					}
				}
				if (btn_stopServer != null) {
					btn_stopServer.setEnabled(isStarted);
				}
			});
		}
	}

	// http://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device
	public static String getIPAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfaces = Collections
					.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf
						.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress().toUpperCase(Locale.US);
						boolean isIPv4 = (addr instanceof Inet4Address);
						if (useIPv4) {
							if (isIPv4)
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 port
																// suffix
								return delim < 0 ? sAddr : sAddr.substring(0,
										delim);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
		}
		return "Unknown";
	}

	private void actionPlayer(final String cmd) {
		if (players == null)
			return;

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
		builder.setTitle(ha.getString(R.string.auto_java_op_player));
		final CharSequence[] list = new CharSequence[players.length + 1];
		list[0] = "Cancel";
		for (int i = 0; i < players.length; i++) {
			list[i + 1] = players[i];
		}
		builder.setItems(list, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface di, int pos) {
				if (pos == 0) {
					// nothing
				} else {
					ServerUtils.executeCMD(cmd + " " + list[pos]); // in
																	// a
																	// worst
																	// case
																	// the
																	// player
																	// array
																	// can
																	// change
				}
			}
		});
		builder.show();
	}

	private static void setupChart(LineChart chart, java.util.List<Entry> entries, String label, int colorArgb) {
		if (chart == null) return;
		LineDataSet set = new LineDataSet(entries, label);
		boolean coloredGraph = prefs != null && prefs.getBoolean("colored_graph", true);
		if (coloredGraph) {
			set.setColor(colorArgb);
			set.setDrawFilled(true);
			set.setFillColor(colorArgb);
		} else {
			set.setColor(android.graphics.Color.GRAY);
			set.setDrawFilled(false);
		}
		set.setLineWidth(3f);
		set.setDrawCircles(false);
		set.setDrawValues(false);
		LineData data = new LineData(set);
		chart.setData(data);
		chart.getDescription().setEnabled(false);
		chart.getAxisRight().setEnabled(false);
		chart.getXAxis().setDrawLabels(false);
		
		int textColor = android.graphics.Color.parseColor("?android:attr/textColorPrimary" != null ? "#AAAAAA" : "#AAAAAA");
		chart.getAxisLeft().setTextColor(android.graphics.Color.GRAY);
		chart.getXAxis().setTextColor(android.graphics.Color.GRAY);
		chart.getLegend().setTextColor(android.graphics.Color.GRAY);
		
		chart.invalidate();
	}

	private static void updateChart(LineChart chart, java.util.List<Entry> entries, float val) {
		entries.add(new Entry(graphTime, val));
		if (entries.size() > 60) entries.remove(0);
		
		if (chart != null && chart.getData() != null) {
			com.github.mikephil.charting.interfaces.datasets.ILineDataSet set = chart.getData().getDataSetByIndex(0);
			if (set != null) {
				((com.github.mikephil.charting.data.DataSet) set).notifyDataSetChanged();
			}
			chart.getData().notifyDataChanged();
			chart.notifyDataSetChanged();
			chart.setVisibleXRangeMaximum(60);
			chart.moveViewToX(graphTime);
		}
	}

	public static void showStats(Boolean reset) {
		if (reset) {
			online = "Unknown";
			ram = "Unknown";
			download = "Unknown";
			upload = "Unknown";
			tps = "Unknown";
			players = null;
			graphTime = 0;
		}
		if (ha != null && ha.getActivity() != null) {
			ha.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					LinearLayout layout = (LinearLayout) ha
							.view.findViewById(R.id.stats);
					layout.setVisibility(View.VISIBLE);
					statsShown = true;
					TextView ip = (TextView) ha.view.findViewById(R.id.stat_ip);
					ip.setText("IP Address: " + getIPAddress(true));
					ip.setOnLongClickListener(new View.OnLongClickListener() {
						public boolean onLongClick(View v) {
							android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ha.requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
							android.content.ClipData clip = android.content.ClipData.newPlainText("IP Address", getIPAddress(true));
							clipboard.setPrimaryClip(clip);
							android.widget.Toast.makeText(ha.requireActivity(), ha.getString(R.string.auto_java_ip_copied), android.widget.Toast.LENGTH_SHORT).show();
							return true;
						}
					});

					final TextView pubIp = (TextView) ha.view.findViewById(R.id.stat_public_ip);
					if (pubIp != null) {
						if (publicIpString == null) {
							pubIp.setText(ha.getString(R.string.auto_text_public_ip_loading));
							new Thread(new Runnable() {
								public void run() {
									String reqIp = "Unavailable";
									try {
										List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
										for (NetworkInterface intf : interfaces) {
											List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
											for (InetAddress addr : addrs) {
												if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
													String sAddr = addr.getHostAddress();
													boolean isPrivate = sAddr.startsWith("10.") ||
															(sAddr.startsWith("172.") && Integer.parseInt(sAddr.split("\\.")[1]) >= 16 && Integer.parseInt(sAddr.split("\\.")[1]) <= 31) ||
															sAddr.startsWith("192.168.") || sAddr.startsWith("169.254.");
													if (!isPrivate) {
														reqIp = sAddr;
														break;
													}
												}
											}
											if (!reqIp.equals("Unavailable")) break;
										}
									} catch (Exception e) {}
									
									if (reqIp.equals("Unavailable")) {
										String[] urls = new String[] {
											"https://api.ipify.org",
											"https://icanhazip.com",
											"https://ifconfig.me/ip"
										};
										for (String urlStr : urls) {
											try {
												java.net.URLConnection conn = new java.net.URL(urlStr).openConnection();
												conn.setConnectTimeout(3000);
												conn.setReadTimeout(3000);
												java.util.Scanner s = new java.util.Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A");
												if (s.hasNext()) {
													String ip = s.next().trim();
													if (!ip.isEmpty()) {
														reqIp = ip;
														s.close();
														break;
													}
												}
												s.close();
											} catch (Exception e) {}
										}
									}
									
									final String finalIp = reqIp;
									if (ha.getActivity() != null) {
										ha.getActivity().runOnUiThread(new Runnable() {
											public void run() {
												publicIpString = finalIp;
												updatePublicIpView(pubIp);
											}
										});
									}
								}
							}).start();
						} else {
							updatePublicIpView(pubIp);
						}
						
						pubIp.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								isPublicIpBlurred = !isPublicIpBlurred;
								updatePublicIpView(pubIp);
							}
						});
						pubIp.setOnLongClickListener(new View.OnLongClickListener() {
							public boolean onLongClick(View v) {
								if (publicIpString != null) {
									android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ha.requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
									android.content.ClipData clip = android.content.ClipData.newPlainText("Public IP", publicIpString);
									clipboard.setPrimaryClip(clip);
									android.widget.Toast.makeText(ha.requireActivity(), ha.getString(R.string.auto_java_public_ip_copied), android.widget.Toast.LENGTH_SHORT).show();
								}
								return true;
							}
						});
					}

					if (reset || entriesDownload.isEmpty()) {
						entriesDownload.clear();
						entriesUpload.clear();
						entriesRam.clear();
					}
					setupChart((LineChart) ha.view.findViewById(R.id.graph_download), entriesDownload, "Download (kB/s)", android.graphics.Color.parseColor("#00E676"));
					setupChart((LineChart) ha.view.findViewById(R.id.graph_upload), entriesUpload, "Upload (kB/s)", android.graphics.Color.parseColor("#2979FF"));
					setupChart((LineChart) ha.view.findViewById(R.id.graph_ram), entriesRam, "RAM Usage (MB)", android.graphics.Color.parseColor("#FF1744"));

					setStats(online, ram, download, upload, tps);
					updatePlayerList(players);
				}
			});
		}
	}

	public static void updatePublicIpView(TextView tv) {
		if (publicIpString == null) return;
		String fullText = "Public IP: " + publicIpString;
		if (isPublicIpBlurred) {
			String masked = publicIpString.replaceAll(".", "\u2022");
			tv.setText("Public IP: " + masked);
		} else {
			tv.setText(fullText);
		}
	}

	public static void setStats(final String nOnline, final String nRAM,
			final String nUpload, final String nDownload, final String nTPS) {

		online = nOnline;
		ram = nRAM;
		upload = nUpload;
		download = nDownload;
		tps = nTPS;

		if (ha != null && ha.getActivity() != null) {
			ha.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (!statsShown) {
						showStats(true);
					}

					TextView onlineTv = (TextView) ha
							.view.findViewById(R.id.stat_online);
					if (onlineTv != null) onlineTv.setText("Online: " + nOnline);
					TextView tpsTv = (TextView) ha.view.findViewById(R.id.stat_tps);
					if (tpsTv != null) tpsTv.setText("TPS: " + nTPS);

					try {
						float valRam = 0;
						float valUpload = 0;
						float valDownload = 0;
						try { valRam = Float.parseFloat(nRAM.replaceAll("[^0-9\\.]", "")); } catch (Exception e){}
						try { valUpload = Float.parseFloat(nUpload.replaceAll("[^0-9\\.]", "")); } catch (Exception e){}
						try { valDownload = Float.parseFloat(nDownload.replaceAll("[^0-9\\.]", "")); } catch (Exception e){}
						
						updateChart((LineChart) ha.view.findViewById(R.id.graph_ram), entriesRam, valRam);
						updateChart((LineChart) ha.view.findViewById(R.id.graph_upload), entriesUpload, valUpload);
						updateChart((LineChart) ha.view.findViewById(R.id.graph_download), entriesDownload, valDownload);
						graphTime++;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	public static void hideStats() {
		if (ha != null && ha.getActivity() != null) {
			ha.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					LinearLayout layout = (LinearLayout) ha
							.view.findViewById(R.id.stats);
					layout.setVisibility(View.VISIBLE);
					statsShown = true;
					
					TextView onlineTv = (TextView) ha.view.findViewById(R.id.stat_online);
					if (onlineTv != null) onlineTv.setText(ha.getString(R.string.auto_java_online_server_offline));
					TextView tpsTv = (TextView) ha.view.findViewById(R.id.stat_tps);
					if (tpsTv != null) tpsTv.setText(ha.getString(R.string.auto_java_tps_server_offline));
					
					entriesRam.clear();
					entriesDownload.clear();
					entriesUpload.clear();
					graphTime = 0;
					
					try {
						updateChart((LineChart) ha.view.findViewById(R.id.graph_ram), entriesRam, 0);
						updateChart((LineChart) ha.view.findViewById(R.id.graph_upload), entriesUpload, 0);
						updateChart((LineChart) ha.view.findViewById(R.id.graph_download), entriesDownload, 0);
					} catch (Exception e) {}
				}
			});
		}
	}

	public static int dip2px(float dips) {
		return (int) (dips * ha.getResources().getDisplayMetrics().density + 0.5f);
	}

	public static void updatePlayerList(final String[] nPlayers) {
		players = nPlayers;
	}

	public static void stopNotifyService() {

		if (ha != null && servInt != null && ha.getActivity() != null) {
			ha.getActivity().runOnUiThread(new Runnable() {
				public void run() {
					isStarted = false;
					hideStats();
					if (ha.getActivity() != null) ha.getActivity().stopService(servInt);
					if (restartRequested) {
						restartRequested = false;
						if (ha != null && ha.getActivity() != null) {
							servInt = new Intent(ha.getActivity(), ServerService.class);
							ha.getActivity().startService(servInt);
							isStarted = true;
							showStats(true);
							ServerUtils.runServer();
							updateButtonsState();
						}
					} else {
						updateButtonsState();
					}
				}
			});
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		
		if (Build.VERSION.SDK_INT >= 33) {
			if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
				requireActivity().requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
			}
		}
		
		if (!ServerUtils.checkIfInstalled()) {
		    if(ServerFragment.prefs!=null){
		    	SharedPreferences.Editor spe = ServerFragment.prefs.edit();
		    	spe.putInt("filesVersion", 6);
		    	spe.commit();
		    }
			startActivity(new Intent(requireContext(), VersionManagerActivity.class));
		}
	}

	

	

	public static void hangUp() {
		if (ha != null && ha.getActivity() != null) {
			ha.getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					hangUp();
				}
			});
		}
	}

	/*
	 * final protected boolean isServerRunning() throws IOException {
	 * InputStream is; java.io.BufferedReader bf; boolean isRunning = false; try
	 * { is = Runtime.getRuntime().exec("ps").getInputStream(); bf = new
	 * java.io.BufferedReader(new java.io.InputStreamReader(is));
	 * 
	 * String r; while ((r = bf.readLine()) != null) { if (r.contains("php")) {
	 * isRunning = true; break; }
	 * 
	 * } is.close(); bf.close();
	 * 
	 * } catch (IOException e) { e.printStackTrace();
	 * 
	 * } return isRunning;
	 * 
	 * }
	 */

}
