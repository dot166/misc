package io.github.dot166.nightstand;

import static io.github.dot166.nightstand.Utils.getAlphaAnimator;
import static io.github.dot166.nightstand.Utils.getScaleAnimator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.TextView;

/**
 * This runnable chooses a random initial position for {@link #mSaverView} within
 * {@link #mContentView} if {@link #mSaverView} is transparent. It also schedules itself to run
 * each minute, at which time {@link #mSaverView} is faded out, set to a new random location, and
 * faded in.
 */
public final class MoveScreensaverRunnable implements Runnable {

    enum Scene {
        CLOCK,
        EVENT
    }

    private Scene mCurrentScene = Scene.CLOCK;

    private final View mClockView;

    private final View mEventView;

    /** The duration over which the fade in/out animations occur. */
    private static final long FADE_TIME = 3000L;

    /** The duration (in ms) to delay the start of the fade in animation to allow Do Not Disturb
     * mode to activate.
     */
    private static final int START_DELAY = 5;

    /** Accelerate the hide animation. */
    private final Interpolator mAcceleration = new AccelerateInterpolator();

    /** Decelerate the show animation. */
    private final Interpolator mDeceleration = new DecelerateInterpolator();

    /** The container that houses {@link #mSaverView}. */
    private final View mContentView;

    /** The display within the {@link #mContentView} that is randomly positioned. */
    private View mSaverView;

    /** Tracks the currently executing animation if any; used to gracefully stop the animation. */
    private Animator mActiveAnimator;

    private final CalendarModel mCalendarModel;

    /**
     * @param contentView contains the {@code saverView}
     */
    public MoveScreensaverRunnable(View contentView) {
        mContentView = contentView;
        mClockView = mContentView.findViewById(R.id.main_clock);
        mEventView = mContentView.findViewById(R.id.calendar_holder);
        mSaverView = mClockView;
        mCalendarModel = new CalendarModel(mContentView.getContext());
    }

    /**
     * Start or restart the random movement of the saver view within the content view.
     */
    public void start() {
        // Stop any existing animations or callbacks.
        stop();

        // Reset the alpha to 0 so saver view will be randomly positioned within the new bounds.
        mSaverView.setAlpha(0);

        mCalendarModel.startPolling();

        CalendarModel.Event event = mCalendarModel.getEvent();
        if (mCalendarModel.hasUpcomingEvent() && event != null) {
            ((TextView)mEventView.findViewById(R.id.event)).setText(event.toString());
        }

        // Execute the position updater runnable to choose the first random position of saver view.
        run();
        mEventView.setVisibility(View.GONE);

        // Schedule callbacks every minute to adjust the position of mSaverView.
        Utils.addMinuteCallback(this, -FADE_TIME);
    }

    /**
     * Stop the random movement of the saver view within the content view.
     */
    public void stop() {
        Utils.removePeriodicCallback(this);

        // End any animation currently running.
        if (mActiveAnimator != null) {
            mActiveAnimator.end();
            mActiveAnimator = null;
        }
        mCalendarModel.stopPolling();
    }

    @Override
    public void run() {
        Utils.enforceMainLooper();

        Scene nextScene = mCalendarModel.hasUpcomingEvent() && mCurrentScene == Scene.CLOCK && !Utils.getScreensaverNightModeOn()
                ? Scene.EVENT
                : Scene.CLOCK;

        if (Utils.getScreensaverNightModeOn()) {
            nextScene = Scene.CLOCK;
        }

        if (nextScene != mCurrentScene) {
            switchScene(nextScene);
        }

        final boolean selectInitialPosition = mSaverView.getAlpha() == 0f;
        if (selectInitialPosition) {
            // When selecting an initial position for the saver view the width and height of
            // mContentView are untrustworthy if this was caused by a configuration change. To
            // combat this, we position the mSaverView randomly within the smallest box that is
            // guaranteed to work.
            final int smallestDim = Math.min(mContentView.getWidth(), mContentView.getHeight());
            final float newX = getRandomPoint(smallestDim - mSaverView.getWidth());
            final float newY = getRandomPoint(smallestDim - mSaverView.getHeight());

            mSaverView.setX(newX);
            mSaverView.setY(newY);
            mActiveAnimator = getAlphaAnimator(mSaverView, 0f, 1f);
            mActiveAnimator.setDuration(FADE_TIME);
            mActiveAnimator.setInterpolator(mDeceleration);
            // Add a slight delay to allow DND mode to engage for the call to Utils.dimClockView().
            mActiveAnimator.setStartDelay(START_DELAY);
            mActiveAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    // Re-dim the display in case DnD has been enabled and we're following it.
                    Utils.dimClockView(Utils.getScreensaverNightModeOn(), mSaverView);
                }
            });
        } else {
            // Select a new random position anywhere in mContentView that will fit mSaverView.
            final float newX = getRandomPoint(mContentView.getWidth() - mSaverView.getWidth());
            final float newY = getRandomPoint(mContentView.getHeight() - mSaverView.getHeight());

            // Fade out and shrink the saver view.
            final AnimatorSet hide = new AnimatorSet();
            hide.setDuration(FADE_TIME);
            hide.setInterpolator(mAcceleration);
            hide.play(getAlphaAnimator(mSaverView, 1f, 0f))
                    .with(getScaleAnimator(mSaverView, 1f, 0.85f));

            // Fade in and grow the saver view after altering its position.
            final AnimatorSet show = new AnimatorSet();
            show.setDuration(FADE_TIME);
            // Add a slight delay to allow DND mode to engage for the call to Utils.dimClockView().
            show.setStartDelay(START_DELAY);
            show.setInterpolator(mDeceleration);
            show.play(getAlphaAnimator(mSaverView, 0f, 1f))
                    .with(getScaleAnimator(mSaverView, 0.85f, 1f));
            show.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mSaverView.setX(newX);
                    mSaverView.setY(newY);
                    // Re-dim the display in case DnD has been enabled and we're following it.
                    Utils.dimClockView(Utils.getScreensaverNightModeOn(), mSaverView);
                }
            });

            // Execute hide followed by show.
            final AnimatorSet all = new AnimatorSet();
            all.play(show).after(hide);
            mActiveAnimator = all;
        }
        mActiveAnimator.start();
    }

    private void switchScene(Scene next) {
        mClockView.setVisibility(next == Scene.CLOCK ? View.VISIBLE : View.GONE);
        mEventView.setVisibility(next == Scene.EVENT ? View.VISIBLE : View.GONE);

        CalendarModel.Event event = mCalendarModel.getEvent();
        if (mCalendarModel.hasUpcomingEvent() && next == Scene.EVENT && event != null) {
            ((TextView)mEventView.findViewById(R.id.event)).setText(event.toString());
        }

        mSaverView = (next == Scene.CLOCK) ? mClockView : mEventView;
        mCurrentScene = next;
    }

    /**
     * @return a random integer between 0 and the {@code maximum} exclusive.
     */
    private static float getRandomPoint(float maximum) {
        return (int) (Math.random() * maximum);
    }
}
