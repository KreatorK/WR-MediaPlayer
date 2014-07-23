package com.example.wrmplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.example.wrmplayer.MusicService.MusicBinder;

public class MainActivity extends Activity implements OnCompletionListener,
		OnSeekBarChangeListener {

	// *** Variables
	// playlist support
	private ArrayList<Song> songsList;
	private ListView songView;

	// service support
	private MusicService musicSrv;
	private Intent playIntent;
	private boolean musicBound = false;

	// buttons for control and info
	private ImageButton btnPlay;
	private ImageButton btnNext;
	private ImageButton btnPrev;
	private TextView artistInfo, songInfo;

	// progress bar support
	private SeekBar songProgressBar;
	private Handler mHandler;
	private Utilities utils;
	private TextView songTotalDurationLabel;
	private TextView songCurrentDurationLabel;

	// ////////////////////////////////////////////////////////
	// *** Methods
	// onCreate()
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// listView for displaying the playlist
		songView = (ListView) findViewById(R.id.song_list);
		songsList = new ArrayList<Song>();

		// retrieve the songs in the songList and sort them
		getSongList();
		sortList();

		// Adding the playlist to ListView
		SongAdapter adapter = new SongAdapter(this, songsList);
		songView.setAdapter(adapter);

		// displaying track info in the central part of the screen
		songInfo = (TextView) findViewById(R.id.songInfoText);
		artistInfo = (TextView) findViewById(R.id.artistInfoText);

		// Play / Pause Button
		btnPlay = (ImageButton) findViewById(R.id.btnPlay);
		btnPlay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// check if already playing
				if (musicSrv.getPlayer().isPlaying()) {
					musicSrv.getPlayer().pause();

					// Changing button image to play button
					btnPlay.setImageResource(R.drawable.btn_play);

				} else {
					// Changing button image to pause button
					btnPlay.setImageResource(R.drawable.btn_pause);

					// progress bar updating
					updateProgressBar();

					// start or resume song
					if (musicSrv.getPlayer().isPlaying())
						startTrack();
					else
						resumeTrack();
				}
			}
		});

		// Next button click event
		btnNext = (ImageButton) findViewById(R.id.btnNext);
		btnNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				int pos = musicSrv.getSongPosn();

				// if the last song in the list, jump to the 1st one
				if (pos < (songsList.size() - 1)) {
					pos++;
				} else
					pos = 0;

				musicSrv.setSong(pos);

				displaySongInfo((int) musicSrv.currentSongId());
				startTrack();
			}
		});

		// Previous button click event
		btnPrev = (ImageButton) findViewById(R.id.btnPrevious);
		btnPrev.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				int pos = musicSrv.getSongPosn();

				if (pos > 0) {
					pos--;
				} else {
					pos = musicSrv.getSongList().size() - 1;
				}

				musicSrv.setSong(pos);

				displaySongInfo((int) musicSrv.currentSongId());
				startTrack();
			}
		});

		// seek bar progress
		songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
		songProgressBar.setOnSeekBarChangeListener(this);
		mHandler = new Handler();
		utils = new Utilities();

		// song progress duration labels
		songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);
		songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
	}

	private void getSongList() {
		// retrieve songs info
		Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		Cursor musicCursor = getContentResolver().query(musicUri, null, null,
				null, null);

		if (musicCursor != null && musicCursor.moveToFirst()) {
			// get columns
			int titleColumn = musicCursor
					.getColumnIndex(MediaStore.Audio.Media.TITLE);
			int idColumn = musicCursor
					.getColumnIndex(MediaStore.Audio.Media._ID);
			int artistColumn = musicCursor
					.getColumnIndex(MediaStore.Audio.Media.ARTIST);

			// add songs to list
			do {
				long thisId = musicCursor.getLong(idColumn);
				String thisTitle = musicCursor.getString(titleColumn);
				String thisArtist = musicCursor.getString(artistColumn);
				songsList.add(new Song(thisId, thisTitle, thisArtist));
			} while (musicCursor.moveToNext());
		}
	}

	// sort the songs by title
	private void sortList() {
		Collections.sort(songsList, new Comparator<Song>() {
			public int compare(Song a, Song b) {
				return a.getTitle().compareTo(b.getTitle());
			}
		});
	}

	// display the info of the current song in the central part
	public void displaySongInfo(int song_id) {
		ContentResolver musicResolver = getContentResolver();
		Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		Cursor musicCursor = musicResolver.query(musicUri, null,
				MediaStore.Audio.Media._ID + "=" + song_id, null, null);

		if (musicCursor != null && musicCursor.moveToFirst()) {
			int titleColumn = musicCursor
					.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
			int artistColumn = musicCursor
					.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);

			String thisTitle = musicCursor.getString(titleColumn);
			String thisArtist = musicCursor.getString(artistColumn);
			songInfo.setText(thisTitle);
			artistInfo.setText(thisArtist);
			musicCursor.close();
		}
	}

	// click event of the list items (the song selected in the playlist)
	public void songPicked(View view) {
		musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
		displaySongInfo((int) musicSrv.currentSongId());
		startTrack();
	}

	// beginning the play
	public void startTrack() {
		musicSrv.playSong();

		// Changing button image to pause button
		btnPlay.setImageResource(R.drawable.btn_pause);

		// not very elegant / temporary solution
		// To do: Synchronized threads approach
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// the completion listener set after the Prepared state of the player
		musicSrv.getPlayer().setOnCompletionListener(this);

		// set Progress bar values
		songProgressBar.setProgress(0);
		songProgressBar.setMax(100);

		// Updating progress bar
		updateProgressBar();
	}

	// used after the Paused state
	public void resumeTrack() {
		musicSrv.resumeSong();

		// Changing button image to pause button
		btnPlay.setImageResource(R.drawable.btn_pause);

		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// set Progress bar values
		songProgressBar.setProgress(0);
		songProgressBar.setMax(100);

		// Updating progress bar
		updateProgressBar();
	}

	// Update timer on seekbar
	private void updateProgressBar() {
		mHandler.postDelayed(mUpdateTimeTask, 100);
	}

	// Background Runnable thread
	private Runnable mUpdateTimeTask = new Runnable() {
		@Override
		public void run() {

			long totalDuration = musicSrv.getPlayer().getDuration();
			long currentDuration = musicSrv.getPlayer().getCurrentPosition();

			// Displaying Total Duration time
			songCurrentDurationLabel.setText("" + utils.milliSecondsToTimer(totalDuration));
			//Log.d("TOTAL DURATION", "" + totalDuration);
			
			// Displaying time completed playing
			songTotalDurationLabel.setText(""
					+ utils.milliSecondsToTimer(currentDuration));
			//Log.d("CURRENT DURATION", "" + currentDuration);
			
			// Updating progress bar
			int progress = (int) (utils.getProgressPercentage(currentDuration,
					totalDuration));
			songProgressBar.setProgress(progress);

			// Running this thread after 100 milliseconds
			mHandler.postDelayed(this, 100);
		}
	};

	// connect to the service
	private ServiceConnection musicConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			MusicBinder binder = (MusicBinder) service;
			// get service
			musicSrv = binder.getService();
			// pass list
			musicSrv.setList(songsList);
			musicBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			musicBound = false;
		}
	};

	// the service is binded and started
	@Override
	protected void onStart() {
		super.onStart();
		if (playIntent == null) {
			playIntent = new Intent(this, MusicService.class);
			bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
			startService(playIntent);
		}
	}



	// ///////////////////////////////////////////
	// *** Overriden methods

	// menu inflater
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	// EXIT the program from the menu icon
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// menu item selected
		if (item.getItemId() == R.id.action_end) {
			stopService(playIntent);
			musicSrv = null;
			System.exit(0);
		}
		return super.onOptionsItemSelected(item);
	}

	// the back button will only close the activity, the service will still
	// running
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// remove message Handler from updating progress bar
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		mHandler.removeCallbacks(mUpdateTimeTask);
		int totalDuration = musicSrv.getPlayer().getDuration();
		int currentPosition = utils.progressToTimer(seekBar.getProgress(),
				totalDuration);

		// forward or backward to certain seconds
		musicSrv.getPlayer().seekTo(currentPosition);

		// update timer progress again
		updateProgressBar();
	}

	// at the end of the track the next track will begin
	@Override
	public void onCompletion(MediaPlayer arg0) {
		musicSrv.getPlayer().reset();

		// same code as in NextButton listener
		int pos = musicSrv.getSongPosn();

		if (pos < (songsList.size() - 1)) {
			pos++;
		} else
			pos = 0;

		musicSrv.setSong(pos);

		displaySongInfo((int) musicSrv.currentSongId());
		startTrack();
	}

	// back button to act like the home button
	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}
}
