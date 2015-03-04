package com.sf.wzq.singletouchview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by sf on 2015/3/3.
 * 单手对图片进行 平移、缩放、旋转等操作
 */
public class SingleTouchView extends View {
    /**
     * 图片的最大缩放比例
     */
    public static final float MAX_SCALE = 10.0f;
    /**
     * 图片的最小缩放比例
     */
    public static final float MIN_SCALE = 0.3f;
    /**
     * 控制(缩放，旋转)图标所在的四个点的位置
     */
    public static final int LEFT_TOP = 0;
    public static final int RIGHT_TOP = 1;
    public static final int RIGHT_BOTTOM = 2;
    public static final int LEFT_BOTTOM = 3;
    /**
     * 一些默认的常量值
     */
    public static final int DEFAULT_FRAME_PADDING = 8;//默认边框间距
    public static final int DEFAULT_FRAME_WIDTH = 2;//默认边框宽度
    public static final int DEFAULT_FRAME_COLOR = Color.WHITE;//默认边框颜色
    public static final float DEFAULT_SCALE = 1.0f;//默认缩放比例
    public static final float DEFAULT_DEGREE = 0;//默认旋转角度
    public static final int DEFAULT_CONTROL_LOCATION = RIGHT_TOP;//控制图标默认位置
    public static final boolean DEFAULT_EDITABLE = true;//默认编辑模式
    public static final int DEFAULT_OTHER_DRAWABLE_WIDTH = 50;
    public static final int DEFAULT_OTHER_DRAWABLE_HEIGHT = 50;
    /**
     * 用于平移/旋转/缩放的Bitmap
     */
    private Bitmap mBitmap;
    /**
     * SingleTouchView的中心点坐标，相对于其父类布局而言的
     */
    private PointF mCenterPoint = new PointF();
    /**
     * View的宽度和高度，随着图片的旋转而变化(不包括控制图标的宽高)
     */
    private int mViewWidth, mViewHeight;
    /**
     * 图片的旋转角度
     */
    private float mDegree = DEFAULT_DEGREE;
    /**
     * 图片的缩放比例
     */
    private float mScale = DEFAULT_SCALE;
    /**
     * 用于缩放，旋转，平移的矩阵
     */
    private Matrix matrix = new Matrix();
    /**
     * SingleTouchView距离父布局的左间距
     */
    private int mViewPaddingLeft;
    /**
     * SingleTouchView距离父布局的上间距
     */
    private int mViewPaddingTop;
    /**
     * 图片的四个角的点坐标
     */
    private Point mLTPoint;
    private Point mRTPoint;
    private Point mRBPoint;
    private Point mLBPoint;
    /**
     * 用于缩放，旋转的控制图标的中心点，值为图片四个角坐标中的一个。
     */
    private Point mControlPoint = new Point();
    /**
     * 用于缩放旋转的控制图标
     */
    private Drawable controlDrawable;
    /**
     * 控制(缩放旋转)图标的宽和高
     */
    private int mDrawableWidth, mDrawableHeight;
    /**
     * 画外围框的Path
     */
    private Path mPath = new Path();
    /**
     * 画外围框的画笔
     */
    private Paint mPaint;
    /**
     * 初始状态
     */
    public static final int STATUS_INIT = 0;
    /**
     * 拖动状态
     */
    public static final int STATUS_DRAG = 1;
    /**
     * 缩放 或 旋转 状态
     */
    public static final int STATUS_ROTATE_ZOOM = 2;
    /**
     * 当前的状态
     */
    private int mStatus = STATUS_INIT;
    /**
     * 外边框与图片之间的间距，单位dip
     */
    private int framePadding = DEFAULT_FRAME_PADDING;
    /**
     * 外边框的颜色
     */
    private int frameColor = DEFAULT_FRAME_COLOR;
    /**
     * 外边框线条粗细，单位dip
     */
    private int frameWidth = DEFAULT_FRAME_WIDTH;
    /**
     * 是否处于可以 平移、缩放、旋转 状态
     */
    private boolean isEditable = DEFAULT_EDITABLE;
    private DisplayMetrics Metrics;
    private PointF mPreMovePointF = new PointF();
    private PointF mCurMovePointF = new PointF();
    /**
     * 图片在旋转时X方向的偏移量
     */
    private int offsetX;
    /**
     * 图片在旋转时Y方向的偏移量
     */
    private int offsetY;
    /**
     * 控制图标所在的位置(左上，右上，左下，右下）
     */
    private int controlLocation = DEFAULT_CONTROL_LOCATION;

    private DisplayMetrics metrics;

    public SingleTouchView(Context context) {
        this(context, null);
    }

    public SingleTouchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SingleTouchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        obtainStyledAttributes(attrs);
        init();
    }

    /**
     * 获取自定义属性
     *
     * @param attrs
     */
    private void obtainStyledAttributes(AttributeSet attrs) {
        metrics = getContext().getResources().getDisplayMetrics();
        framePadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_FRAME_PADDING, metrics);
        frameWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_FRAME_WIDTH, metrics);

        TypedArray mTypedArray = getContext().obtainStyledAttributes(attrs, R.styleable.SingleTouchView);
        Drawable srcDrawable = mTypedArray.getDrawable(R.styleable.SingleTouchView_src);
        // 将drawable转变为bitmap
        mBitmap = drawable2Bitmap(srcDrawable);

        framePadding = mTypedArray.getDimensionPixelSize(R.styleable.SingleTouchView_framePadding, framePadding);
        frameWidth = mTypedArray.getDimensionPixelSize(R.styleable.SingleTouchView_frameWidth, frameWidth);
        frameColor = mTypedArray.getColor(R.styleable.SingleTouchView_frameColor, DEFAULT_FRAME_COLOR);
        mScale = mTypedArray.getFloat(R.styleable.SingleTouchView_scale, DEFAULT_SCALE);
        mDegree = mTypedArray.getFloat(R.styleable.SingleTouchView_degree, DEFAULT_DEGREE);
        controlDrawable = mTypedArray.getDrawable(R.styleable.SingleTouchView_controlDrawable);
        controlLocation = mTypedArray.getInt(R.styleable.SingleTouchView_controlLocation, DEFAULT_CONTROL_LOCATION);
        isEditable = mTypedArray.getBoolean(R.styleable.SingleTouchView_editable, DEFAULT_EDITABLE);
        mTypedArray.recycle();
    }

    /**
     * 从Drawable中获取Bitmap对象
     *
     * @param srcDrawable
     * @return
     */
    private Bitmap drawable2Bitmap(Drawable srcDrawable) {

        if (srcDrawable == null) return null;
        if (srcDrawable instanceof BitmapDrawable)
            return ((BitmapDrawable) srcDrawable).getBitmap();

        int intrinsicWidth = srcDrawable.getIntrinsicWidth();
        int intrinsicHeight = srcDrawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(intrinsicWidth == -1 ? DEFAULT_OTHER_DRAWABLE_WIDTH : intrinsicWidth,
                intrinsicHeight == -1 ? DEFAULT_OTHER_DRAWABLE_HEIGHT : intrinsicHeight, Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        srcDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        srcDrawable.draw(canvas);

        return bitmap;
    }


    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(frameColor);
        mPaint.setStrokeWidth(frameWidth);
        mPaint.setStyle(Paint.Style.STROKE);

        if (controlDrawable == null) {
            controlDrawable = getContext().getResources().getDrawable(R.drawable.icon_rotate_zoom);
        }
        mDrawableWidth = controlDrawable.getIntrinsicWidth();
        mDrawableHeight = controlDrawable.getIntrinsicHeight();

        transformDraw();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        System.out.println("method -- onMeasure");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // 获取SingleTouchView所在父布局的中心点，设置为mCenterPoint
        ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) {
            int width = parent.getWidth();
            int height = parent.getHeight();
            System.out.println("onMeasure -- mCenterPoint.x = "+width+"; mCenterPoint.y ="+height);
            mCenterPoint.set(width / 2, height / 2);
        }
    }

    /**
     * 调整View的大小，位置
     */
    private void adjustLayout() {
        int actualWidth = mViewWidth + mDrawableWidth;
        int actualHeight = mViewHeight + mDrawableHeight;

        int newPaddingLeft = (int) (mCenterPoint.x - actualWidth / 2);
        int newPaddingTop = (int) (mCenterPoint.y - actualHeight / 2);

        if (mViewPaddingLeft != newPaddingLeft || mViewPaddingTop != newPaddingTop) {

            mViewPaddingLeft = newPaddingLeft;
            mViewPaddingTop = newPaddingTop;

            System.out.println("View的 L = " + newPaddingLeft + "; T = " + newPaddingTop + "; R = " + newPaddingLeft + actualWidth + "; B = " + newPaddingTop + actualHeight);
            layout(newPaddingLeft, newPaddingTop, newPaddingLeft + actualWidth, newPaddingTop + actualHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // 每次画之前调整位置和大小
        adjustLayout();

        super.onDraw(canvas);

        if (mBitmap == null) return;
        canvas.drawBitmap(mBitmap, matrix, null);

        //处于编辑状态太画边框和控制图标
        if (isEditable) {
            // 1,画边框
            mPath.reset();
            mPath.moveTo(mLTPoint.x, mLTPoint.y);
            mPath.lineTo(mRTPoint.x, mRTPoint.y);
            mPath.lineTo(mRBPoint.x, mRBPoint.y);
            mPath.lineTo(mLBPoint.x, mLBPoint.y);
            mPath.lineTo(mLTPoint.x, mLTPoint.y);
            canvas.drawPath(mPath, mPaint);
            // 2,画控制图标
            controlDrawable.setBounds(mControlPoint.x - mDrawableWidth / 2, mControlPoint.y - mDrawableHeight / 2,
                    mControlPoint.x + mDrawableWidth / 2, mControlPoint.y + mDrawableHeight / 2);
            controlDrawable.draw(canvas);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isEditable) return super.onTouchEvent(event);
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mPreMovePointF.set(event.getX() + mViewPaddingLeft,event.getY() + mViewPaddingTop);
                mStatus = judgeStatus(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
                mStatus = STATUS_INIT;
                break;
            case MotionEvent.ACTION_MOVE:
                mCurMovePointF.set(event.getX() + mViewPaddingLeft,event.getY() + mViewPaddingTop);
                if(mStatus == STATUS_DRAG){
                    // 修改中心点
                    mCenterPoint.x += mCurMovePointF.x - mPreMovePointF.x;
                    mCenterPoint.y += mCurMovePointF.y - mPreMovePointF.y;

                    adjustLayout();
                }else if(mStatus == STATUS_ROTATE_ZOOM){
                    float scale = 1f;

                    int halfBitmapWidth = mBitmap.getWidth() / 2;
                    int halfBitmapHeight = mBitmap.getHeight() /2 ;

                    //图片某个点到图片中心的距离
                    float bitmapToCenterDistance = FloatMath.sqrt(halfBitmapWidth * halfBitmapWidth + halfBitmapHeight * halfBitmapHeight);

                    //移动的点到图片中心的距离
                    float moveToCenterDistance = distance4PointF(mCenterPoint, mCurMovePointF);

                    //计算缩放比例
                    scale = moveToCenterDistance / bitmapToCenterDistance;

                    //缩放比例的界限判断
                    if (scale <= MIN_SCALE) {scale = MIN_SCALE;}
                    else if (scale >= MAX_SCALE) {scale = MAX_SCALE;}

                    // 角度
                    double a = distance4PointF(mCenterPoint, mPreMovePointF);
                    double b = distance4PointF(mPreMovePointF, mCurMovePointF);
                    double c = distance4PointF(mCenterPoint, mCurMovePointF);

                    double cosb = (a * a + c * c - b * b) / (2 * a * c);

                    if (cosb >= 1) {
                        cosb = 1f;
                    }

                    double radian = Math.acos(cosb);
                    float newDegree = (float) radianToDegree(radian);

                    //center -> proMove的向量， 我们使用PointF来实现
                    PointF centerToProMove = new PointF((mPreMovePointF.x - mCenterPoint.x), (mPreMovePointF.y - mCenterPoint.y));

                    //center -> curMove 的向量
                    PointF centerToCurMove = new PointF((mCurMovePointF.x - mCenterPoint.x), (mCurMovePointF.y - mCenterPoint.y));

                    //向量叉乘结果, 如果结果为负数， 表示为逆时针， 结果为正数表示顺时针
                    float result = centerToProMove.x * centerToCurMove.y - centerToProMove.y * centerToCurMove.x;

                    if (result < 0) {
                        newDegree = -newDegree;
                    }

                    mDegree = mDegree + newDegree;
                    mScale = scale;

                    transformDraw();
                }
                mPreMovePointF.set(mCurMovePointF);
                break;
        }
        return  true;
    }

    private double radianToDegree(double radian) {
        return radian * 180 / Math.PI;
    }

    /**
     * 设置matrix,强制刷新
     */
    private void transformDraw() {
        int bitmapWidth = (int) (mBitmap.getWidth() * mScale);
        int bitmapHeight = (int) (mBitmap.getHeight() * mScale);
        // 计算四个点和View的大小
        computeRect(-framePadding, -framePadding, bitmapWidth + framePadding, bitmapHeight + framePadding, mDegree);

        // 设置缩放比例
        matrix.setScale(mScale, mScale);
        // 绕着图片中心进行旋转
        matrix.postRotate(mDegree / 360, bitmapWidth / 2, bitmapHeight / 2);
        // 设置画该图片的起始点
        matrix.postTranslate(offsetX + mDrawableWidth / 2, offsetY + mDrawableHeight / 2);

        invalidate();
    }

    /**
     * 获取四个点和View的大小
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     * @param degree
     */
    private void computeRect(int left, int top, int right, int bottom, float degree) {
        Point lt = new Point(left, top);
        Point rt = new Point(right, top);
        Point rb = new Point(right, bottom);
        Point lb = new Point(left, bottom);
        Point cp = new Point((left + right) / 2, (top + bottom) / 2);
        // 获取旋转degree角度后的点
        mLTPoint = obtainRotationPoint(cp, lt, degree);
        mRTPoint = obtainRotationPoint(cp, rt, degree);
        mRBPoint = obtainRotationPoint(cp, rb, degree);
        mLBPoint = obtainRotationPoint(cp, lb, degree);

        //计算X坐标最大的值和最小的值
        int maxCoordinateX = getMaxValue(mLTPoint.x, mRTPoint.x, mRBPoint.x, mLBPoint.x);
        int minCoordinateX = getMinValue(mLTPoint.x, mRTPoint.x, mRBPoint.x, mLBPoint.x);

        mViewWidth = maxCoordinateX - minCoordinateX;

        //计算Y坐标最大的值和最小的值
        int maxCoordinateY = getMaxValue(mLTPoint.y, mRTPoint.y, mRBPoint.y, mLBPoint.y);
        int minCoordinateY = getMinValue(mLTPoint.y, mRTPoint.y, mRBPoint.y, mLBPoint.y);

        mViewHeight = maxCoordinateY - minCoordinateY;


        //View中心点的坐标
        Point viewCenterPoint = new Point((maxCoordinateX + minCoordinateX) / 2, (maxCoordinateY + minCoordinateY) / 2);

        offsetX = mViewWidth / 2 - viewCenterPoint.x;
        offsetY = mViewHeight / 2 - viewCenterPoint.y;


        int halfDrawableWidth = mDrawableWidth / 2;
        int halfDrawableHeight = mDrawableHeight / 2;

        //将Bitmap的四个点的X的坐标移动offsetX + halfDrawableWidth
        mLTPoint.x += (offsetX + halfDrawableWidth);
        mRTPoint.x += (offsetX + halfDrawableWidth);
        mRBPoint.x += (offsetX + halfDrawableWidth);
        mLBPoint.x += (offsetX + halfDrawableWidth);

        //将Bitmap的四个点的Y坐标移动offsetY + halfDrawableHeight
        mLTPoint.y += (offsetY + halfDrawableHeight);
        mRTPoint.y += (offsetY + halfDrawableHeight);
        mRBPoint.y += (offsetY + halfDrawableHeight);
        mLBPoint.y += (offsetY + halfDrawableHeight);

        mControlPoint = LocationToPoint(controlLocation);

    }

    /**
     * 获取一个点旋转degree角度后的点
     */
    private Point obtainRotationPoint(Point center, Point source, float degree) {
        /**
         * sin(A+B) = sinAcosB+cosAsinB
         * cos(A+B) = cosAcosB-sinAsinB
         */
        //将degree转换为弧度
        double degreeRadian = degree * Math.PI / 180;
        //先求 点source 到 点center的距离
        Point disPoint = new Point(source.x - center.x, source.y - center.y);
        double radius = Math.sqrt(disPoint.x * disPoint.x + disPoint.y * disPoint.y);

        // 计算中间值sinA、sinB、cosA、cosB
        double sinA = disPoint.y / radius;
        double cosA = disPoint.x / radius;
        double sinB = Math.sin(degreeRadian);
        double cosB = Math.cos(degreeRadian);

        double sinAB = sinA * cosB + cosA * sinB;
        double cosAB = cosA * cosB - sinA * sinB;

        //旋转degree角度之后的点
        Point result = new Point();

        result.x = (int) Math.round(radius * cosAB);
        result.y = (int) Math.round(radius * sinAB);

        return result;
    }

    /**
     * 根据位置判断控制图标处于那个点
     */
    private Point LocationToPoint(int location) {
        switch (location) {
            case LEFT_TOP:
                return mLTPoint;
            case RIGHT_TOP:
                return mRTPoint;
            case RIGHT_BOTTOM:
                return mRBPoint;
            case LEFT_BOTTOM:
                return mLBPoint;
        }
        return mLTPoint;
    }

    /**
     * 根据点击的位置判断是否点中控制旋转，缩放的图片， 初略的计算
     */
    private int judgeStatus(float x, float y) {
        PointF touchPoint = new PointF(x, y);
        PointF controlPointF = new PointF(mControlPoint);

        //点击的点到控制旋转，缩放点的距离
        float distanceToControl = distance4PointF(touchPoint, controlPointF);

        //如果两者之间的距离小于 控制图标的宽度，高度的最小值，则认为点中了控制图标
        if (distanceToControl < Math.min(mDrawableWidth / 2, mDrawableHeight / 2)) {
            return STATUS_ROTATE_ZOOM;
        }

        return STATUS_DRAG;
    }

    /**
     * 两个 PointF 点之间的距离
     */
    private float distance4PointF(PointF pf1, PointF pf2) {
        float disX = pf2.x - pf1.x;
        float disY = pf2.y - pf1.y;
        return PointF.length(disX, disY);
    }

    /**
     * 设置旋转图
     */
    public void setImageBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
        transformDraw();
    }


    /**
     * 设置旋转图
     */
    public void setImageDrawable(Drawable drawable) {
        this.mBitmap = drawable2Bitmap(drawable);
        transformDraw();
    }

    /**
     * 根据id设置旋转图
     */
    public void setImageResource(int resId) {
        Drawable drawable = getContext().getResources().getDrawable(resId);
        setImageDrawable(drawable);
    }

    public float getImageDegree() {
        return mDegree;
    }

    /**
     * 设置图片旋转角度
     */
    public void setImageDegree(float degree) {
        if (this.mDegree != degree) {
            this.mDegree = degree;
            transformDraw();
        }
    }

    public float getImageScale() {
        return mScale;
    }

    /**
     * 设置图片缩放比例
     */
    public void setImageScale(float scale) {
        if (this.mScale != scale) {
            this.mScale = scale;
            transformDraw();
        }
    }

    public Drawable getControlDrawable() {
        return controlDrawable;
    }

    /**
     * 设置控制图标
     */
    public void setControlDrawable(Drawable drawable) {
        if (drawable == null) return;
        this.controlDrawable = drawable;
        mDrawableWidth = drawable.getIntrinsicWidth();
        mDrawableHeight = drawable.getIntrinsicHeight();
        transformDraw();
    }

    public int getFrameColor() {
        return frameColor;
    }

    public int getFramePadding() {
        return framePadding;
    }

    public void setFramePadding(int framePadding) {
        if (this.framePadding != framePadding) {
            this.framePadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, framePadding, metrics);
            transformDraw();
        }
    }

    public void setFrameColor(int frameColor) {
        if (this.frameColor != frameColor) {
            this.frameColor = frameColor;
            mPaint.setColor(frameColor);
            invalidate();
        }
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        if (this.frameWidth != frameWidth) {
            this.frameWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, frameWidth, metrics);
            mPaint.setStrokeWidth(frameWidth);
            invalidate();
        }
    }

    /**
     * 设置控制图标的位置, 设置的值只能选择LEFT_TOP ，RIGHT_TOP， RIGHT_BOTTOM，LEFT_BOTTOM
     */
    public void setControlLocation(int location) {
        if (this.controlLocation != location) {
            this.controlLocation = location;
            transformDraw();
        }
    }

    public int getControlLocation() {
        return controlLocation;
    }

    public PointF getCenterPoint() {
        return mCenterPoint;
    }

    /**
     * 设置图片中心点位置，相对于父布局而言
     */
    public void setCenterPoint(PointF mCenterPoint) {
        this.mCenterPoint = mCenterPoint;
        adjustLayout();
    }

    public boolean isEditable() {
        return isEditable;
    }

    @Override
    public CharSequence getContentDescription() {
        return super.getContentDescription();
    }

    /**
     * 设置是否处于可缩放，平移，旋转状态
     */
    public void setEditable(boolean isEditable) {
        this.isEditable = isEditable;
        invalidate();
    }

    /**
     * 获取变长参数最大的值
     */
    public int getMaxValue(Integer... array) {
        List<Integer> list = Arrays.asList(array);
        Collections.sort(list);
        return list.get(list.size() - 1);
    }


    /**
     * 获取变长参数最大的值
     */
    public int getMinValue(Integer... array) {
        List<Integer> list = Arrays.asList(array);
        Collections.sort(list);
        return list.get(0);
    }
}
