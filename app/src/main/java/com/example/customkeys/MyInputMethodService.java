package com.example.customkeys;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Selection;
import android.util.Log;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard keyboard;

    private boolean caps = false;
    private boolean isSelectionMode = false;


    private boolean isLongPress = false;
    private List<Keyboard.Key> keylist;

    private float tapX = 0;
    private float tapY = 0;

    //BreakIteratorを使ったカーソル操作のためのクラス
    public class Memo{
        public static final int LEFT=-1, RIGHT=1;
        public CharSequence text;
        private BreakIterator sentenceBoundary, wordBoundary;

        Memo(){
            sentenceBoundary = BreakIterator.getSentenceInstance(Locale.JAPANESE);
            wordBoundary = BreakIterator.getWordInstance(Locale.JAPANESE);
        }

        public void setText(CharSequence t){
            text = t;
            sentenceBoundary.setText(t.toString());
            wordBoundary.setText(t.toString());
        }

        //文字のまとまりごとにカーソルを移動するとき移動後のカーソル位置を取得
        public int getNearestCursorNum(int i, int direction){
            int wordLimit = wordBoundary.following(i);
            if(direction==RIGHT) {
                wordLimit = wordBoundary.next();
            }else if(direction==LEFT){
                wordLimit = wordBoundary.next(-2);
            }
            if(wordLimit == BreakIterator.DONE){
                return BreakIterator.DONE;
            }
            if(wordLimit < 0){
                wordLimit = 0;
            }
            return wordLimit;
        }

        //句点のまとまりごとにカーソルを移動するときの移動後のカーソル位置を取得
        public int getNearestSentenceCursor(int i, int direction){
            int wordLimit = sentenceBoundary.following(i);
            if(direction==1){
                wordLimit = sentenceBoundary.next();
            }else{
                wordLimit = sentenceBoundary.next(-2);
            }
            if(wordLimit == BreakIterator.DONE){
                if(direction==LEFT) {
                    wordLimit=0;
                }else{
                    wordLimit= text.length();
                }
            }

            return wordLimit;
        }
    }

    private Memo memo;
    private int currentStartIndex, currentEndIndex, currentIndex;
    private int keyDirection = 0;//left=1, up=2, right=3, down=4

    private MyCandidateView candidateView = null;

    //未確定文字のバッファー
    private StringBuilder mComposing = new StringBuilder();
    private MyDictionary dictionary;

    private class MyCandidateView extends CandidateView{
        public MyCandidateView(Context context){
            super(context);
        }

        @Override
        public void onSelectCandidate(int index, String text){
            getCurrentInputConnection().commitText(text, text.length());
            mComposing.setLength(0);
            setCandidatesViewShown(false);
            super.clear();
            super.update();
        }
    }

    /**
     * 変換候補の単語を扱うクラス
     */
    private class MyDictionary{
        public MyDictionary(Context context){
        }

        private void httpRequest(String url, CandidateView candidateview) throws IOException {
            candidateview.clear();
            OkHttpClient client = new OkHttpClient();

            //リクエストを行う
            Request request = new Request.Builder().url(url).build();

            //非同期でレスポンスの処理を行う
            client.newCall(request).enqueue(new Callback() {

                //エラーのとき
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("Hoge", e.getMessage());
                }

                //正常のとき
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                    //レスポンスの取り出し
                    String jsonStr = response.body().string();
                    Log.d("Hoge","jsonStr=" + jsonStr);

                    //json処理
                    try{
                        //jsonパース　送られてくるのはリスト構造
                        JSONArray array = new JSONArray(jsonStr);
                        candidateview.clear();

                        //親スレッドを更新
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                for(int i=0; i< array.length(); i++){
                                    try {
                                        JSONArray s = array.getJSONArray(i);
                                        s = s.getJSONArray(1);

                                        for(int j=0; j<s.length(); j++){
                                            try {
                                                candidateview.add(s.getString(j));
                                            }catch (Exception e){
                                                Log.d("error", candidateview.getClass().toString());
                                            }
                                        }
                                    }catch (Exception e){
                                        Log.d("error", candidateview.getClass().toString());
                                    }

                                }
                                candidateview.update();
                                //Log.d("update", "viewupdate");
                                setCandidatesViewShown(candidateview.size()>0);
                                candidateview.invalidate();
                                //candidateview.debugCandidate();
                                setCandidatesView(candidateview);
                            }
                        });


                    }catch(Exception e){
                        Log.e("Hoge",e.getMessage());
                    }

                }
            });
        }

        public void updateCandidateList(String inword, CandidateView view){
            //String target = new String(inword, "UTF-8");
            try {
                httpRequest("http://www.google.com/transliterate?langpair=ja-Hira|ja&text="+inword, view);
            }catch (Exception e){
                Log.e("Hoge",e.getMessage());
            }
        }

    }


    @Override
    public View onCreateInputView() {
        //keyboard_view.xmlに対応するViewをインスタンス化
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);

        //Keyboardクラスのインすランスを生成
        keyboard = new Keyboard(this, R.xml.keys_layout);
        keyboardView.setKeyboard(keyboard);

        //リスナーに設定
        keyboardView.setOnKeyboardActionListener(this);

        //keyのリストを取得(使ってない)
        keylist = keyboardView.getKeyboard().getKeys();

        //キーのポップアップは表示しない
        keyboardView.setPreviewEnabled(false);

        //フリック操作のためにタッチ操作のリスナーを登録　
        //参考：https://qiita.com/Dooteeen/items/5df3b4e9b11f60651c95
        keyboardView.setOnTouchListener(new View.OnTouchListener() {

            //画面にタッチしている
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //タッチした座標を保存
                float x = motionEvent.getX();
                float y = motionEvent.getY();

                //タッチ時のアクションによって分岐
                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK){
                    //画面に触れたとき
                    case MotionEvent.ACTION_DOWN:
                        //基準となる触れた部分の座標を保存
                        tapX = x;
                        tapY = y;

                        //フリックの向き
                        keyDirection=0;
                        return false;

                    //触れたまま動かしているとき
                    case MotionEvent.ACTION_MOVE:
                        Log.d("onTouch", "move");

                        //左にフリックしたとき
                        if(tapX-x>=keyboardView.getWidth()/8){
                            Log.d("TouchDir", "left");
                            keyDirection=1;

                        //右にフリックしたとき
                        }else if(tapX-x<=-keyboardView.getHeight()/8){
                            Log.d("TouchDir", "right");
                            keyDirection = 3;

                        //下にフリックしたとき
                        }else if(tapY-y<=-keyboardView.getWidth()/8){
                            Log.d("TouchDir", "down");
                            keyDirection=4;

                        //上にフリックしたとき
                        }else if(tapY-y>=keyboardView.getHeight()/8){
                            Log.d("TouchDir", "up");
                            keyDirection=2;

                        //フリックしていないとき
                        }else{
                            keyDirection = 0;
                        }
                        return true;

                    //画面から離したとき
                    case MotionEvent.ACTION_UP:
                        Log.d("onTouch", "end");
                        return false;
                    default:
                        Log.d("onTouch", "end");
                        return true;
                }
            }
        });

        //clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        memo = new Memo();

        return keyboardView;
    }

    @Override
    public void onInitializeInterface(){
        dictionary = new MyDictionary(this);
        super.onInitializeInterface();
    }

    //候補ビューの初期化
    @Override
    public View onCreateCandidatesView(){
        //ここで候補を表示するViewを定義する
        candidateView = new MyCandidateView(this);
        Log.d("log", "create CandidateView");
        return candidateView;
    }

    @Override
    public void onFinishInput(){
        mComposing.setLength(0);
        if(candidateView!=null) {
            candidateView.clear();
        }

        setCandidatesViewShown(false);
        super.onFinishInput();
    }

    @Override
    public void onPress(int i) {

    }

    @Override
    public void onRelease(int i) {

    }

    //キーを押したとき
    @Override
    public void onKey(int primaryCode, int[] keyCodes) {

        //アプリケーションとIMEの仲介を行うためのインスタンス
        InputConnection inputConnection = getCurrentInputConnection();

        if (inputConnection != null) {
            //編集しているテキスト情報を抽出するインスタンス
            ExtractedText extractedText = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);

            //BreakIteratorのためのテキスト更新
            memo.setText(extractedText.text);

            //現在のカーソルの位置を取得
            currentIndex = extractedText.startOffset;

            //現在の選択カーソルのはじまり位置を取得
            currentStartIndex = extractedText.startOffset + extractedText.selectionStart;

            //現在の選択カーソルの終わり位置を取得
            currentEndIndex = extractedText.startOffset + extractedText.selectionEnd;

            //押したキーのコードによって分岐
            switch(primaryCode) {

                //deleteキーのとき
                case Keyboard.KEYCODE_DELETE :
                    //選択中も文字を取得
                    CharSequence selectedText = inputConnection.getSelectedText(0);

                    if (TextUtils.isEmpty(selectedText)) {//選択中でないないなら

                        //未確定なら
                        if(mComposing.length()>0) {

                            //未確定の文字列の最後を消す
                            mComposing.setLength(mComposing.length() - 1);

                            //未確定文字列に反映
                            inputConnection.setComposingText(mComposing, 1);

                        }else{//未確定の文字列がないなら

                            //カーソルの一つ後ろを削除
                            inputConnection.deleteSurroundingText(1, 0);
                        }

                    } else {//選択中なら

                        //選択している文字列を消す
                        inputConnection.commitText("", 1);
                    }
                    break;

                //shiftキーのとき（現在使用していない）
                case Keyboard.KEYCODE_SHIFT:
                    caps = !caps;
                    keyboard.setShifted(caps);
                    keyboardView.invalidateAllKeys();
                    break;

                //リターンキーのとき
                case Keyboard.KEYCODE_DONE:
                    if(mComposing.length()>0){//未確定なら

                        //確定した文字列にする
                        inputConnection.commitText(mComposing, mComposing.length());

                        //未確定文字列を保存するバッファを消す
                        mComposing.setLength(0);
                    }else {

                        inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    }
                    break;
                case CustomCode.KEYCODE_COPY:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_COPY));
                    break;
                case CustomCode.KEYCODE_CUT:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CUT));
                    break;
                case CustomCode.KEYCODE_PASTE:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PASTE));
                    break;

                //前向きへの特殊カーソル移動を行う
                case CustomCode.KEYCODE_UNDO:
                    Log.d("UNDO", "push UNDO");
                    //isLongPress = !isLongPress;

                    if (isSelectionMode) {//選択モードのとき
                        if(keyDirection==2){//上向きにフリック

                            //現在のカーソルの位置からワード単位で左側に移動したカーソルを選択
                            inputConnection.setSelection(currentStartIndex, memo.getNearestCursorNum(currentStartIndex, memo.LEFT));
                        }else {

                            //現在のカーソルの位置から文単位で左側に移動したカーソルを選択
                            inputConnection.setSelection(currentStartIndex, memo.getNearestSentenceCursor(currentStartIndex, memo.LEFT));
                        }
                    }else {
                        if(keyDirection==2){//上向きにフリック

                            //ワード単位で左側にカーソルを移動
                            inputConnection.setSelection(memo.getNearestCursorNum(currentStartIndex, memo.LEFT), memo.getNearestCursorNum(currentStartIndex, memo.LEFT));
                        }else {

                            //文単位でカーソルを移動
                            inputConnection.setSelection(memo.getNearestSentenceCursor(currentStartIndex, memo.LEFT), memo.getNearestSentenceCursor(currentStartIndex, memo.LEFT));
                        }
                    }
                    break;

                case CustomCode.KEYCODE_REDO:
                    //ワード単位でカーソルを移動
                    Log.d("REDO", "push REDO");
                    int end = memo.getNearestCursorNum(currentStartIndex, memo.RIGHT);
                    inputConnection.setSelection(end, end);
                    break;

                case CustomCode.KEYCODE_REGION:
                    //選択モードを解除するときは選択を消す
                    if(isSelectionMode){
                        inputConnection.setSelection(currentStartIndex, currentStartIndex);
                    }

                    //選択モードへのフラグを変更
                    isSelectionMode = !isSelectionMode;
                    keyboard.setShifted(isSelectionMode);

                    //キーボードのビューにに変更を適用
                    keyboardView.invalidateAllKeys();
                    break;

                case CustomCode.KEYCODE_TAB:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
                    break;

                case CustomCode.KEYCODE_LEFTCURSOR:
                    if(isSelectionMode) {//選択モードのとき
                        if(keyDirection==2){//左にフリックしているとき

                            //現在のカーソルの位置から文単位で左側に移動したカーソルを選択
                            inputConnection.setSelection(currentStartIndex, memo.getNearestSentenceCursor(currentStartIndex, memo.LEFT));
                        }else {

                            ////現在のカーソルの位置から一つ左側に移動したカーソルを選択
                            inputConnection.setSelection(currentStartIndex, currentEndIndex - 1);
                        }
                    }else {
                        if(keyDirection==2){//左側にフリックしているとき

                            //現在のカーソルから文単位で移動
                            int leftSentenceEnd = memo.getNearestSentenceCursor(currentStartIndex, memo.LEFT);
                            inputConnection.setSelection(leftSentenceEnd, leftSentenceEnd);

                        }else if (isLongPress) {
                            inputConnection.setSelection(0, 0);
                        } else {

                            //一つ左にカーソルを移動
                            inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                        }
                    }
                    break;

                case CustomCode.KEYCODE_UPCURSOR:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
                    break;
                case CustomCode.KEYCODE_DOWNCURSOR:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
                    break;
                case CustomCode.KEYCODE_RIGHTCURSOR:
                    //ExtractedText rightExtractedText = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
                    if(isSelectionMode) {//選択モードなら
                        if(keyDirection==4) {//右にフリックしたとき
                            inputConnection.setSelection(currentStartIndex, memo.getNearestSentenceCursor(currentStartIndex, memo.RIGHT));
                        }else{
                            inputConnection.setSelection(currentStartIndex, currentEndIndex + 1);
                        }
                    }else {
                        if(keyDirection==4){//右にフリックしたとき
                            int rightSentenceEnd = memo.getNearestSentenceCursor(currentStartIndex, memo.RIGHT);
                            inputConnection.setSelection(rightSentenceEnd, rightSentenceEnd);
                        }else if (isLongPress) {
                            int index = extractedText.text.length();
                            inputConnection.setSelection(index, index);
                        } else {
                            inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                        }
                    }
                    break;
                case CustomCode.KEYCODE_DIACRITIC: //濁点半濁点促音の変換
                    //カーソルの一つ前の文字を取得
                    CharSequence diacritic_target = inputConnection.getTextBeforeCursor(1, 0);
                    Log.d("diacritic_target", diacritic_target.toString());
                    if(diacritic_target==null) {//一つ前に文字がないとき
                        break;
                    }
                    //テーブルに従って変換を行う
                    String converted_target = CustomCode.convert_diatric(diacritic_target.toString());
                    Log.d("converted_target", converted_target);

                    if(converted_target != null) {//変換できるとき

                        if(mComposing.length()>0) {//未確定の文字があるとき

                            //未確定文字列のバッファーを一文字消す
                            mComposing.setLength(mComposing.length()-1);

                            //未確定文字に変換後の文字を入れる
                            mComposing.append(converted_target);
                            inputConnection.setComposingText(mComposing.toString(), mComposing.length());
                        }
                    }
                    break;
                default :
                    if (primaryCode>=CustomCode.KEYCODE_kana && primaryCode <=CustomCode.KEYCODE_kana_end){//keycodeがひらがなを示しているとき

                        //未確定文字列のバッファーにいれる
                        mComposing.append(CustomCode.NUM_TO_FIFTY[primaryCode-CustomCode.KEYCODE_kana + keyDirection]);

                        inputConnection.setComposingText(mComposing.toString(), mComposing.length());
                        Log.d("composing", mComposing.toString());
                    }else{//特殊記号のとき

                        //文字コードを文字に変換
                        char code = (char) primaryCode;
                        //if (Character.isLetter(code) && caps) {
                            //code = Character.toUpperCase(code);
                        //}
                        mComposing.append(String.valueOf(code));
                        inputConnection.setComposingText(mComposing.toString(), mComposing.length());
                    }
            }//switch

            //候補ビューの表示の処理
            if(candidateView!=null&&mComposing.length()>0){//未確定文字があるとき
                dictionary.updateCandidateList(mComposing.toString(), candidateView);
                //setCandidatesViewShown(candidateView.size()>0);

            }else{//未確定の文字がないときは候補ビューは表示しない
                setCandidatesViewShown(false);
            }
        }

    }

    @Override
    public void onText(CharSequence charSequence) {

    }

    @Override
    public void swipeLeft() {
        Log.d("swipe", "ok");
    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

}
