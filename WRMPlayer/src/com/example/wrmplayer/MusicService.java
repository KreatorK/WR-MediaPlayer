package com.example.wrmplayer;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class MusicService extends Service implements OnPreparedListener,
		OnErrorListener {
	// ///////////////////////////////////////////////////
	// *** Variables

	private MediaPlayer player;
	private ArrayList<Song> songs;
	private int songPosn;

	// activity - service binder
	private final IBinder musicBind = new MusicBinder();

	// notification ID
	private static final int NOTIFICATION_ID = 1;

	// ///////////////////////////////////////////////////
	// *** Methods

	public void onCreate() {
		super.onCreate();
		// initialize position
		songPosn = 0;
		// create player
		player = new MediaPlayer();

		initMusicPlayer();

		// notification section
		prepareNotification();

	} // end onCreate()

	// ///////////////////////////////////////////////////////////
	// *** Methods
	// player init
	private void initMusicPlayer() {
		// set player properties
		player.setWakeMode(getApplicationContext(),
				PowerManager.PARTIAL_WAKE_LOCK);
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);

		player.setOnPreparedListener(this);
		//player.setOnCompletionListener(this);
		player.setOnErrorListener(this);
	}

	// start the selected track
	public void playSong() {
		player.reset();

		// get current song
		Song playSong = songs.get(songPosn);

		// get id
		long currSong = playSong.getID();

		Uri trackUri = ContentUris.withAppendedId(
				android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				currSong);

		try {
			player.setDataSource(getApplicationContext(), trackUri);
		} catch (Exception e) {
			Log.e("MUSIC SERVICE", "Error setting data source", e);
			e.printStackTrace();
		}

		// when prepared, the onPrepared method will trigger
		player.prepareAsync();
	}

	// resume a paused song
	public void resumeSong() {
		int length = player.getCurrentPosition();
		player.seekTo(length);
		player.start();
	}

	// Next button
	public void playNext() {
		player.reset();

		if (songPosn < (songs.size() - 1)) {
			songPosn++;
		} else
			songPosn = 0;

		playSong();
	}

	// Previous button
	public void playPrev() {
		player.reset();
		//Log.d("Check", " " + songPosn);

		if (songPosn > 0) {
			songPosn--;
		} else
			songPosn = songs.size() - 1;

		playSong();
	}

	// notification
	@SuppressLint("NewApi")
	private void prepareNotification() {
		final Intent notificationIntent = new Intent(getApplicationContext(),
				MainActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		// verify device version for implementing two different implementations
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;

		// for API 16 and above versions
		if (currentapiVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			final Notification notification = new Notification.Builder(
					getApplicationContext())
					.setSmallIcon(android.R.drawable.ic_media_play)
					.setOngoing(true).setContentTitle("Music Playing")
					.setContentText("Click to Access Music Player")
					.setContentIntent(pendingIntent).build();
			// Put this Service in a foreground state
			startForeground(NOTIFICATION_ID, notification);

			// for phones running an SDK before API 16 (Jelly Bean)
		} else {
			@SuppressWarnings("deprecation")
			final Notification notification = new Notification.Builder(
					getApplicationContext())
					.setSmallIcon(android.R.drawable.ic_media_play)
					.setOngoing(true).setContentTitle("Music Playing")
					.setContentText("Click to Access Music Player")
					.setContentIntent(pendingIntent).getNotification();
			// Put this Service in a foreground state
			startForeground(NOTIFICATION_ID, notification);
		}
	}

	// class for binding the activity with the service
	public class MusicBinder extends Binder {
		MusicService getService() {
			return MusicService.this;
		}
	}

	//
	//
	// /////////////////////////////////////////
	// *** Setters
	public void setList(ArrayList<Song> theSongs) {
		songs = theSongs;
	}

	public void setSong(int songIndex) {
		songPosn = songIndex;
	}

	// ///////////////////////////////
	// *** Getters
	public MediaPlayer getPlayer() {
		return player;
	}

	public boolean inPlay() {
		return player.isPlaying();
	}

	public int getPosition() {
		return player.getCurrentPosition();
	}

	public void seek(int posn) {
		player.seekTo(posn);
	}

	public long currentSongId() {
		return songs.get(songPosn).getID();
	}

	public int getSongPosn() {
		return songPosn;
	}

	public ArrayList<Song> getSongList() {
		return songs;
	}

	// ///////////////////////////////////////////
	// *** Overriden methods

	@Override
	public boolean onError(MediaPlayer mp, int arg1, int arg2) {
		mp.reset();
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.start();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return musicBind;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		player.stop();
		player.release();
		return false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopForeground(true);
	}

}
