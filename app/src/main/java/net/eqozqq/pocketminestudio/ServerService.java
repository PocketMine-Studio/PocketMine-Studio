package net.eqozqq.pocketminestudio;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import net.eqozqq.pocketminestudio.R;
import android.os.Build;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class ServerService extends Service {
	private static ServerService instance = null;
	private static boolean isRunning = false;
	private static final int NOTIFICATION_ID = 1337;
	private static final String CHANNEL_ID = "pmmp_channel";

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		instance = this;
		run();
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		stop();
		instance = null;
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private static Notification buildNotification(Context context, String online, String tps, String ram, String upload, String download) {
		Intent i = new Intent(context, HomeActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_IMMUTABLE);

		String core = ServerUtils.getRunningCoreName();
		if (core == null || core.isEmpty()) {
			core = "Server";
		}
		String title = core + " \u2022 Online";
		String text = "TPS: " + tps + " | Online: " + online + " | RAM: " + ram;

		Notification.Builder builder = new Notification.Builder(context)
				.setSmallIcon(R.drawable.ic_network_node_24px)
				.setContentIntent(pi)
				.setContentTitle(title)
				.setContentText(text)
				.setOngoing(true)
				.setOnlyAlertOnce(true)
				.setWhen(System.currentTimeMillis());

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
					CHANNEL_ID,
					"PocketMine-MP Notifications",
					NotificationManager.IMPORTANCE_LOW
			);
			channel.setShowBadge(false);
			NotificationManager nm = context.getSystemService(NotificationManager.class);
			if (nm != null) {
				nm.createNotificationChannel(channel);
			}
			builder.setChannelId(CHANNEL_ID);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			String bigText = "Status: Online\nTPS: " + tps + "\nPlayers: " + online + "\nRAM: " + ram;
			if (upload != null && download != null && (!upload.equals("0") || !download.equals("0"))) {
				bigText += "\nUpload: " + upload + " | Download: " + download;
			}
			builder.setStyle(new Notification.BigTextStyle().bigText(bigText));
		}

		return builder.build();
	}

	@SuppressWarnings("deprecation")
	private void run() {
		if (!isRunning) {
			isRunning = true;
			String online = ServerFragment.online != null ? ServerFragment.online : "0";
			String tps = ServerFragment.tps != null ? ServerFragment.tps : "20.0";
			String ram = ServerFragment.ram != null ? ServerFragment.ram : "0 MB";
			String upload = ServerFragment.upload != null ? ServerFragment.upload : "0";
			String download = ServerFragment.download != null ? ServerFragment.download : "0";

			Notification note = buildNotification(this, online, tps, ram, upload, download);
			note.flags |= Notification.FLAG_NO_CLEAR;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				startForeground(NOTIFICATION_ID, note, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
			} else {
				startForeground(NOTIFICATION_ID, note);
			}
		}
	}

	public static void updateStats(Context context, String online, String tps, String ram, String upload, String download) {
		if (!isRunning || context == null) {
			return;
		}
		try {
			NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			if (nm != null) {
				Notification note = buildNotification(context, online, tps, ram, upload, download);
				note.flags |= Notification.FLAG_NO_CLEAR;
				nm.notify(NOTIFICATION_ID, note);
			}
		} catch (Exception ignored) {}
	}

	private void stop() {
		isRunning = false;
		stopForeground(true);
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (nm != null) {
			nm.cancel(NOTIFICATION_ID);
		}
		stopSelf();
	}

	public static boolean isServiceRunning() {
		return isRunning;
	}
}
