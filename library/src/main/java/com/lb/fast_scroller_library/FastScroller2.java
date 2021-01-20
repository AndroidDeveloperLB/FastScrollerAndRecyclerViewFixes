/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * NOTE: This is almost completely the same as FastScroller
 * in the androidx.recycler.widget package. However, this one can
 * account for the bottom padding, where the items are visible (clipToPadding="false"),
 * but the fast scroller should not be inside the padded area (e. g. items visible
 * behind a navigation bar, but the scroller should not go under the navigation bar).
 */

package com.lb.fast_scroller_library;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.MotionEvent;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class responsible to animate and provide a fast scroller.
 * based on: https://stackoverflow.com/q/47846873/878126 https://stackoverflow.com/a/58652264/878126
 */
@SuppressWarnings("unused")
public class FastScroller2 extends RecyclerView.ItemDecoration implements RecyclerView.OnItemTouchListener {
    @IntDef({STATE_HIDDEN, STATE_VISIBLE, STATE_DRAGGING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
    }

    // Scroll thumb not showing
    private static final int STATE_HIDDEN = 0;
    // Scroll thumb visible and moving along with the scrollbar
    private static final int STATE_VISIBLE = 1;
    // Scroll thumb being dragged by user
    private static final int STATE_DRAGGING = 2;

    @IntDef({DRAG_X, DRAG_Y, DRAG_NONE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface DragState {
    }

    private static final int DRAG_NONE = 0;
    private static final int DRAG_X = 1;
    private static final int DRAG_Y = 2;

    @IntDef({ANIMATION_STATE_OUT, ANIMATION_STATE_FADING_IN, ANIMATION_STATE_IN,
            ANIMATION_STATE_FADING_OUT})
    @Retention(RetentionPolicy.SOURCE)
    private @interface AnimationState {
    }

    private static final int ANIMATION_STATE_OUT = 0;
    private static final int ANIMATION_STATE_FADING_IN = 1;
    private static final int ANIMATION_STATE_IN = 2;
    private static final int ANIMATION_STATE_FADING_OUT = 3;

    private static final int SHOW_DURATION_MS = 500;
    private static final int HIDE_DELAY_AFTER_VISIBLE_MS = 1500;
    private static final int HIDE_DELAY_AFTER_DRAGGING_MS = 1200;
    private static final int HIDE_DURATION_MS = 500;
    private static final int SCROLLBAR_FULL_OPAQUE = 255;

    private static final int[] PRESSED_STATE_SET = new int[]{android.R.attr.state_pressed};
    private static final int[] EMPTY_STATE_SET = new int[]{};

    private final int mScrollbarMinimumRange;
    private final int mMargin;

    // Final values for the vertical scroll bar
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final StateListDrawable mVerticalThumbDrawable;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Drawable mVerticalTrackDrawable;
    private final int mVerticalThumbWidth;
    private final int mVerticalTrackWidth;
    private final boolean mConsiderPadding;

    // Final values for the horizontal scroll bar
    private final StateListDrawable mHorizontalThumbDrawable;
    private final Drawable mHorizontalTrackDrawable;
    private final int mHorizontalThumbHeight;
    private final int mHorizontalTrackHeight;
    //    private final int defaultWidth;
    private final int minThumbSize;

    // Dynamic values for the vertical scroll bar
    @VisibleForTesting
    private
    int mVerticalThumbHeight;
    @VisibleForTesting
    private
    int mVerticalThumbCenterY;
    @VisibleForTesting
    private
    float mVerticalDragY;

    // Dynamic values for the horizontal scroll bar
    @VisibleForTesting
    private
    int mHorizontalThumbWidth;
    @VisibleForTesting
    private
    int mHorizontalThumbCenterX;
    @VisibleForTesting
    private
    float mHorizontalDragX;

    private int recyclerViewWidth = 0;
    private int recyclerViewHeight = 0;

    private RecyclerView recyclerView;
    /**
     * Whether the document is long/wide enough to require scrolling. If not, we don't show the
     * relevant scroller.
     */
    private boolean mNeedVerticalScrollbar = false;
    private boolean mNeedHorizontalScrollbar = false;
    @State
    private int state = STATE_HIDDEN;
    @DragState
    private int dragState = DRAG_NONE;

    private final int[] mVerticalRange = new int[2];
    private final int[] mHorizontalRange = new int[2];
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ValueAnimator mShowHideAnimator = ValueAnimator.ofFloat(0, 1);
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @AnimationState
    int mAnimationState = ANIMATION_STATE_OUT;
    private final Runnable mHideRunnable = () -> hide(HIDE_DURATION_MS);
    private final RecyclerView.OnScrollListener
            mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            updateScrollPosition(recyclerView.computeHorizontalScrollOffset(),
                    recyclerView.computeVerticalScrollOffset());
        }
    };

    public FastScroller2(RecyclerView recyclerView, StateListDrawable verticalThumbDrawable,
                         Drawable verticalTrackDrawable, StateListDrawable horizontalThumbDrawable,
                         Drawable horizontalTrackDrawable, int defaultWidth, int scrollbarMinimumRange,
                         int margin, boolean considerPadding, int minThumbSize) {
        mVerticalThumbDrawable = verticalThumbDrawable;
        mVerticalTrackDrawable = verticalTrackDrawable;
        mHorizontalThumbDrawable = horizontalThumbDrawable;
        mHorizontalTrackDrawable = horizontalTrackDrawable;
        this.minThumbSize = minThumbSize;
        mVerticalThumbWidth = Math.max(defaultWidth, verticalThumbDrawable.getIntrinsicWidth());
        mVerticalTrackWidth = Math.max(defaultWidth, verticalTrackDrawable.getIntrinsicWidth());
        mHorizontalThumbHeight = Math
                .max(defaultWidth, horizontalThumbDrawable.getIntrinsicWidth());
        mHorizontalTrackHeight = Math
                .max(defaultWidth, horizontalTrackDrawable.getIntrinsicWidth());
        mScrollbarMinimumRange = scrollbarMinimumRange;
        mMargin = margin;
        mVerticalThumbDrawable.setAlpha(SCROLLBAR_FULL_OPAQUE);
        mVerticalTrackDrawable.setAlpha(SCROLLBAR_FULL_OPAQUE);
        mConsiderPadding = considerPadding;

        mShowHideAnimator.addListener(new AnimatorListener());
        mShowHideAnimator.addUpdateListener(new AnimatorUpdater());

        attachToRecyclerView(recyclerView);
    }

    private void attachToRecyclerView(@Nullable RecyclerView recyclerView) {
        if (this.recyclerView == recyclerView) {
            return; // nothing to do
        }
        if (this.recyclerView != null) {
            destroyCallbacks();
        }
        this.recyclerView = recyclerView;
        if (this.recyclerView != null) {
            setupCallbacks();
        }
    }

    private void setupCallbacks() {
        recyclerView.addItemDecoration(this);
        recyclerView.addOnItemTouchListener(this);
        recyclerView.addOnScrollListener(mOnScrollListener);
    }

    private void destroyCallbacks() {
        recyclerView.removeItemDecoration(this);
        recyclerView.removeOnItemTouchListener(this);
        recyclerView.removeOnScrollListener(mOnScrollListener);
        cancelHide();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void requestRedraw() {
        recyclerView.invalidate();
    }

    private void setState(@State int state) {
        if (state == STATE_DRAGGING && this.state != STATE_DRAGGING) {
            mVerticalThumbDrawable.setState(PRESSED_STATE_SET);
            cancelHide();
        }

        if (state == STATE_HIDDEN) {
            requestRedraw();
        } else {
            show();
        }

        if (this.state == STATE_DRAGGING && state != STATE_DRAGGING) {
            mVerticalThumbDrawable.setState(EMPTY_STATE_SET);
            resetHideDelay(HIDE_DELAY_AFTER_DRAGGING_MS);
        } else if (state == STATE_VISIBLE) {
            resetHideDelay(HIDE_DELAY_AFTER_VISIBLE_MS);
        }
        this.state = state;
    }

    private boolean isLayoutRTL() {
        return ViewCompat.getLayoutDirection(recyclerView) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    public boolean isDragging() {
        return state == STATE_DRAGGING;
    }

    @VisibleForTesting
    boolean isVisible() {
        return state == STATE_VISIBLE;
    }

    @SuppressLint("SwitchIntDef")
    private void show() {
        switch (mAnimationState) {
            case ANIMATION_STATE_FADING_OUT:
                mShowHideAnimator.cancel();
                // fall through
            case ANIMATION_STATE_OUT:
                mAnimationState = ANIMATION_STATE_FADING_IN;
                mShowHideAnimator.setFloatValues((float) mShowHideAnimator.getAnimatedValue(), 1);
                mShowHideAnimator.setDuration(SHOW_DURATION_MS);
                mShowHideAnimator.setStartDelay(0);
                mShowHideAnimator.start();
                break;
        }
    }

    @SuppressLint("SwitchIntDef")
    @VisibleForTesting
    private void hide(@SuppressWarnings("SameParameterValue") int duration) {
        switch (mAnimationState) {
            case ANIMATION_STATE_FADING_IN:
                mShowHideAnimator.cancel();
                // fall through
            case ANIMATION_STATE_IN:
                mAnimationState = ANIMATION_STATE_FADING_OUT;
                mShowHideAnimator.setFloatValues((float) mShowHideAnimator.getAnimatedValue(), 0);
                mShowHideAnimator.setDuration(duration);
                mShowHideAnimator.start();
                break;
        }
    }

    private void cancelHide() {
        recyclerView.removeCallbacks(mHideRunnable);
    }

    private void resetHideDelay(int delay) {
        cancelHide();
        recyclerView.postDelayed(mHideRunnable, delay);
    }

    @Override
    public void onDrawOver(@NotNull Canvas canvas, @NotNull RecyclerView parent, @NotNull RecyclerView.State state) {
        if (recyclerViewWidth != recyclerView.getWidth()
                || recyclerViewHeight != recyclerView.getHeight()) {
            recyclerViewWidth = recyclerView.getWidth();
            recyclerViewHeight = recyclerView.getHeight();
            // This is due to the different events ordering when keyboard is opened or
            // retracted vs rotate. Hence to avoid corner cases we just disable the
            // scroller when size changed, and wait until the scroll position is recomputed
            // before showing it back.
            setState(STATE_HIDDEN);
            return;
        }

        if (mAnimationState != ANIMATION_STATE_OUT) {
            if (mNeedVerticalScrollbar) {
                drawVerticalScrollbar(canvas);
            }
            if (mNeedHorizontalScrollbar) {
                drawHorizontalScrollbar(canvas);
            }
        }
    }

    private void drawVerticalScrollbar(Canvas canvas) {
        int viewWidth = recyclerViewWidth;

        int left = viewWidth - mVerticalThumbWidth;
        // Counter intuitive, but left and right are switched here
        if (mConsiderPadding) left -= recyclerView.getPaddingRight();
        int top = mVerticalThumbCenterY - mVerticalThumbHeight / 2;
        mVerticalThumbDrawable.setBounds(0, 0, mVerticalThumbWidth, mVerticalThumbHeight);
        int trackTop = 0;
        int trackBottom = recyclerViewHeight;
        if (mConsiderPadding) trackBottom -= recyclerView.getPaddingBottom();
        mVerticalTrackDrawable
                .setBounds(0, trackTop, mVerticalTrackWidth, trackBottom);

        if (isLayoutRTL()) {
            mVerticalTrackDrawable.draw(canvas);
            canvas.translate(mVerticalThumbWidth, top);
            canvas.scale(-1, 1);
            mVerticalThumbDrawable.draw(canvas);
            canvas.scale(1, 1);
            canvas.translate(-mVerticalThumbWidth, -top);
        } else {
            canvas.translate(left, 0);
            mVerticalTrackDrawable.draw(canvas);
            canvas.translate(0, top);
            mVerticalThumbDrawable.draw(canvas);
            canvas.translate(-left, -top);
        }
    }

    private void drawHorizontalScrollbar(Canvas canvas) {
        int viewHeight = recyclerViewHeight;

        int top = viewHeight - mHorizontalThumbHeight;
        int left = mHorizontalThumbCenterX - mHorizontalThumbWidth / 2;
        mHorizontalThumbDrawable.setBounds(0, 0, mHorizontalThumbWidth, mHorizontalThumbHeight);
        mHorizontalTrackDrawable
                .setBounds(0, 0, recyclerViewWidth, mHorizontalTrackHeight);

        canvas.translate(0, top);
        mHorizontalTrackDrawable.draw(canvas);
        canvas.translate(left, 0);
        mHorizontalThumbDrawable.draw(canvas);
        canvas.translate(-left, -top);
    }

    /**
     * Notify the scroller of external change of the scroll, e.g. through dragging or flinging on
     * the view itself.
     *
     * @param offsetX The new scroll X offset.
     * @param offsetY The new scroll Y offset.
     */
    private void updateScrollPosition(int offsetX, int offsetY) {
        int verticalContentLength = recyclerView.computeVerticalScrollRange();
        int verticalVisibleLength = recyclerViewHeight;
        // This is important, because the thumb is drawn inside the vertical visible length!
        if (mConsiderPadding) verticalVisibleLength -= recyclerView.getPaddingBottom();
        mNeedVerticalScrollbar = verticalContentLength - verticalVisibleLength > 0
                && recyclerViewHeight >= mScrollbarMinimumRange;

        int horizontalContentLength = recyclerView.computeHorizontalScrollRange();
        int horizontalVisibleLength = recyclerViewWidth;
        mNeedHorizontalScrollbar = horizontalContentLength - horizontalVisibleLength > 0
                && recyclerViewWidth >= mScrollbarMinimumRange;

        if (!mNeedVerticalScrollbar && !mNeedHorizontalScrollbar) {
            if (state != STATE_HIDDEN) {
                setState(STATE_HIDDEN);
            }
            return;
        }

        if (mNeedVerticalScrollbar) {
            final int baseMinVerticalThumbHeight = Math.min(verticalVisibleLength,
                    (verticalVisibleLength * verticalVisibleLength) / verticalContentLength);
            if (minThumbSize <= 0) {
                //original logic
                float middleScreenPos = offsetY + verticalVisibleLength / 2.0f;
                mVerticalThumbCenterY =
                        (int) ((verticalVisibleLength * middleScreenPos) / verticalContentLength);
                mVerticalThumbHeight = baseMinVerticalThumbHeight;
            } else {
                mVerticalThumbCenterY =
                        (int) ((verticalVisibleLength - mVerticalThumbHeight) / ((float) verticalContentLength - verticalVisibleLength) * offsetY + mVerticalThumbHeight / 2.0);
                mVerticalThumbHeight = Math.max(minThumbSize, baseMinVerticalThumbHeight);
            }
        }
        if (mNeedHorizontalScrollbar) {
            final int baseMinHorizontalThumbWidth = Math.min(horizontalVisibleLength,
                    (horizontalVisibleLength * horizontalVisibleLength) / horizontalContentLength);
            if (minThumbSize <= 0) {
                //original logic
                float middleScreenPos = offsetX + horizontalVisibleLength / 2.0f;
                mHorizontalThumbCenterX =
                        (int) ((horizontalVisibleLength * middleScreenPos) / horizontalContentLength);
                mHorizontalThumbWidth = baseMinHorizontalThumbWidth;
            } else {
                mHorizontalThumbCenterX = (int) ((horizontalVisibleLength - mHorizontalThumbCenterX) / ((float) horizontalContentLength - horizontalVisibleLength) * offsetX + mHorizontalThumbHeight / 2.0);
                mHorizontalThumbWidth = Math.max(minThumbSize, baseMinHorizontalThumbWidth);
            }
        }

        if (state == STATE_HIDDEN || state == STATE_VISIBLE) {
            setState(STATE_VISIBLE);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView,
                                         @NonNull MotionEvent ev) {
        final boolean handled;
        if (state == STATE_VISIBLE) {
            boolean insideVerticalThumb = isPointInsideVerticalThumb(ev.getX(), ev.getY());
            boolean insideHorizontalThumb = isPointInsideHorizontalThumb(ev.getX(), ev.getY());
            if (ev.getAction() == MotionEvent.ACTION_DOWN
                    && (insideVerticalThumb || insideHorizontalThumb)) {
                if (insideHorizontalThumb) {
                    dragState = DRAG_X;
                    mHorizontalDragX = (int) ev.getX();
                } else {//if (insideVerticalThumb) {
                    dragState = DRAG_Y;
                    mVerticalDragY = (int) ev.getY();
                }

                setState(STATE_DRAGGING);
                handled = true;
            } else {
                handled = false;
            }
        } else handled = state == STATE_DRAGGING;
        return handled;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent me) {
        if (state == STATE_HIDDEN) {
            return;
        }

        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            boolean insideVerticalThumb = isPointInsideVerticalThumb(me.getX(), me.getY());
            boolean insideHorizontalThumb = isPointInsideHorizontalThumb(me.getX(), me.getY());
            if (insideVerticalThumb || insideHorizontalThumb) {
                if (insideHorizontalThumb) {
                    dragState = DRAG_X;
                    mHorizontalDragX = (int) me.getX();
                } else {//if (insideVerticalThumb) {
                    dragState = DRAG_Y;
                    mVerticalDragY = (int) me.getY();
                }
                setState(STATE_DRAGGING);
            }
        } else if (me.getAction() == MotionEvent.ACTION_UP && state == STATE_DRAGGING) {
            mVerticalDragY = 0;
            mHorizontalDragX = 0;
            setState(STATE_VISIBLE);
            dragState = DRAG_NONE;
        } else if (me.getAction() == MotionEvent.ACTION_MOVE && state == STATE_DRAGGING) {
            show();
            if (dragState == DRAG_X) {
                horizontalScrollTo(me.getX());
            }
            if (dragState == DRAG_Y) {
                verticalScrollTo(me.getY());
            }
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    private void verticalScrollTo(float y) {
        final int[] scrollbarRange = getVerticalRange();
        // We want to allow the user to scroll over the bounds.
        // y = Math.max(scrollbarRange[0], Math.min(scrollbarRange[1], y));
        if (Math.abs(mVerticalThumbCenterY - y) < 2) {
            return;
        }
        int height = recyclerViewHeight;
        if (mConsiderPadding) height -= recyclerView.getPaddingBottom();
        int scrollingBy = scrollTo(mVerticalDragY, y, scrollbarRange,
                recyclerView.computeVerticalScrollRange(),
                recyclerView.computeVerticalScrollOffset(), height);
        if (scrollingBy != 0) {
            recyclerView.scrollBy(0, scrollingBy);
        }
        mVerticalDragY = y;
    }

    private void horizontalScrollTo(float x) {
        final int[] scrollbarRange = getHorizontalRange();
        x = Math.max(scrollbarRange[0], Math.min(scrollbarRange[1], x));
        if (Math.abs(mHorizontalThumbCenterX - x) < 2) {
            return;
        }

        int scrollingBy = scrollTo(mHorizontalDragX, x, scrollbarRange,
                recyclerView.computeHorizontalScrollRange(),
                recyclerView.computeHorizontalScrollOffset(), recyclerViewWidth);
        if (scrollingBy != 0) {
            recyclerView.scrollBy(scrollingBy, 0);
        }

        mHorizontalDragX = x;
    }

    private int scrollTo(float oldDragPos, float newDragPos, int[] scrollbarRange, int scrollRange,
                         int scrollOffset, int viewLength) {
        int scrollbarLength = scrollbarRange[1] - scrollbarRange[0];
        if (scrollbarLength == 0) {
            return 0;
        }
        float percentage = ((newDragPos - oldDragPos) / (float) scrollbarLength);
        int totalPossibleOffset = scrollRange - viewLength;
        int scrollingBy = (int) (percentage * totalPossibleOffset);
        int absoluteOffset = scrollOffset + scrollingBy;
        if (absoluteOffset < totalPossibleOffset && absoluteOffset >= 0) {
            return scrollingBy;
        } else {
            return 0;
        }
    }

    @VisibleForTesting
    private boolean isPointInsideVerticalThumb(float x, float y) {
        return (isLayoutRTL() ? x <= mVerticalThumbWidth / 2f
                : x >= recyclerViewWidth - mVerticalThumbWidth)
                && y >= mVerticalThumbCenterY - mVerticalThumbHeight / 2f
                && y <= mVerticalThumbCenterY + mVerticalThumbHeight / 2f;
    }

    @VisibleForTesting
    private boolean isPointInsideHorizontalThumb(float x, float y) {
        return (y >= recyclerViewHeight - mHorizontalThumbHeight)
                && x >= mHorizontalThumbCenterX - mHorizontalThumbWidth / 2f
                && x <= mHorizontalThumbCenterX + mHorizontalThumbWidth / 2f;
    }

    @VisibleForTesting
    Drawable getHorizontalTrackDrawable() {
        return mHorizontalTrackDrawable;
    }

    @VisibleForTesting
    Drawable getHorizontalThumbDrawable() {
        return mHorizontalThumbDrawable;
    }

    @VisibleForTesting
    Drawable getVerticalTrackDrawable() {
        return mVerticalTrackDrawable;
    }

    @VisibleForTesting
    Drawable getVerticalThumbDrawable() {
        return mVerticalThumbDrawable;
    }

    /**
     * Gets the (min, max) vertical positions of the vertical scroll bar.
     */
    private int[] getVerticalRange() {
        mVerticalRange[0] = mMargin;
        mVerticalRange[1] = recyclerViewHeight - mMargin;
        if (mConsiderPadding) mVerticalRange[1] -= recyclerView.getPaddingBottom();
        return mVerticalRange;
    }

    /**
     * Gets the (min, max) horizontal positions of the horizontal scroll bar.
     */
    private int[] getHorizontalRange() {
        mHorizontalRange[0] = mMargin;
        mHorizontalRange[1] = recyclerViewWidth - mMargin;
        return mHorizontalRange;
    }

    private class AnimatorListener extends AnimatorListenerAdapter {

        private boolean mCanceled = false;

        AnimatorListener() {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            // Cancel is always followed by a new directive, so don't update state.
            if (mCanceled) {
                mCanceled = false;
                return;
            }
            if ((float) mShowHideAnimator.getAnimatedValue() == 0) {
                mAnimationState = ANIMATION_STATE_OUT;
                setState(STATE_HIDDEN);
            } else {
                mAnimationState = ANIMATION_STATE_IN;
                requestRedraw();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mCanceled = true;
        }
    }

    private class AnimatorUpdater implements AnimatorUpdateListener {
        AnimatorUpdater() {
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            int alpha = (int) (SCROLLBAR_FULL_OPAQUE * ((float) valueAnimator.getAnimatedValue()));
            mVerticalThumbDrawable.setAlpha(alpha);
            mVerticalTrackDrawable.setAlpha(alpha);
            requestRedraw();
        }
    }
}
