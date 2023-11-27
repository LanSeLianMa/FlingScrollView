package com.example.flingscrollview.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.flingscrollview.common.FlingTask;

public class LinerScrollView extends LinearLayout {

    final Handler mHandler;
    private final int mTouchSlop; // 移动的距离大于这个像素值的时候，会认为是在滑动
    private final int mMinimumVelocity; // 最小的速度
    private final int mMaximumVelocity; // 最大的速度
    private VelocityTracker mVelocityTracker; // 速度跟踪器
    private int mScrollPointerId; // 当前最新放在屏幕伤的手指
    private int mLastTouchX; // 上一次触摸的X坐标
    private int mLastTouchY; // 上一次触摸的Y坐标
    private int mInitialTouchX; // 初始化触摸的X坐标
    private int mInitialTouchY; // 初始化触摸的Y坐标
    public final int SCROLL_STATE_IDLE = -1; // 没有滚动
    public final int SCROLL_STATE_DRAGGING = 1; // 被手指拖动情况下滚动
    public final int SCROLL_STATE_SETTLING = 2; // 没有被手指拖动情况下，惯性滚动
    private int mScrollState = SCROLL_STATE_IDLE; // 滚动状态

    // 在测试过程中，通过速度正负值判断方向，方向有概率不准确
    // 所以我在onTouchEvent里自己处理
    private boolean direction = true; // true：向上 false：向下
    private FlingTask flingTask; // 惯性任务

    public LinerScrollView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mHandler = new Handler(Looper.getMainLooper());

        // 一些系统的预定义值:
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        initView();
    }

    /**
     * 初始化视图
     */
    private void initView() {
        for (int i = 0; i < 50; i++) {
            TextView textView = new TextView(getContext());
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 350);
            textView.setLayoutParams(params);
            textView.setText("index：" + i);
            textView.setTextColor(Color.BLACK);
            textView.setTextSize(30);
            textView.setBackgroundColor(Color.CYAN);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            addView(textView);
        }
    }

    boolean notUp = false; // 是否 不能再向上滑了
    boolean notDown = false; // 是否 不能再向下滑了
    int listMaxOffsetY = 0; // 列表最大滑动Y值

    /**
     * 滚动列表
     * @param offsetY 偏移Y值
     */
    private void translationViewY(int offsetY) {
        if (listMaxOffsetY == 0) {
            listMaxOffsetY = (350 * 50) - getHeight();
        }

        if (mScrollState == SCROLL_STATE_DRAGGING) {

            if (direction) { // 向上滑动
                if (Math.abs(getChildAt((getChildCount() - 1)).getTranslationY()) < listMaxOffsetY) {
                    notUp = false;
                }
            } else { // 向下滑动
                if (getChildAt(0).getTranslationY() < 0) {
                    notDown = false;
                }
            }
        }

        for (int i = 0; i < getChildCount(); i++) {
            View childView = getChildAt(i);
            int yv = (int) (childView.getTranslationY() + offsetY);
            if (direction) { // 向上滑动
                notDown = false;
                if (!notUp) {
                    if (Math.abs(yv) >= listMaxOffsetY) {
                        notUp = true;
                    }
                }
                if (!notUp) childView.setTranslationY(yv);
            } else { // 向下滑动
                notUp = false;
                if (!notDown) {
                    if (yv >= 0) {
                        notDown = true;
                    }
                }
                if (!notDown) childView.setTranslationY(yv);
            }
        }
    }

    /**
     * 惯性任务
     * @param velocityX X轴速度
     * @param velocityY Y轴速度
     * @return
     */
    private boolean fling(int velocityX, int velocityY) {
        if (Math.abs(velocityY) > mMinimumVelocity) {
            flingTask = new FlingTask(Math.abs(velocityY), mHandler, new FlingTask.FlingTaskCallback() {
                @Override
                public void executeTask(int dy) {
                    if (direction) { // 向上滑动
                        translationViewY(-dy);
                    } else { // 向下滑动
                        translationViewY(dy);
                    }
                }

                @Override
                public void stopTask() {
                    setScrollState(SCROLL_STATE_IDLE);
                }
            });

            flingTask.run();
            setScrollState(SCROLL_STATE_SETTLING);
            return true;
        }
        return false;
    }

    /**
     * 停止惯性滚动任务
     */
    private void stopFling() {
        if (mScrollState == SCROLL_STATE_SETTLING) {
            if (flingTask != null) {
                flingTask.stopTask();
                setScrollState(SCROLL_STATE_IDLE);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopFling();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        boolean eventAddedToVelocityTracker = false;

        // 获取一个新的VelocityTracker对象来观察滑动的速度
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        // 返回正在执行的操作，不包含触摸点索引信息。即事件类型，如MotionEvent.ACTION_DOWN
        final int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();// Action的索引

        // 复制事件信息创建一个新的事件，防止被污染
        final MotionEvent copyEv = MotionEvent.obtain(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN: { // 手指按下
                stopFling();

                // 特定触摸点相关联的触摸点id,获取第一个触摸点的id
                mScrollPointerId = event.getPointerId(0);

                // 记录down事件的X、Y坐标
                mInitialTouchX = mLastTouchX = (int) (event.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (event.getY() + 0.5f);
            }
            break;
            case MotionEvent.ACTION_POINTER_DOWN: { // 多个手指按下
                // 更新mScrollPointerId,表示只会响应最近按下的手势事件
                mScrollPointerId = event.getPointerId(actionIndex);

                // 更新最近的手势坐标
                mInitialTouchX = mLastTouchX = (int) (event.getX() + 0.5f);
                mInitialTouchY = mLastTouchY = (int) (event.getY() + 0.5f);
            }
            break;
            case MotionEvent.ACTION_MOVE: { // 手指移动
                setScrollState(SCROLL_STATE_DRAGGING);

                // 根据mScrollPointerId获取触摸点下标
                final int index = event.findPointerIndex(mScrollPointerId);

                // 根据move事件产生的x，y来计算偏移量dx，dy
                final int x = (int) (event.getX() + 0.5f);
                final int y = (int) (event.getY() + 0.5f);

                int dx = Math.abs(mLastTouchX - x);
                int dy = Math.abs(mLastTouchY - y);

                // 在手指拖动状态下滑动
                if (mScrollState == SCROLL_STATE_DRAGGING) {
                    if (mLastTouchY - y > 0.5f) {
                        direction = true;
                        // Log.d("TAG", "向上");
                        translationViewY(-dy);
                    } else if (y - mLastTouchY > 0.5f) {
                        direction = false;
                        // Log.d("TAG", "向下");
                        translationViewY(dy);
                    }
                }

                mLastTouchX = x;
                mLastTouchY = y;
            }
            break;
            case MotionEvent.ACTION_POINTER_UP: { // 多个手指离开
                // 选择一个新的触摸点来处理结局，重新处理坐标
                onPointerUp(event);
            }
            break;
            case MotionEvent.ACTION_UP: { // 手指离开，滑动事件结束
                mVelocityTracker.addMovement(copyEv);
                eventAddedToVelocityTracker = true;

                // 计算滑动速度
                // mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

                // 最后一次 X/Y 轴的滑动速度
                final float xVel = -mVelocityTracker.getXVelocity(mScrollPointerId);
                final float yVel = -mVelocityTracker.getYVelocity(mScrollPointerId);

                if (!((xVel != 0 || yVel != 0) && fling((int) xVel, (int) yVel))) {
                    setScrollState(SCROLL_STATE_IDLE); // 设置滑动状态
                }
                resetScroll(); // 重置滑动
            }
            break;
            case MotionEvent.ACTION_CANCEL: { //手势取消，释放各种资源
                cancelScroll(); // 退出滑动
            }
            break;
        }

        if (!eventAddedToVelocityTracker) {
            // 回收滑动事件，方便重用，调用此方法你不能再接触事件
            mVelocityTracker.addMovement(copyEv);
        }

        // 回收滑动事件，方便重用
        copyEv.recycle();
        return true;
    }

    /**
     * 有新手指触摸屏幕，更新初始坐标
     * @param e
     */
    private void onPointerUp(MotionEvent e) {
        final int actionIndex = e.getActionIndex();
        if (e.getPointerId(actionIndex) == mScrollPointerId) {
            // Pick a new pointer to pick up the slack.
            final int newIndex = actionIndex == 0 ? 1 : 0;
            mScrollPointerId = e.getPointerId(newIndex);
            mInitialTouchX = mLastTouchX = (int) (e.getX(newIndex) + 0.5f);
            mInitialTouchY = mLastTouchY = (int) (e.getY(newIndex) + 0.5f);
        }
    }

    /**
     * 手指离开屏幕
     */
    private void cancelScroll() {
        resetScroll();
        setScrollState(SCROLL_STATE_IDLE);
    }

    /**
     * 重置速度
     */
    private void resetScroll() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
    }

    /**
     * 更新 滚动状态
     * @param state
     */
    private void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        mScrollState = state;
    }

}
