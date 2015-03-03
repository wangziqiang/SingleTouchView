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
import android.util.TypedValue;
import android.view.View;

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
     * 控制缩放，旋转图标所在的四个点的位置
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
     * 用于平移旋转缩放的Bitmap
     */
    private Bitmap mBitmap;
    /**
     * SingleTouchView的中心点坐标，相对于其父类布局而言的
     */
    private PointF mCenterPoint = new PointF();
    /**
     * View的宽度和高度，随着图片的旋转而变化(不包括控制旋转，缩放图片的宽高)
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
     * 图片的四个点坐标
     */
    private Point mLTPoint;
    private Point mRTPoint;
    private Point mRBPoint;
    private Point mLBPoint;
    /**
     * 用于缩放，旋转的控制点的坐标
     */
    private Point mControlPoint = new Point();
    /**
     * 用于缩放旋转的图标
     */
    private Drawable controlDrawable;
    /**
     * 缩放旋转图标的宽和高
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

        // TODO transformDraw();
        transformDraw();
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
        matrix.setScale(mScale,mScale);
        // 绕着图片中心进行旋转
        matrix.postRotate(mDegree / 360,bitmapWidth / 2,bitmapHeight / 2);
        // 设置画该图片的起始点
        matrix.postTranslate(offsetX + mDrawableWidth / 2,offsetY + mDrawableHeight / 2);

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
        // 获取旋转degree角度后的点 TODO
        mLTPoint = obtainRotationPoint(cp, lt, degree);
        mRTPoint = obtainRotationPoint(cp, rt, degree);
        mRBPoint = obtainRotationPoint(cp, rb, degree);
        mLBPoint = obtainRotationPoint(cp, lb, degree);

        //计算X坐标最大的值和最小的值
        int maxCoordinateX = getMaxValue(mLTPoint.x, mRTPoint.x, mRBPoint.x, mLBPoint.x);
        int minCoordinateX = getMinValue(mLTPoint.x, mRTPoint.x, mRBPoint.x, mLBPoint.x);;

        mViewWidth = maxCoordinateX - minCoordinateX ;


        //计算Y坐标最大的值和最小的值
        int maxCoordinateY = getMaxValue(mLTPoint.y, mRTPoint.y, mRBPoint.y, mLBPoint.y);
        int minCoordinateY = getMinValue(mLTPoint.y, mRTPoint.y, mRBPoint.y, mLBPoint.y);

        mViewHeight = maxCoordinateY - minCoordinateY ;


        //View中心点的坐标
        Point viewCenterPoint = new Point((maxCoordinateX + minCoordinateX) / 2, (maxCoordinateY + minCoordinateY) / 2);

        offsetX = mViewWidth / 2 - viewCenterPoint.x;
        offsetY = mViewHeight / 2 - viewCenterPoint.y;



        int halfDrawableWidth = mDrawableWidth / 2;
        int halfDrawableHeight = mDrawableHeight /2;

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
     *
     * @param center
     * @param source
     * @param degree
     * @return
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
     * @return
     */
    private Point LocationToPoint(int location){
        switch(location){
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
     * 获取变长参数最大的值
     * @param array
     * @return
     */
    public int getMaxValue(Integer...array){
        List<Integer> list = Arrays.asList(array);
        Collections.sort(list);
        return list.get(list.size() -1);
    }


    /**
     * 获取变长参数最大的值
     * @param array
     * @return
     */
    public int getMinValue(Integer...array){
        List<Integer> list = Arrays.asList(array);
        Collections.sort(list);
        return list.get(0);
    }
}
