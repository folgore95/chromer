/*
 * Chromer
 * Copyright (C) 2017 Arunkumar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package arun.com.chromer.webheads.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import arun.com.chromer.R;
import arun.com.chromer.util.Utils;
import timber.log.Timber;

/**
 * Created by Arun on 03/02/2016.
 */
@SuppressLint("ViewConstructor")
public class RemoveWebHead extends FrameLayout {

    static final double MAGNETISM_THRESHOLD = Utils.dpToPx(120);
    private static WindowManager sWindowManager;
    private static RemoveWebHead sOurInstance;

    private WindowManager.LayoutParams mWindowParams;

    private int mDispWidth;
    private int mDispHeight;

    private Spring mScaleSpring;
    private SpringSystem mSpringSystem;

    private boolean mHidden;

    private RemoveHeadCircle mRemoveHeadCircle;

    private boolean mGrew;

    private int[] mCentrePoint = null;

    @SuppressLint("RtlHardcoded")
    private RemoveWebHead(Context context, WindowManager windowManager) {
        super(context);
        sWindowManager = windowManager;

        mRemoveHeadCircle = new RemoveHeadCircle(context);
        addView(mRemoveHeadCircle);

        mWindowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        setDisplayMetrics();

        setVisibility(INVISIBLE);
        mHidden = true;

        mWindowParams.gravity = Gravity.LEFT | Gravity.TOP;
        int offset = getAdaptWidth() / 2;
        mWindowParams.x = (mDispWidth / 2) - offset;
        mWindowParams.y = mDispHeight - (mDispHeight / 6) - offset;

        setUpSprings();
        initCentreCoords();

        sWindowManager.addView(this, mWindowParams);
    }

    public static void init(@NonNull Context context) {
        get(context);
    }

    /**
     * Returns an instance of this view. If the view is not initialized, then a new view is created
     * and returned.
     * The returned view might not have been laid out yet.
     *
     * @param context
     * @return
     */
    public synchronized static RemoveWebHead get(@NonNull Context context) {
        if (sOurInstance != null)
            return sOurInstance;
        else {
            Timber.d("Creating new instance of remove web head");
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            sOurInstance = new RemoveWebHead(context, windowManager);
            return sOurInstance;
        }
    }

    public static void destroy() {
        if (sOurInstance != null) {
            sOurInstance.destroySelf();
        }
    }

    public static void disappear() {
        if (sOurInstance != null) {
            sOurInstance.hide();
        }
    }

    public void destroyAnimator(final Runnable endAction) {
        if (sOurInstance == null || mRemoveHeadCircle == null) endAction.run();

        sOurInstance.mRemoveHeadCircle.animate()
                .scaleX(0.0f)
                .scaleY(0.0f)
                .alpha(0.5f)
                .setDuration(300)
                .withLayer()
                .withEndAction(endAction)
                .setInterpolator(new BounceInterpolator())
                .start();
    }

    private void destroySelf() {
        mScaleSpring.setAtRest();
        mScaleSpring.destroy();
        mScaleSpring = null;

        removeView(mRemoveHeadCircle);
        mRemoveHeadCircle = null;

        mWindowParams = null;

        mSpringSystem = null;

        sWindowManager.removeView(this);

        mCentrePoint = null;

        sOurInstance = null;
        Timber.d("Remove view detached and killed");
    }

    private int getAdaptWidth() {
        return Math.max(getWidth(), RemoveHeadCircle.getSizePx());
    }

    int[] getCenterCoordinates() {
        if (mCentrePoint == null) {
            initCentreCoords();
        }
        return mCentrePoint;
    }

    private void initCentreCoords() {
        int offset = getAdaptWidth() / 2;
        int rX = getWindowParams().x + offset;
        int rY = getWindowParams().y + offset;
        mCentrePoint = new int[]{rX, rY};
    }

    private void setUpSprings() {
        mSpringSystem = SpringSystem.create();
        mScaleSpring = mSpringSystem.createSpring();

        SpringConfig scaleSpringConfig = SpringConfig.fromOrigamiTensionAndFriction(100, 9);
        mScaleSpring.setSpringConfig(scaleSpringConfig);
        mScaleSpring.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                float value = (float) spring.getCurrentValue();
                mRemoveHeadCircle.setScaleX(value);
                mRemoveHeadCircle.setScaleY(value);
            }
        });
    }

    private void setDisplayMetrics() {
        final DisplayMetrics metrics = new DisplayMetrics();
        sWindowManager.getDefaultDisplay().getMetrics(metrics);
        mDispWidth = metrics.widthPixels;
        mDispHeight = metrics.heightPixels;
    }

    private WindowManager.LayoutParams getWindowParams() {
        return mWindowParams;
    }

    private void hide() {
        if (!mHidden) {
            mScaleSpring.setEndValue(0.0f);
            mHidden = true;
        }
    }

    void reveal() {
        setVisibility(VISIBLE);
        if (mHidden) {
            mScaleSpring.setEndValue(0.9f);
            mHidden = false;
        }
    }

    void grow() {
        if (!mGrew) {
            mScaleSpring.setCurrentValue(0.9f, true);
            mScaleSpring.setEndValue(1f);
            mGrew = true;
        }
    }

    void shrink() {
        if (mGrew) {
            mScaleSpring.setEndValue(0.9f);
            mGrew = false;
        }
    }

    /**
     * Created by Arun on 04/02/2016.
     */
    private static class RemoveHeadCircle extends View {

        private static int sSizePx;
        private static int sDiameterPx;
        private final Paint mBgPaint;

        public RemoveHeadCircle(Context context) {
            super(context);
            mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBgPaint.setColor(ContextCompat.getColor(getContext(), R.color.remove_web_head_color));
            mBgPaint.setStyle(Paint.Style.FILL);

            float shadwR = context.getResources().getDimension(R.dimen.remove_head_shadow_radius);
            float shadwDx = context.getResources().getDimension(R.dimen.remove_head_shadow_dx);
            float shadwDy = context.getResources().getDimension(R.dimen.remove_head_shadow_dy);

            mBgPaint.setShadowLayer(shadwR, shadwDx, shadwDy, 0x75000000);

            setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            sSizePx = context.getResources().getDimensionPixelSize(R.dimen.remove_head_size);
        }

        static int getSizePx() {
            return sSizePx;
        }

        public static int getDiameterPx() {
            return sDiameterPx;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(sSizePx, sSizePx);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            float radius = (float) (getWidth() / 2.4);
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius, mBgPaint);

            drawDeleteIcon(canvas);

            sDiameterPx = (int) (2 * radius);
        }

        private void drawDeleteIcon(Canvas canvas) {
            Bitmap deleteIcon = new IconicsDrawable(getContext())
                    .icon(CommunityMaterial.Icon.cmd_delete)
                    .color(Color.WHITE)
                    .sizeDp(18).toBitmap();
            int cHeight = canvas.getClipBounds().height();
            int cWidth = canvas.getClipBounds().width();
            float x = cWidth / 2f - deleteIcon.getWidth() / 2;
            float y = cHeight / 2f - deleteIcon.getHeight() / 2;
            canvas.drawBitmap(deleteIcon, x, y, null);
        }
    }
}
