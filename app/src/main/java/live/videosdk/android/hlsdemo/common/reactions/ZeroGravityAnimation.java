package live.videosdk.android.hlsdemo.common.reactions;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;

import java.util.Random;

public class ZeroGravityAnimation {

    private static final int RANDOM_DURATION = -1;

    private DirectionGenerator.Direction mOriginationDirection = DirectionGenerator.Direction.RANDOM;
    private DirectionGenerator.Direction mDestinationDirection = DirectionGenerator.Direction.RANDOM;
    private int mDuration = RANDOM_DURATION;
    private int mCount = 1;
    private Drawable drawable;
    private float mScalingFactor = 1f;
    private Animation.AnimationListener mAnimationListener;


    /**
     * Sets the original direction. The animation will originate from the given direction.
     */
    public ZeroGravityAnimation setOriginationDirection(DirectionGenerator.Direction direction) {
        this.mOriginationDirection = direction;
        return this;
    }

    /**
     * Sets the animation destination direction. The translate animation will proceed towards the given direction.
     */
    public ZeroGravityAnimation setDestinationDirection(DirectionGenerator.Direction direction) {
        this.mDestinationDirection = direction;
        return this;
    }

    /**
     * Sets the time duration in millseconds for animation to proceed.
     */
    public ZeroGravityAnimation setDuration(int duration) {
        this.mDuration = duration;
        return this;
    }

    /**
     * Sets the image reference for drawing the image
     */

    public ZeroGravityAnimation setImage(Drawable drawable) {
        this.drawable = drawable;
        return this;
    }

    /**
     * Sets the image scaling value.
     */
    public ZeroGravityAnimation setScalingFactor(float scale) {
        this.mScalingFactor = scale;
        return this;
    }

    public ZeroGravityAnimation setAnimationListener(Animation.AnimationListener listener) {
        this.mAnimationListener = listener;
        return this;
    }

    public ZeroGravityAnimation setCount(int count) {
        this.mCount = count;
        return this;
    }


    /**
     * Starts the Zero gravity animation by creating an OTT and attach it to th given ViewGroup
     */
    public void play(Activity activity, ViewGroup ottParent) {

        DirectionGenerator generator = new DirectionGenerator();

        if (mCount > 0) {

            for (int i = 0; i < mCount; i++) {


                final int iDupe = i;

                DirectionGenerator.Direction origin = mOriginationDirection == DirectionGenerator.Direction.RANDOM ? generator.getRandomDirection() : mOriginationDirection;
                DirectionGenerator.Direction destination = mDestinationDirection == DirectionGenerator.Direction.RANDOM ? generator.getRandomDirection(origin) : mDestinationDirection;

                int startingPoints[] = generator.getPointsInDirection(activity, origin);
                int endPoints[] = generator.getPointsInDirection(activity, destination);

                Bitmap bitmap = drawableToBitmap(this.drawable);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * mScalingFactor), (int) (bitmap.getHeight() * mScalingFactor), false);

                switch (origin) {
                    case LEFT:
                        startingPoints[0] -= scaledBitmap.getWidth();
                        break;

                    case RIGHT:
                        startingPoints[0] += scaledBitmap.getWidth();
                        break;

                    case TOP:
                        startingPoints[1] -= scaledBitmap.getHeight();
                        break;
                    case BOTTOM:
                        startingPoints[1] += scaledBitmap.getHeight();
                        break;
                }

                switch (destination) {
                    case LEFT:
                        endPoints[0] -= scaledBitmap.getWidth();
                        break;

                    case RIGHT:
                        endPoints[0] += scaledBitmap.getWidth();
                        break;

                    case TOP:
                        endPoints[1] -= scaledBitmap.getHeight();
                        break;
                    case BOTTOM:
                        endPoints[1] += scaledBitmap.getHeight();
                        break;
                }


                final OverTheTopLayer layer = new OverTheTopLayer();

                FrameLayout ottLayout = layer.with(activity)
                        .scale(mScalingFactor)
                        .attachTo(ottParent)
                        .setBitmap(scaledBitmap, startingPoints)
                        .create();


                switch (origin) {
                    case LEFT:

                }

                int deltaX = endPoints[0] - startingPoints[0];
                int deltaY = endPoints[1] - startingPoints[1];

                int duration = mDuration;
                if (duration == RANDOM_DURATION) {
                    duration = generateRandomBetween(1, 10);
                }

                TranslateAnimation animation = new TranslateAnimation(0, deltaX, 0, deltaY);
                animation.setDuration(duration);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                        if (iDupe == 0) {
                            if (mAnimationListener != null) {
                                mAnimationListener.onAnimationStart(animation);
                            }
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                        layer.destroy();

                        if (iDupe == (mCount - 1)) {
                            if (mAnimationListener != null) {
                                mAnimationListener.onAnimationEnd(animation);
                            }
                        }

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                layer.applyAnimation(animation);
            }
        } else {

            Log.e(ZeroGravityAnimation.class.getSimpleName(), "Count was not provided, animation was not started");
        }
    }

    /**
     * Takes the content view as view parent for laying the animation objects and starts the animation.
     *
     * @param activity - activity on which the zero gravity animation should take place.
     */
    public void play(Activity activity) {

        play(activity, null);

    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     * Generates the random between two given integers.
     */

    public static int generateRandomBetween(int start, int end) {

        Random random = new Random();
        int rand = random.nextInt(Integer.MAX_VALUE - 1) % end;

        if (rand < start) {
            rand = start;
        }
        return rand;
    }
}