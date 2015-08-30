package org.helllabs.android.xmp.service;

import org.helllabs.android.xmp.service.PlayerCallback;


interface ModInterface {
	void play(in List<String> fileList, int start, boolean shuffle, boolean loopList, boolean keepFirst);
	void add(in List<String> fileList);
	void stop();
	void pause();
	void getInfo(out int[] values);
	void seek(in int seconds);
	int time();
	void getModVars(out int[] vars);
	String getModName();
	String getModType();
	void getChannelData(out int[] volumes, out int[] finalvols, out int[] pans, out int[] instruments, out int[] keys, out int[] periods);
	void getSampleData(boolean trigger, int ins, int key, int period, int chn, int width, out byte[] buffer);
	void nextSong();
	void prevSong(); 
	boolean isPaused();
	boolean toggleLoop();
	boolean toggleAllSequences();
	boolean getLoop();
	boolean getAllSequences();
	String getFileName();
	String[] getInstruments();
	void getPatternRow(int pat, int row, out byte[] rowNotes, out byte[] rowInstruments);
	int mute(int chn, int status);
	boolean deleteFile();
	boolean setSequence(int seq);
	void allowRelease();
	void getSeqVars(out int[] vars);
	boolean hasComment();
	void nextPosition();
	void prevPosition();

	void registerCallback(PlayerCallback cb);
	void unregisterCallback(PlayerCallback cb);
}
