package cz.vitskalicky.lepsirozvrh.view.rozvrhtable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import cz.vitskalicky.lepsirozvrh.model.rozvrh.RozvrhCaption;

/** Custom view for lesson captions */
public class CaptionView extends CellView {
    private RozvrhCaption caption = null;
    private String startTime = "";
    private String endTime = "";
    private String captionText = "";
    private boolean transposed = false;

    public void setTransposed(boolean transposed) {
        this.transposed = transposed;
        invalidate();
        requestLayout();
    }

    public CaptionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setDrawDividers(false, true, true);
    }

    @Override
    public void updateTheme() {
        super.updateTheme();
        backgroundPaint.setColor(clr(t.cHeaderBg()));
        primaryTextPaint.setColor(clr(t.cHeaderPrimaryText()));
        secondaryTextPaint.setColor(clr(t.cHeaderSecondaryText()));
    }

    public RozvrhCaption getCaption() {
        return caption;
    }

    public void setCaption(RozvrhCaption caption) {
        this.caption = caption;
        if (caption == null){
            startTime = "";
            endTime = "";
            captionText = "";
        }else {
            startTime = caption.getBeginTime().toString("HH:mm");
            endTime = caption.getEndTime().toString("HH:mm");
            captionText = caption.getName();
        }
        invalidate();
        requestLayout();
    }

    @Override
    public int getMinimumHeight() {
        int timeHeight = (int) Math.max(secondaryTextPaint.measureText(startTime), secondaryTextPaint.measureText(startTime));
        int captionHeight = primaryTextSize;
        return super.getMinimumHeight() + Math.max(captionHeight, timeHeight);
    }

    @Override
    public int getMinimumWidth() {
        if (transposed) {
            return (int) (super.getSuggestedMinimumWidth() + Math.max(secondaryTextPaint.measureText(startTime), secondaryTextPaint.measureText(endTime)));
        }
        return (int) (super.getSuggestedMinimumWidth() + secondaryTextSize + primaryTextPaint.measureText(captionText) + secondaryTextSize);
    }

    @Override
    protected void onDrawContent(Canvas canvas, int xStart, int yStart, int xEnd, int yEnd) {
        int w = xEnd - xStart;
        int h = yEnd - yStart;

        int actualPrimaryTextSize = primaryTextSize;
        int actualSecondaryTextSize = secondaryTextSize;

        if (transposed) {
            // Row header: number on top, start time in middle, end time at bottom — stacked, no rotation
            float third = h / 3f;
            float cx = xStart + w / 2f;

            float capSize = Math.min(actualPrimaryTextSize, third * 0.85f);
            primaryTextPaint.setTextSize(capSize);
            primaryTextPaint.setTextAlign(Paint.Align.CENTER);
            float timeSize = Math.min(actualSecondaryTextSize, third * 0.85f);
            secondaryTextPaint.setTextSize(timeSize);
            secondaryTextPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(startTime, cx, yStart + third * 0.5f + timeSize * 0.5f, secondaryTextPaint);

            canvas.drawText(captionText, cx, yStart + third + third * 0.5f + capSize * 0.5f, primaryTextPaint);

            canvas.drawText(endTime,   cx, yStart + 2 * third + third * 0.5f + timeSize * 0.5f, secondaryTextPaint);
            return;
        }

        if (actualPrimaryTextSize > h){
            actualPrimaryTextSize = h;
        }
        float startTimeLenght = secondaryTextPaint.measureText(startTime);
        float endTimeLenght = secondaryTextPaint.measureText(endTime);
        if (Math.max(startTimeLenght, endTimeLenght) > h){
            if (startTimeLenght > endTimeLenght){
                actualSecondaryTextSize = (int) (actualSecondaryTextSize / (startTimeLenght / h));
            }else{
                actualSecondaryTextSize = (int) (actualSecondaryTextSize / (endTimeLenght / h));
            }
        }
        primaryTextPaint.setTextSize(actualPrimaryTextSize);
        secondaryTextPaint.setTextSize(actualSecondaryTextSize);

        primaryTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(captionText, w/2f, (h + actualPrimaryTextSize)/2f, primaryTextPaint);

        canvas.rotate(-90);

        Rect startTimeBounds = new Rect();
        secondaryTextPaint.getTextBounds(startTime, 0, startTime.length(), startTimeBounds);
        canvas.drawText(startTime, (yEnd)* -1,startTimeBounds.height() + yStart, secondaryTextPaint);
        canvas.drawText(endTime, (yEnd) * -1,xEnd, secondaryTextPaint);

        canvas.rotate(90);
    }
}
