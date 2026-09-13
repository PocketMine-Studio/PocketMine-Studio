/**
 * This file is part of DroidPHP
 *
 * (c) 2013 Shushant Kumar
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package net.eqozqq.pocketminestudio;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import android.content.Context;
import android.util.Log;

public final class ServerUtils {

	final static String TAG = "com.MrARM.DroidPocketMine.ServerUtils";
	static Context mContext;
	private static java.io.OutputStream stdin;
	private static java.io.InputStream stdout;
	static Process serverProc = null;
	public static boolean isServerReady = false;
	public static long lastBackgroundStatus = 0;
	private static boolean isSchedulerRunning = false;
	private static long lastTxBytes = -1;
	private static long lastRxBytes = -1;
	private static long lastTrafficTime = 0;

	final public static void setContext(Context mContext) {
		ServerUtils.mContext = mContext;
	}

	final public static Context getContext() {
		return mContext;
	}

	final public static String getAppDirectory() {
		if (mContext == null) {
			return "/data/data/net.eqozqq.pocketminestudio";
		}
		return mContext.getApplicationInfo().dataDir;
	}
	
	final public static String getExecDirectory() {
		if (mContext == null) {
			return "/data/data/net.eqozqq.pocketminestudio/lib";
		}
		return mContext.getApplicationInfo().nativeLibraryDir;
	}

	final public static String getDataDirectory() {
		if (mContext == null) {
			return android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/PocketMine-MP";
		}
		File ext = mContext.getExternalFilesDir(null);
		if (ext != null && ext.getParent() != null) {
			return ext.getParent();
		}
		File files = mContext.getFilesDir();
		if (files != null) {
			return files.getAbsolutePath();
		}
		return "/data/data/net.eqozqq.pocketminestudio/files";
	}

	final public static void stopServer() {
		if (serverProc != null) {
		    serverProc.destroy();
			serverProc = null;
		}
    }

	public static Boolean isRunning() {
		if (serverProc != null) {
		    try {
			    serverProc.exitValue();
		    } catch (Exception e) {
			    return true;
			}
		}

		return false;
	}

	final public static void runServer() {
		File f = new File(getDataDirectory(), "tmp/");
		if (!f.exists()) {
			f.mkdir();
		} else if (!f.isDirectory()) {
			f.delete();
			f.mkdir();
		}

		String corePath = getDataDirectory() + "/PocketMine-MP.phar";
		String customCore = null;
		String selectedCore = null;
		if (ServerFragment.prefs != null) {
			customCore = ServerFragment.prefs.getString("custom_core_path", null);
			selectedCore = ServerFragment.prefs.getString("selected_core", null);
		}
		if (customCore != null && new File(customCore).exists()) {
			corePath = customCore;
		} else if (selectedCore != null && new File(getDataDirectory() + "/" + selectedCore).exists()) {
			corePath = getDataDirectory() + "/" + selectedCore;
		} else if (new File(getDataDirectory() + "/BetterAltay.phar").exists() && !new File(getDataDirectory() + "/PocketMine-MP.phar").exists()) {
			corePath = getDataDirectory() + "/BetterAltay.phar";
		} else if (new File(getDataDirectory() + "/PocketMine-MP.phar").exists()) {
			corePath = getDataDirectory() + "/PocketMine-MP.phar";
		} else if (new File(getDataDirectory() + "/BetterAltay.phar").exists()) {
			corePath = getDataDirectory() + "/BetterAltay.phar";
		} else if (new File(getDataDirectory() + "/src/pocketmine/PocketMine.php").exists()) {
			corePath = getDataDirectory() + "/src/pocketmine/PocketMine.php";
		} else {
			File dataDir = new File(getDataDirectory());
			File[] files = dataDir.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile() && file.getName().endsWith(".phar")) {
						corePath = file.getAbsolutePath();
						break;
					}
				}
			}
		}

		String phpDir = (mContext != null ? mContext.getFilesDir().getAbsolutePath() : (getAppDirectory() + "/files")) + "/php";
		File iniFile = new File(phpDir, "php.ini");
		if (iniFile.exists()) {
			try {
				String content = new String(java.nio.file.Files.readAllBytes(iniFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
				boolean changed = false;
				if (content.contains("auto_prepend_file")) {
					content = content.replaceAll("(?m)^auto_prepend_file=.*$\\R?", "");
					changed = true;
				}
				if (!content.contains("include_path")) {
					content += "\ninclude_path=\".:" + phpDir + "\"\n";
					changed = true;
				}
				if (changed) {
					java.nio.file.Files.write(iniFile.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				}
			} catch (Exception e) {}
		}

		File bootstrapFile = new File(phpDir, "bootstrap.php");
		java.util.List<String> cmdList = new java.util.ArrayList<>();
		cmdList.add(getExecDirectory() + "/libphp.so");
		cmdList.add("-c");
		cmdList.add(phpDir);
		if (bootstrapFile.exists() && (corePath.toLowerCase().contains("altay") || !corePath.endsWith("PocketMine-MP.phar"))) {
			cmdList.add("-d");
			cmdList.add("auto_prepend_file=" + bootstrapFile.getAbsolutePath());
		}
		cmdList.add(corePath);
		String[] serverCmd = cmdList.toArray(new String[0]);
		
		try {
			File targetPhpFile = new File(getDataDirectory() + "/src/PocketMinecraftServer.php");
			if (targetPhpFile.exists()) {
				StringBuilder sb = new StringBuilder();
				java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(targetPhpFile));
				String line;
				boolean changed = false;
				while ((line = br.readLine()) != null) {
					if (line.contains("//echo \"\\x1b]0;NostalgiaCore \"")) {
						line = line.replace("//echo \"\\x1b]0;NostalgiaCore \"", "echo \"\\x1b]0;NostalgiaCore \"");
						changed = true;
					} else if (line.contains("// echo \"\\x1b]0;NostalgiaCore \"")) {
						line = line.replace("// echo \"\\x1b]0;NostalgiaCore \"", "echo \"\\x1b]0;NostalgiaCore \"");
						changed = true;
					}
					sb.append(line).append("\n");
				}
				br.close();
				if (changed) {
					java.io.FileWriter fw = new java.io.FileWriter(targetPhpFile);
					fw.write(sb.toString());
					fw.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ProcessBuilder builder = new ProcessBuilder(serverCmd);
		builder.redirectErrorStream(true);
		builder.directory(new File(getDataDirectory()));
		builder.environment().put("TMPDIR", getDataDirectory() + "/tmp");
		builder.environment().put("PHPRC", phpDir);
		try {
			isServerReady = false;
			serverProc = builder.start();
			stdout = serverProc.getInputStream();
			stdin = serverProc.getOutputStream();

			LogActivity.log("[PocketMine] Server is starting...");

			Thread tMonitor = new Thread() {
				private void processLogLine(String line, char c) {
					Log.d(TAG, line);

					String lineNoDate = "";
					int iof = line.indexOf(" ");
					if (iof != -1) {
						lineNoDate = line.substring(iof + 1).replaceAll("\u001B\\[[;\\d]*m", "");
					} else {
						lineNoDate = line.replaceAll("\u001B\\[[;\\d]*m", "");
					}
					
					
					
					if (lineNoDate.contains("Done (") && lineNoDate.contains("For help, type \"help\"")) {
						isServerReady = true;
						net.eqozqq.pocketminestudio.compose.state.ServerState.INSTANCE.setServerReady(true);
						executeCMD("list");
						Context sCtx = mContext != null ? mContext : getContext();
						if (sCtx != null) {
							try {
								if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
									sCtx.startForegroundService(new android.content.Intent(sCtx, ServerService.class));
								} else {
									sCtx.startService(new android.content.Intent(sCtx, ServerService.class));
								}
							} catch (Exception ignored) {}
						}
					}
					
					if ((lineNoDate.startsWith("[CMD] There are ") || lineNoDate.startsWith("Command output | There are "))
							&& requestPlayerRefresh
							&& requestPlayerRefreshCount == -1) {

						try {
							String targetStr = lineNoDate.startsWith("[CMD] ") ? "[CMD] There are " : "Command output | There are ";
							String num = lineNoDate.substring(targetStr.length());
							num = num.substring(0,
									num.indexOf("/"));
							requestPlayerRefreshCount = Integer
									.parseInt(num);

							if (requestPlayerRefreshCount == 0) {
								ServerFragment
										.updatePlayerList(null);
								requestPlayerRefresh = false;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if ((lineNoDate.startsWith("[CMD] ") || lineNoDate.startsWith("Command output | "))
							&& requestPlayerRefresh
							&& requestPlayerRefreshCount != -1) {

						String targetStr = lineNoDate.startsWith("[CMD] ") ? "[CMD] " : "Command output | ";
						String player = lineNoDate.substring(targetStr.length());
						if (!player.startsWith("There are ")) {
							String[] players = player.split(", ");

						ServerFragment
								.updatePlayerList(players);

							requestPlayerRefresh = false;
						}
					} else if (c == '\u0007'
							&& line.startsWith("\u001B]0;")) {
						line = line.substring(4);
						System.out
								.println("[Stat] " + line);
						ServerFragment.setStats(
								getStat(line, "Online"),
								getStat(line, "RAM"),
								getStat(line, "U"),
								getStat(line, "D"),
								getStat(line, "TPS"));
					} else if (line.contains("[Memory Manager] [Cyclic Garbage Collector]")) {
					} else if (lineNoDate.contains("---- Server status ----") ||
							lineNoDate.contains("Uptime: ") ||
							lineNoDate.contains("Current TPS: ") ||
							lineNoDate.contains("Average TPS: ") ||
							lineNoDate.contains("Network upload: ") ||
							lineNoDate.contains("Network download: ") ||
							lineNoDate.contains("Thread count: ") ||
							lineNoDate.contains("Main thread memory: ") ||
							lineNoDate.contains("Total memory: ") ||
							lineNoDate.contains("Peak memory: ") ||
							lineNoDate.contains("Total virtual memory: ") ||
							lineNoDate.contains("Total network upload: ") ||
							lineNoDate.contains("Total network download: ") ||
							(lineNoDate.contains("World \"") && lineNoDate.contains("loaded chunks"))) {
						
						String cleanStatus = lineNoDate.replaceAll("§[0-9a-fk-or]", "").trim();
						if (cleanStatus.contains("Current TPS: ")) {
							String sub = cleanStatus.substring(cleanStatus.indexOf("Current TPS: ") + 13).trim();
							ServerFragment.tps = sub.split("[ %]")[0].trim();
						} else if (cleanStatus.contains("Network upload: ")) {
							String sub = cleanStatus.substring(cleanStatus.indexOf("Network upload: ") + 16).trim();
							String val = sub.split("[ /kKMGB]")[0].trim();
							if (!val.isEmpty()) {
								try {
									float v = Float.parseFloat(val);
									if (v > 0) ServerFragment.upload = val;
								} catch (Exception ignored) {}
							}
						} else if (cleanStatus.contains("Network download: ")) {
							String sub = cleanStatus.substring(cleanStatus.indexOf("Network download: ") + 18).trim();
							String val = sub.split("[ /kKMGB]")[0].trim();
							if (!val.isEmpty()) {
								try {
									float v = Float.parseFloat(val);
									if (v > 0) ServerFragment.download = val;
								} catch (Exception ignored) {}
							}
						} else if (cleanStatus.contains("Total memory: ")) {
							String sub = cleanStatus.substring(cleanStatus.indexOf("Total memory: ") + 14).trim();
							String val = sub.split("[ /kKMGB]")[0].trim();
							if (!val.isEmpty() && (ServerFragment.ram == null || ServerFragment.ram.equals("0 MB"))) {
								ServerFragment.ram = val + " MB";
							}
							ServerFragment.setStats(ServerFragment.online, ServerFragment.ram, ServerFragment.upload, ServerFragment.download, ServerFragment.tps);
						}
						
						if (System.currentTimeMillis() - lastBackgroundStatus >= 2500) {
							LogActivity.log(line);
						}
					} else if (line.contains("Command output |") && line.trim().replaceAll("\u001B\\[[;\\d]*m", "").equals("Command output |")) {
					} else if (line.contains("There are ") && line.contains(" players online")) {
						int start = line.indexOf("There are ") + 10;
						int end = line.indexOf(" players online");
						if (start < end) {
							String num = line.substring(start, end);
							ServerFragment.online = num;
							ServerFragment.setStats(ServerFragment.online, ServerFragment.ram, ServerFragment.upload, ServerFragment.download, ServerFragment.tps);
						}
						LogActivity.log(line);
					} else {
						LogActivity.log(line);

						if (line.contains("] logged in with entity id ")
								|| line.contains("] logged out due to ")) {
							refreshPlayers();
						}
					}
				}

				public void run() {
					InputStreamReader reader = new InputStreamReader(stdout,
							Charset.forName("UTF-8"));
					BufferedReader br = new BufferedReader(reader);
					LogActivity.log("[PocketMine] Server was started.");

					while (isRunning()) {
						try {
							char[] buffer = new char[8192];
							int size = 0;
							StringBuilder s = new StringBuilder();
							while ((size = br.read(buffer, 0, buffer.length)) != -1) {
								for (int i = 0; i < size; i++) {
									char c = buffer[i];
									if (c == '\r') {
									} else if (c == '\n' || c == '\u0007') {
										processLogLine(s.toString(), c);
										s.setLength(0);
									} else {
										s.append(c);
									}
								}
								if (s.length() > 0 && !br.ready()) {
									processLogLine(s.toString(), '\0');
									s.setLength(0);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					try {
						br.close();
					} catch (Exception e) {
						e.printStackTrace();
					}

					LogActivity.log("[PocketMine] Server was stopped.");
					ServerFragment.stopNotifyService();
					ServerFragment.hideStats();
					ServerFragment.upload = "0";
					ServerFragment.download = "0";
					ServerFragment.ram = "0 MB";
					ServerFragment.tps = "20.0";
					ServerFragment.setStats("0/0", "0 MB", "0", "0", "20.0");
					isServerReady = false;
					net.eqozqq.pocketminestudio.compose.state.ServerState.INSTANCE.setServerReady(false);
					net.eqozqq.pocketminestudio.compose.state.ServerState.INSTANCE.setStarted(false);
					ServerFragment.isStarted = false;
					Context ctx = mContext != null ? mContext : getContext();
					if (ctx != null) {
						try {
							ctx.stopService(new android.content.Intent(ctx, ServerService.class));
						} catch (Exception ignored) {}
					}
				}

			};
			tMonitor.start();
			startSchedulerThread();
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (isRunning()) {
						try { Thread.sleep(3000); } catch (Exception ignored) {}
						if (isRunning()) {
							long now = System.currentTimeMillis();
							long elapsedMs = now - lastTrafficTime;
							if (lastTrafficTime > 0 && elapsedMs > 0) {
								try {
									long tx = android.net.TrafficStats.getUidTxBytes(android.os.Process.myUid());
									long rx = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid());
									if (lastTxBytes != -1 && tx >= lastTxBytes) {
										float upRate = ((tx - lastTxBytes) / 1024f) / (elapsedMs / 1000f);
										float downRate = ((rx - lastRxBytes) / 1024f) / (elapsedMs / 1000f);
										ServerFragment.upload = String.format(java.util.Locale.US, "%.2f", upRate);
										ServerFragment.download = String.format(java.util.Locale.US, "%.2f", downRate);
									}
									lastTxBytes = tx;
									lastRxBytes = rx;
								} catch (Exception ignored) {}
							} else {
								lastTxBytes = android.net.TrafficStats.getUidTxBytes(android.os.Process.myUid());
								lastRxBytes = android.net.TrafficStats.getUidRxBytes(android.os.Process.myUid());
							}
							lastTrafficTime = now;

							if (serverProc != null) {
								try {
									int pId = -1;
									try {
										java.lang.reflect.Field f = serverProc.getClass().getDeclaredField("pid");
										f.setAccessible(true);
										pId = f.getInt(serverProc);
									} catch (Exception ignored) {}
									if (pId > 0) {
										java.io.File statusFile = new java.io.File("/proc/" + pId + "/status");
										if (statusFile.exists()) {
											java.io.BufferedReader sbr = new java.io.BufferedReader(new java.io.FileReader(statusFile));
											String stLine;
											while ((stLine = sbr.readLine()) != null) {
												if (stLine.startsWith("VmRSS:")) {
													String kbStr = stLine.substring(6).replaceAll("[^0-9]", "").trim();
													if (!kbStr.isEmpty()) {
														float mb = Float.parseFloat(kbStr) / 1024f;
														ServerFragment.ram = String.format(java.util.Locale.US, "%.1f MB", mb);
													}
													break;
												}
											}
											sbr.close();
										}
									}
								} catch (Exception ignored) {}
							}

							ServerFragment.setStats(ServerFragment.online, ServerFragment.ram, ServerFragment.upload, ServerFragment.download, ServerFragment.tps);

							if (isServerReady) {
								lastBackgroundStatus = System.currentTimeMillis();
								executeCMD("status");
							}
						}
					}
				}
			}).start();

			Log.i(TAG, "PHP is started");
		} catch (java.lang.Exception e) {
			Log.e(TAG, "Unable to start PHP", e);
			LogActivity.log("[PocketMine] Unable to start PHP.");
			LogActivity.log(e.getMessage());
			ServerFragment.stopNotifyService();
			ServerFragment.hideStats();
			stopServer();
		}

		return;

	}

	public static void startSchedulerThread() {
		if (isSchedulerRunning) return;
		isSchedulerRunning = true;
		new Thread(() -> {
			while (true) {
				try { Thread.sleep(5000); } catch (Exception e) {}
				checkSchedulerEvents();
			}
		}).start();
	}

	private static void checkSchedulerEvents() {
		try {
			Context ctx = mContext != null ? mContext : getContext();
			if (ctx == null) return;
			android.content.SharedPreferences prefs = ctx.getSharedPreferences("EventScheduler", Context.MODE_PRIVATE);
			String json = prefs.getString("events", "[]");
			org.json.JSONArray arr = new org.json.JSONArray(json);
			boolean changed = false;
			long now = System.currentTimeMillis();
			
			for (int i = 0; i < arr.length(); i++) {
				org.json.JSONObject obj = arr.getJSONObject(i);
				if (obj.optBoolean("isEnabled", true)) {
					long lastRun = obj.optLong("lastRunTime", 0);
					
					long intervalMs = obj.optLong("intervalMs", 0);
					if (intervalMs <= 0) {
						intervalMs = obj.optInt("intervalMinutes", 20) * 60 * 1000L;
						obj.put("intervalMs", intervalMs);
						changed = true;
					}
					
					if (lastRun == 0) {
						obj.put("lastRunTime", now);
						changed = true;
						continue;
					}
					
					if (now - lastRun >= intervalMs) {
						boolean retryOnFail = obj.optBoolean("retryOnFail", false);
						boolean serverActive = isRunning();
						
						if (serverActive) {
							String action = obj.optString("action", "");
							if ("restart".equals(action)) {
								executeCMD("stop");
								new Thread(() -> {
									try { Thread.sleep(2500); } catch (Exception ignored) {}
									runServer();
								}).start();
							} else if ("stop".equals(action)) {
								executeCMD("stop");
							} else if ("command".equals(action)) {
								String cmd = obj.optString("actionData", "");
								if (!cmd.isEmpty()) {
									executeCMD(cmd);
								}
							}
							obj.put("lastRunTime", now);
							changed = true;
						} else {
							if (!retryOnFail) {
								obj.put("lastRunTime", now);
								changed = true;
							}
						}
					}
				}
			}
			if (changed) {
				prefs.edit().putString("events", arr.toString()).apply();
			}
		} catch (Exception e) {
		}
	}

	public static String getRunningCoreName() {
		String selectedCore = ServerFragment.prefs != null ? ServerFragment.prefs.getString("selected_core", null) : null;
		if ("BetterAltay.phar".equals(selectedCore) || (new File(getDataDirectory() + "/BetterAltay.phar").exists() && !new File(getDataDirectory() + "/PocketMine-MP.phar").exists())) {
			return "BetterAltay";
		}
		return "PocketMine-MP";
	}

	public static String getStat(String line, String stat) {
		int index = line.indexOf(stat + " ");
		if (index == -1) {
			index = line.indexOf(stat + ": ");
			if (index == -1) {
				if (stat.equals("RAM")) {
					return getStat(line, "Memory");
				}
				return "Unknown";
			}
			String result = line.substring(index + stat.length() + 2);
			int iof = result.indexOf(" |");
			if (iof != -1) result = result.substring(0, iof);
			else {
				int iof2 = result.indexOf(" ");
				if (iof2 != -1) result = result.substring(0, iof2);
			}
			return result;
		}
		String result = line.substring(index + stat.length() + 1);
		int iof = result.indexOf(" |");
		if (iof != -1) {
			result = result.substring(0, iof);
		} else {
			int iof2 = result.indexOf(" ");
			if (iof2 != -1) result = result.substring(0, iof2);
		}
		return result;
	}

	private static Boolean requestPlayerRefresh = false;
	private static int requestPlayerRefreshCount = -1;

	public static void refreshPlayers() {
		System.out.println("Refreshing player list");
		requestPlayerRefreshCount = -1;
		requestPlayerRefresh = true;
		executeCMD("list");
	}

	public static boolean checkIfInstalled() {
		File mPM = new File(getDataDirectory() + "/PocketMine-MP.phar");
		File mAltay = new File(getDataDirectory() + "/BetterAltay.phar");
		File mPMSRC = new File(getDataDirectory() + "/src/pocketmine/PocketMine.php");
		String customCore = ServerFragment.prefs != null ? ServerFragment.prefs.getString("custom_core_path", null) : null;
		boolean customExists = customCore != null && new File(customCore).exists();

		boolean anyPhar = false;
		File dataDir = new File(getDataDirectory());
		if (dataDir.exists()) {
			File[] files = dataDir.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile() && file.getName().endsWith(".phar")) {
						anyPhar = true;
						break;
					}
				}
			}
		}

		int saveVer = ServerFragment.prefs != null ? ServerFragment.prefs.getInt(
				"filesVersion", 0) : 0;

		if ((mPM.exists() || mAltay.exists() || mPMSRC.exists() || customExists || anyPhar) && saveVer == 6) {

			return true;

		}

		return false;

	}

	public static int dip2px(int dp) {
		return (int) (dp * android.content.res.Resources.getSystem().getDisplayMetrics().density);
	}

	public static void executeCMD(String CCmd) {

		try {
			stdin.write((CCmd + "\n").getBytes());
			stdin.flush();
		} catch (Exception e) {
			// stdin.close();
			Log.e(TAG, "Cannot execute: " + CCmd, e);

		}
	}
}
