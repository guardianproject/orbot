package org.torproject.android.ui;

import java.util.Random;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class ImageProgressView extends ImageView
{

	  private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

	  private float progress = 0f; // 0 to 1
	  
	  private RectF circle;
	  
	public ImageProgressView(Context context) {
	 super(context);
	 // TODO Auto-generated constructor stub
	 init();
	 
	}

	public ImageProgressView(Context context, AttributeSet attrs) {
	 super(context, attrs);
	 init();
	}

	public ImageProgressView(Context context, AttributeSet attrs, int defStyle) {
	 super(context, attrs, defStyle);
	 
	 init();
	}

	private void init(){
	 paint.setStyle(Paint.Style.STROKE);
	 paint.setColor(Color.GREEN);
	 paint.setAntiAlias(true);
	 paint.setStrokeWidth(20);
	 
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	   super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	   
	   setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
	   MeasureSpec.getSize(heightMeasureSpec));
	 
	}

	@Override
	protected void onDraw(Canvas canvas) {

		super.onDraw(canvas);
		
		if (circle == null)
		{
		circle = new RectF(getWidth()/2,getHeight()/2+getHeight()/8, getWidth()/3,getHeight()/3);
		}
		
		float sweepAngle = 360f * progress;
		
		canvas.drawArc(circle, 0, sweepAngle, true, paint);
		
	}
	
	

	
}