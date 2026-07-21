/**
 * This file is part of DroidPHP
 *
 * (c) 2013 Shushant Kumar
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
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
	private boolean isRunning = false;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		run();

		return (START_NOT_STICKY);
	}

	@Override
	public void onDestroy() {
		stop();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return (null);
	}

	@SuppressWarnings("deprecation")
	private void run() {
		if (!isRunning) {

			isRunning = true;
			
			Context context = getApplicationContext();
			
			Intent i = new Intent(context, HomeActivity.class);

			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					   | Intent.FLAG_ACTIVITY_SINGLE_TOP);

			PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE);
			
			Notification.Builder builder = new Notification.Builder(this)
			    .setSmallIcon(R.drawable.ic_network_node_24px)
				.setContentIntent(pi)
				.setContentTitle("PocketMine-MP is running")
				.setContentText("Tap here to open PocketMine-Studio.")
				.setWhen(System.currentTimeMillis());
				
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel(
					"pmmp_channel",
					"PocketMine-MP Notifications",
					NotificationManager.IMPORTANCE_HIGH
				);
				NotificationManager nm = getSystemService(NotificationManager.class);
				nm.createNotificationChannel(channel);
				builder.setChannelId("pmmp_channel");
			}
			
			Notification note = builder.build();
			note.flags |= Notification.FLAG_NO_CLEAR;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			    startForeground(1337, note, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
			} else {
			    startForeground(1337, note);
			}
		}
	}

	private void stop() {
		if (isRunning) {

			isRunning = false;
			stopForeground(true);
		}
	}
}
