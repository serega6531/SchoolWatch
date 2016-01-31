package ru.serega6531.schoolwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class WatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    private static final int MSG_UPDATE_TIME = 0;

    private static final int[] SCHOOL_EVENTS_TIME_LONG = new int[]{t(8, 30), t(9, 15), t(9, 25), t(10,10), t(10, 20), t(11, 5), t(11, 15), t(12, 0), t(12, 20), t(13, 5), t(13, 25), t(14, 10), t(14, 20), t(15, 5)};
    private static final int[] SCHOOL_EVENTS_TIME_SHORT = new int[]{t(8, 30), t(9, 15), t(9, 25), t(10,10), t(10, 20), t(11, 5), t(11, 15), t(12, 0), t(12, 20), t(13, 5), t(13, 25), t(14, 10)};

    private static int t(int h, int m){
        return h*60 + m;
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        Paint mTextPaint;
        Paint mSubTextPaint;
        Paint mDateTextPaint;

        boolean mAmbient;

        Time mTime;

        float mXOffset;
        float mYOffset;
        float mSubXOffset;
        float mSubYOffset;
        float mDateXOffset;
        float mDateYOffset;

        float textSize;
        float largeTextSize;
        float subTextSize;
        float dateTextSize;

        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            Resources resources = WatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mSubYOffset = resources.getDimensionPixelOffset(R.dimen.digital_sub_y_offset);
            mDateYOffset = resources.getDimensionPixelOffset(R.dimen.digital_date_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.digital_background));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mSubTextPaint = new Paint();
            mSubTextPaint = createTextPaint(resources.getColor(R.color.digital_subtext));

            mDateTextPaint = new Paint();
            mDateTextPaint = createTextPaint(resources.getColor(R.color.digital_datetext));

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = WatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            mSubXOffset = mXOffset;
            mDateXOffset = mXOffset;
            textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            largeTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_large_text_size_round : R.dimen.digital_large_text_size);
            subTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_subtext_size_round : R.dimen.digital_subtext_size);
            dateTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_datetext_size_round : R.dimen.digital_datetext_size);

            mTextPaint.setTextSize(largeTextSize);
            mSubTextPaint.setTextSize(subTextSize);
            mDateTextPaint.setTextSize(dateTextSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                    mSubTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }


            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            mTime.setToNow();
            String text = String.format("%d:%02d", mTime.hour, mTime.minute);
            canvas.drawText(text, mXOffset, mYOffset, mTextPaint);
            text = String.format("%d %s, %s", mTime.monthDay, getMonthName(mTime.month), getWeekDayName(mTime.weekDay));
            canvas.drawText(text, mDateXOffset, mDateYOffset, mDateTextPaint);

            int d = mTime.weekDay;
            if(d > 0) {
                int h = mTime.hour;
                int m = mTime.minute;
                int total = h * 60 + m;
                int next;

                boolean longday = d == 1 || d == 3 || d == 4;
                if((next = getNextEventTimeDiff(total, longday ? SCHOOL_EVENTS_TIME_LONG : SCHOOL_EVENTS_TIME_SHORT)) > 0) {
                    text = String.format("Ещё %d м.", next);
                    mTextPaint.setTextSize(textSize);
                } else {
                    text = "";
                    mTextPaint.setTextSize(largeTextSize);
                }
            } else {
                text = "";
                mTextPaint.setTextSize(largeTextSize);
            }

            canvas.drawText(text, mSubXOffset, mSubYOffset, mSubTextPaint);
        }

        private int getNextEventTimeDiff(int now, int[] events){
            for(int time : events){
                if(time > now && time - now < 60)
                    return time - now;
            }
            return 0;
        }

        private String getMonthName(int month){
            switch (month){
                case 0: return "янв.";
                case 1: return "фев.";
                case 2: return "мар.";
                case 3: return "апр.";
                case 4: return "мая";
                case 5: return "июня";
                case 6: return "июля";
                case 7: return "авг.";
                case 8: return "сен.";
                case 9: return "окт.";
                case 10: return "ноя.";
                case 11: return "дек.";
            }

            return "of " + month;
        }

        private String getWeekDayName(int weekDay){
            switch (weekDay){
                case Time.MONDAY: return "понедельник";
                case Time.TUESDAY: return "вторник";
                case Time.WEDNESDAY: return "среда";
                case Time.THURSDAY: return "черверг";
                case Time.FRIDAY: return "пятница";
                case Time.SATURDAY: return "суббота";
                case Time.SUNDAY: return "воскресьние";
            }
            return String.valueOf(weekDay);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<WatchFace.Engine> mWeakReference;

        public EngineHandler(WatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            WatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
