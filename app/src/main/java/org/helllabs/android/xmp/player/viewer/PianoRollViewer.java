package org.helllabs.android.xmp.player.viewer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.RemoteException;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.player.Util;
import org.helllabs.android.xmp.service.ModInterface;

import java.util.Random;


// http://developer.android.com/guide/topics/graphics/2d-graphics.html

public class PianoRollViewer extends Viewer {
	private static final int MAX_NOTES = 120;
	private static final int MAX_CHANNELS = 64;
	private final Paint headerPaint, headerTextPaint, insPaint;
	private final Paint barPaint, muteNotePaint, muteInsPaint;
	private final Paint[] notePaint = new Paint[MAX_CHANNELS];
	private final int fontSize, fontHeight, fontWidth;
	private final String[] allNotes = new String[MAX_NOTES];
	private final String[] hexByte = new String[256];
	private final byte[] rowNotes = new byte[64];
	private final byte[] rowInstruments = new byte[64];
	private int oldRow, oldOrd, oldPosX;
	private final Rect rect = new Rect();

	public PianoRollViewer(final Context context) {
		super(context);

		fontSize = getResources().getDimensionPixelSize(R.dimen.patternview_font_size);

		for (int channel = 0; channel < MAX_CHANNELS; channel++) {
			notePaint[channel] = new Paint();
			notePaint[channel].setARGB(255, 255, 0, 0);
			notePaint[channel].setAntiAlias(true);
		}

		insPaint = new Paint();
		insPaint.setARGB(255, 160, 80, 80);
		insPaint.setTypeface(Typeface.MONOSPACE);
		insPaint.setTextSize(fontSize);
		insPaint.setAntiAlias(true);

		muteNotePaint = new Paint();
		muteNotePaint.setARGB(255, 60, 60, 60);
		muteNotePaint.setTypeface(Typeface.MONOSPACE);
		muteNotePaint.setTextSize(fontSize);
		muteNotePaint.setAntiAlias(true);

		muteInsPaint = new Paint();
		muteInsPaint.setARGB(255, 80, 40, 40);
		muteInsPaint.setTypeface(Typeface.MONOSPACE);
		muteInsPaint.setTextSize(fontSize);
		muteInsPaint.setAntiAlias(true);

		headerTextPaint = new Paint();
		headerTextPaint.setARGB(255, 50, 50, 50);
		headerTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
		headerTextPaint.setTextSize(fontSize);
		headerTextPaint.setAntiAlias(true);

		headerPaint = new Paint();
		headerPaint.setARGB(255, 140, 140, 220);

		barPaint = new Paint();
		barPaint.setARGB(50, 255, 255, 255);

		fontWidth = (int)insPaint.measureText("X");
		fontHeight = fontSize * 12 / 10;

		final char[] c = new char[2];
		for (int i = 0; i < 256; i++) {
			Util.to02X(c, i);
			hexByte[i] = new String(c);
		}
	}
	
	@Override
	public void setup(final ModInterface modPlayer, final int[] modVars) {
		super.setup(modPlayer, modVars);

		oldRow = -1;
		oldOrd = -1;
		oldPosX = -1;

		final int chn = modVars[3];
		setMaxX((chn * 6 + 2) * fontWidth);
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
			// do this in a finally so that if an exception is thrown
			// during the above, we don't leave the Surface in an
			// inconsistent state
			if (canvas != null) {
				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	private void doDraw(final Canvas canvas, final ModInterface modPlayer, final Info info) {
		final int channelCount = modVars[3];
		final int currentPattern = info.values[1];
		final int currentRow = info.values[2];
		final int rowCount = info.values[3];
		final float noteHeight = canvasHeight / MAX_NOTES;
		final float noteWidth = canvasWidth / rowCount;
		final float noteRadius = 5;

		// Clear screen
		canvas.drawColor(Color.BLACK);

		//No header to draw yet


		//Draw Notes
		for (int row = 0; row < rowCount; row++) {
			try {
				modPlayer.getPatternRow(currentPattern, row, rowNotes, rowInstruments);
			} catch (RemoteException e) { }

			for (int channel = 0; channel < channelCount; channel++) {
				if (rowNotes[channel] != 0) {
					float left = row * noteWidth;
					float top = (canvasHeight - noteHeight) - rowNotes[channel] * noteHeight;
					canvas.drawRect(left, top, left + noteWidth, top + noteHeight, notePaint[channel]);
					//canvas.drawRoundRect(left, top, left + noteWidth, top + noteHeight, noteRadius, noteRadius, notePaint[channel]);
				}
			}
		}

		//Draw Position Marker
		canvas.drawRect(currentRow * noteWidth, 0, currentRow * noteWidth + noteWidth, canvasHeight, barPaint);
	}
}
