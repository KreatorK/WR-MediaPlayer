package com.example.wrmplayer;

public class Song {
	// *** Variables
	private long id;
	private String title;
	private String artist;

	// *** Constructor
	public Song(long songID, String songTitle, String songArtist) {
		id = songID;
		title = songTitle;
		artist = songArtist;
	}

	// *** Getters
	public long getID() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getArtist() {
		return artist;
	}

}
