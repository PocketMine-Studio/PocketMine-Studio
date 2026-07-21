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

	final public static void setContext(Context mContext) {
		ServerUtils.mContext = mContext;

	}

	final public static String getAppDirectory() {

		return mContext.getApplicationInfo().dataDir;

	}
	
	final public static String getExecDirectory() {

		return mContext.getApplicationInfo().nativeLibraryDir;

	}

	final public static String getDataDirectory() {

		return mContext.getExternalFilesDir(null).getParent();

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
		if (ServerFragment.prefs != null) {
			customCore = ServerFragment.prefs.getString("custom_core_path", null);
		}
		if (customCore != null && new File(customCore).exists()) {
			corePath = customCore;
		} else {
			if (new File(getDataDirectory() + "/src/pocketmine/PocketMine.php").exists()) {
				corePath = getDataDirectory() + "/src/pocketmine/PocketMine.php";
			} else if (new File(getDataDirectory() + "/PocketMine-MP.phar").exists()) {
				corePath = getDataDirectory() + "/PocketMine-MP.phar";
			}
		}

		String phpDir = mContext.getFilesDir().getAbsolutePath() + "/php";
		String[] serverCmd = {
			getExecDirectory() + "/libphp.so",
			"-c", phpDir,
			corePath
		};
		
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
		builder.environment().put("PHPRC", mContext.getFilesDir().getAbsolutePath() + "/php");
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
						executeCMD("list");
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
					} else if (line.contains("Command output |") && (
							line.contains("---- Server status ----") ||
							line.contains("Uptime: ") ||
							line.contains("Current TPS: ") ||
							line.contains("Average TPS: ") ||
							line.contains("Network upload: ") ||
							line.contains("Network download: ") ||
							line.contains("Thread count: ") ||
							line.contains("Main thread memory: ") ||
							line.contains("Total memory: ") ||
							line.contains("Peak memory: ") ||
							line.contains("Total virtual memory: ") ||
							line.contains("Total network upload: ") ||
							line.contains("Total network download: ") ||
							(line.contains("World \"") && line.contains("loaded chunks")))) {
						
						if (line.contains("Current TPS: ")) {
							ServerFragment.tps = line.substring(line.indexOf("Current TPS: ") + 13).split(" ")[0];
						} else if (line.contains("Network upload: ")) {
							ServerFragment.upload = line.substring(line.indexOf("Network upload: ") + 16).split(" ")[0];
						} else if (line.contains("Network download: ")) {
							ServerFragment.download = line.substring(line.indexOf("Network download: ") + 18).split(" ")[0];
						} else if (line.contains("Main thread memory: ")) {
							ServerFragment.ram = line.substring(line.indexOf("Main thread memory: ") + 20).split(" ")[0];
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
				}

			};
			tMonitor.start();
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (isRunning()) {
						try { Thread.sleep(4000); } catch (Exception e) {}
						if (isRunning() && isServerReady) {
							lastBackgroundStatus = System.currentTimeMillis();
							executeCMD("status");
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
			android.content.SharedPreferences prefs = mContext.getSharedPreferences("EventScheduler", Context.MODE_PRIVATE);
			String json = prefs.getString("events", "[]");
			org.json.JSONArray arr = new org.json.JSONArray(json);
			boolean changed = false;
			long now = System.currentTimeMillis();
			
			for (int i = 0; i < arr.length(); i++) {
				org.json.JSONObject obj = arr.getJSONObject(i);
				if (obj.getBoolean("isEnabled")) {
					long lastRun = obj.optLong("lastRunTime", now);
					
					long intervalMs = obj.optLong("intervalMs", 0);
					if (intervalMs == 0) {
						intervalMs = obj.optInt("intervalMinutes", 20) * 60 * 1000L;
						obj.put("intervalMs", intervalMs);
						changed = true;
					}
					
					if (now - lastRun >= intervalMs) {
						boolean retryOnFail = obj.optBoolean("retryOnFail", false);
						boolean serverActive = isRunning() && isServerReady;
						
						if (serverActive) {
							String action = obj.getString("action");
							if ("restart".equals(action)) {
								ServerFragment.restartRequested = true;
								executeCMD("stop");
							} else if ("stop".equals(action)) {
								ServerFragment.restartRequested = false;
								executeCMD("stop");
							} else if ("command".equals(action)) {
								executeCMD(obj.optString("actionData", ""));
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

	public static String getStat(String line, String stat) {
		int index = line.indexOf(stat + " ");
		if (index == -1) {
			index = line.indexOf(stat + ": ");
			if (index == -1) return "Unknown";
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
		File mPMPhar = new File(getDataDirectory() + "/PocketMine-MP.phar");
		File mPMSRC = new File(getDataDirectory() + "/src/pocketmine/PocketMine.php");

		int saveVer = ServerFragment.prefs != null ? ServerFragment.prefs.getInt(
				"filesVersion", 0) : 0;

		// File mMySql = new File(getAppDirectory() + "/mysqld");
		// File mLighttpd = new File(getAppDirectory() + "/lighttpd");
		// File mMySqlMon = new File(getAppDirectory() + "/mysql-monitor");

		if ((mPM.exists() || mPMPhar.exists() || mPMSRC.exists()) && saveVer == 6) {

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
