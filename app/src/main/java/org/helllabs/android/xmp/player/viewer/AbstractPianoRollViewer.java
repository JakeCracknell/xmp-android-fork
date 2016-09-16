package org.helllabs.android.xmp.player.viewer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.service.ModInterface;

public abstract class AbstractPianoRollViewer extends Viewer {
    protected static final int MAX_CHANNELS = 64;
    protected static final int MAX_NOTES = 96;

    protected static final boolean ROUNDED_RECTANGLES_ENABLED = false;
    protected final Paint[] noteFillPaint = new Paint[MAX_CHANNELS];
    protected final Paint[] noteOutlinePaint = new Paint[MAX_CHANNELS];
    protected final Paint barPaint;

    protected final byte[] rowNotes = new byte[64];
    protected final byte[] rowInstruments = new byte[64];
    protected int oldRow;
    protected int oldOrd;
    protected int oldPosX;

    public AbstractPianoRollViewer(Context context) {
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
    }

    private void setupChannelPaint(int channel, int color) {
        noteFillPaint[channel] = new Paint();
        noteFillPaint[channel].setColor(color);
        noteFillPaint[channel].setAntiAlias(true);
        noteOutlinePaint[channel] = new Paint();
        noteOutlinePaint[channel].setColor(color);
        noteOutlinePaint[channel].setAntiAlias(true);
        noteOutlinePaint[channel].setStyle(Paint.Style.STROKE);
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

    protected abstract void doDraw(final Canvas canvas, final ModInterface modPlayer, final Info info);
}
