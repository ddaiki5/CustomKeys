package com.example.customkeys;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.Canvas;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;

import java.util.ArrayList;
import java.util.List;

/**
 * 候補ビューの実装例
 * @author kob
 * @version 1.0
 * https://sites.google.com/site/cobgroupsite/android/programing/ime_2?tmpl=%2Fsystem%2Fapp%2Ftemplates%2Fprint%2F&showPrintDialog=1
 */


public abstract class CandidateView extends HorizontalScrollView
{
    private Drawable selHighlight;
    private int xgap ;
    private int lineHeight ;
    private int bottomPadding ;
    private List<String> candidate = new ArrayList<String>() ;
    private static final int OUT_OF_BOUNDS = -1;
    private int touchX = OUT_OF_BOUNDS ;
    private int selectIndex = -1 ;
    private int selectWidth = 0 ;
    private CanvasView view ;

    /**
     * コンストラクタ
     * @param context   コンテキスト
     */
    public CandidateView(Context context)
    {
        super(context);

        Resources r = context.getResources();
        selHighlight = r.getDrawable( android.R.drawable.list_selector_background ) ;
        selHighlight.setState(new int[] {
                android.R.attr.state_enabled,
                android.R.attr.state_focused,
                android.R.attr.state_window_focused,
                android.R.attr.state_pressed
        });
        lineHeight = r.getDimensionPixelSize(R.dimen.candidate_line_height);
        bottomPadding = r.getDimensionPixelSize(R.dimen.candidate_bottom_padding);
        view = new CanvasView( context ) ;
        addView(view) ;
    }

    /**
     * 候補をクリアする。
     */
    public void clear()
    {
        candidate.clear() ;
        touchX = OUT_OF_BOUNDS ;
        selectIndex = -1 ;
    }

    /**
     * 候補を追加する。
     * @param item  項目
     * @return  trueを返す。
     */
    public boolean add(String item)
    {
        return candidate.add( item ) ;
    }

    /**
     * 候補の数を取得
     * @return  候補の数
     */
    public int size()
    {
        return candidate.size() ;
    }

    /**
     * 表示の更新
     */
    public void update()
    {
        view.measure(view.getMeasuredWidth(),view.getMeasuredHeight() ) ;
        scrollTo(0,0) ;
        requestLayout() ;
    }

    /**
     * 候補選択時に呼ばれる処理。
     * @param index
     * @param text
     */
    public abstract void onSelectCandidate( int index, String text ) ;

    /**
     * レイアウトが変更されたときに呼ばれる処理
     * @see android.widget.HorizontalScrollView#onLayout(boolean, int, int, int, int)
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(changed)
        {
            // ここで親のレイアウトが決まるので、子の大きさを再計測させる。
            // 子の最小幅を、ウィンドウ幅にさせるため。
            view.measure(view.getMeasuredWidth(),view.getMeasuredHeight() ) ;
        }
        super.onLayout(changed, l, t, r, b);
    }

    public void debugCandidate(){
        for(String s: candidate){
            Log.d("candidate", s);
        }
    }

    /**
     * 候補を並べるビューのクラス。
     */
    private class CanvasView extends View
    {
        private Paint p ;

        public CanvasView(Context context)
        {
            super(context);

            int ts = context.getResources().getDimensionPixelSize(R.dimen.candidate_font_height) ;
            p = new Paint() ;
            p.setColor( Color.BLACK ) ;
            p.setAntiAlias(true) ;
            p.setTextSize(ts) ;
            p.setStrokeWidth(0);
            p.setFakeBoldText(false);

            setBackgroundResource(R.drawable.candidatebg) ;

            xgap = ts  ;              // 両端のマージンを文字サイズ程度空ける。
            lineHeight = ts + ts ;    // 上下のマージンを文字サイズの1/2程空けたときの高さ。

            // implementsだと飛んでこない。
            setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(touchX==OUT_OF_BOUNDS) { return ; }
                    String word = "" ;
                    int x = 0 ;
                    final int count = candidate.size();
                    for(int i = 0; i < count; i++)
                    {
                        word = candidate.get(i) ;
                        int w = (int)( p.measureText(word) + 0.5f ) + xgap + xgap ;
                        if( touchX >= x && touchX < x+w )
                        {
                            selectIndex = i ;
                            selectWidth = w ;
                            onSelectCandidate( i, word ) ;
                            invalidate() ;
                            break ;
                        }
                        x += w ;
                    }
                }
            }) ;
        }

        /**
         * タッチイベントの実装
         * @see android.view.View#onTouchEvent(android.view.MotionEvent)
         */
        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            touchX = (int) event.getX();
            return super.onTouchEvent(event);
        }

        /**
         * 描画処理の実装
         * @see android.view.View#onDraw(Canvas canvas)
         */
        @Override
        protected void onDraw(Canvas canvas)
        {
            final int count = candidate.size();
            final int height = getHeight();
            int x = 0 ;
            final int y = (int)(((lineHeight - p.getTextSize()) / 2) - p.ascent());

            for(int i = 0; i < count; i++)
            {
                String text = candidate.get(i);
                if(i==selectIndex)
                {
                    selHighlight.setBounds(x, 0, x+selectWidth, height);
                    selHighlight.draw(canvas);
                }
                x += xgap ;
                p.setAntiAlias(true) ;
                canvas.drawText(text, x, y, p);
                x += p.measureText(text) + 0.5f ;
                x += xgap ;
                p.setAntiAlias(false) ;
                canvas.drawLine(x, 0.0f, x, height, p);
            }
        }

        /**
         * ビューサイズ測定の実装
         * @see android.view.View#onMeasure(int, int)
         */
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
        {
            int x = 0 ;
            final int width = CandidateView.this.getWidth() ;
            final int count = candidate.size();
            for(int i = 0; i < count; i++)
            {
                x += (int)(p.measureText(candidate.get(i)) + 0.5f) + xgap + xgap ;
            }
            x = Math.max(x,width) ;
            x = resolveSize(x, widthMeasureSpec) ;
            setMeasuredDimension(resolveSize(x, widthMeasureSpec),
                    resolveSize(lineHeight+bottomPadding, heightMeasureSpec));
        }
    }
}
