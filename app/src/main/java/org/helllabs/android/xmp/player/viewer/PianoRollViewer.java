package org.helllabs.android.xmp.player.viewer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.RemoteException;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.service.ModInterface;

public class PianoRollViewer extends Viewer {
	private static final int MAX_NOTES = 96;
	private static final int MAX_CHANNELS = 64;
	private static final float PLAYED_NOTE_SIZE_COEFFICIENT = 1.3f;
	private static final float CHANNEL_SCROLL_WIDTH_COEFFICIENT = 1.5f;
	private static final float NOTE_RADIUS_COEFFICIENT = 0.4f;
	private static final int DRAW_ALL_CHANNELS_CODE = -1;
	private static final boolean SCROLLABLE_CHANNELS_ENABLED = false;
	private final Paint[] notePaint = new Paint[MAX_CHANNELS];
	private final Paint barPaint;
	private final Paint mutedPaint;
	private final byte[] rowNotes = new byte[64];
	private final byte[] rowInstruments = new byte[64];
	private int oldRow, oldOrd, oldPosX;
	private int channelToDraw = DRAW_ALL_CHANNELS_CODE;
	private int maxChannelScrollX;

	public PianoRollViewer(final Context context) {
		super(context);

		for (int channel = 0; channel < MAX_CHANNELS; channel += 8) {
			setupChannelPaint(channel + 0, getResources().getColor(R.color.track0_color));
			setupChannelPaint(channel + 1, getResources().getColor(R.color.track1_color));
			setupChannelPaint(channel + 2, getResources().getColor(R.color.track2_color));
			setupChannelPaint(channel + 3, getResources().getColor(R.color.track3_color));
			setupChannelPaint(channel + 4, getResources().getColor(R.color.track4_color));
			setupChannelPaint(channel + 5, getResources().getColor(R.color.track5_color));
			setupChannelPaint(channel + 6, getResources().getColor(R.color.track6_color));
			setupChannelPaint(channel + 7, getResources().getColor(R.color.track7_color));
		}

		barPaint = new Paint();
		barPaint.setARGB(50, 255, 255, 255);
		mutedPaint = new Paint();
		mutedPaint.setARGB(255, 0, 0, 0);
	}
	
	@Override
	public void setup(final ModInterface modPlayer, final int[] modVars) {
		super.setup(modPlayer, modVars);

		oldRow = -1;
		oldOrd = -1;
		oldPosX = -1;
		if (SCROLLABLE_CHANNELS_ENABLED) {
			maxChannelScrollX = (int) (canvasWidth * CHANNEL_SCROLL_WIDTH_COEFFICIENT);
			setMaxX(maxChannelScrollX);
		}
	}

	@Override
	public void update(final Info info, final boolean paused) {
		super.update(info, paused);

		final int row = info.values[2];
		final int ord = info.values[0];

		if (oldRow == row && oldOrd == ord && oldPosX == (int)posX) {
			return;
		}

		final int numRows = info.values[3];
		Canvas canvas = null;

		if (numRows != 0) {		// Skip first invalid infos
			oldRow = row;
			oldOrd = ord;
			oldPosX = (int)posX;
		}

		try {
			canvas = surfaceHolder.lockCanvas(null);
			if (canvas != null) {
				synchronized (surfaceHolder) {
					doDraw(canvas, modPlayer, info);
				}
			}
		} finally {
			if (canvas != null) {
				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	private void setupChannelPaint(int channel, int color) {
		notePaint[channel] = new Paint();
		notePaint[channel].setColor(color);
		notePaint[channel].setAntiAlias(true);
	}

	private void doDraw(final Canvas canvas, final ModInterface modPlayer, final Info info) {
		final int channelCount = modVars[3];
		final int currentPattern = info.values[1];
		final int currentRow = info.values[2];
		final int rowCount = info.values[3];
		final float noteHeight = (float) canvasHeight / MAX_NOTES;
		final float noteWidth = (float) canvasWidth / rowCount;
		final float noteRadius = NOTE_RADIUS_COEFFICIENT * Math.min(noteHeight, noteWidth);
		determineChannelToDraw();

		// Clear screen
		canvas.drawColor(Color.argb(255, 0, 0, 0));

		//No header to draw yet


		//Draw Notes
		for (int row = 0; row < rowCount; row++) {
			try {
				modPlayer.getPatternRow(currentPattern, row, rowNotes, rowInstruments);
			} catch (RemoteException e) { }

			for (int channel = 0; channel < channelCount; channel++) {
				if (channelToDraw == DRAW_ALL_CHANNELS_CODE || channelToDraw % channelCount == channel) {
					int rowNote = rowNotes[channel] - 12;
					if (rowNote != -12 && rowNote < MAX_NOTES) {
						float left = row * noteWidth;
						float top = (canvasHeight - noteHeight) - rowNote * noteHeight;

						if (row != currentRow) {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
								canvas.drawRoundRect(left, top, left + noteWidth, top + noteHeight,
										noteRadius, noteRadius, notePaint[channel]);
							} else {
								canvas.drawRect(left, top, left + noteWidth, top + noteHeight,
										notePaint[channel]);
							}
						} else if (!isMuted[channel]) {
							float extraMarginWidth = (PLAYED_NOTE_SIZE_COEFFICIENT - 1) * noteWidth;
							float extraMarginHeight = (PLAYED_NOTE_SIZE_COEFFICIENT - 1) * noteHeight;
							canvas.drawRect(left - extraMarginWidth, top - extraMarginHeight,
									left + noteWidth + extraMarginWidth,
									top + noteHeight + extraMarginHeight, notePaint[channel]);
						}
						if (isMuted[channel]) {
							canvas.drawRect(left + 1, top + 1, left + noteWidth - 1, top + noteHeight - 1, mutedPaint);
						}
					}
				}
			}
		}

		//Draw Position Marker
		canvas.drawRect(currentRow * noteWidth, 0, currentRow * noteWidth + noteWidth, canvasHeight, barPaint);
	}

	public void determineChannelToDraw() {
		if (!SCROLLABLE_CHANNELS_ENABLED || posX == 0 || posX == maxChannelScrollX) {
			channelToDraw = DRAW_ALL_CHANNELS_CODE;
		} else {
			int touchX = (int) (8 * posX / canvasWidth);
			int touchY = (int) (8 * posY / canvasHeight);
			channelToDraw = touchX + (8 * touchY);
		}
	}
}
