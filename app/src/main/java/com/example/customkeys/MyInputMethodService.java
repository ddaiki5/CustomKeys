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

    //private ClipboardManager clipboardManager;
    //private Selection selection;

    private boolean caps = false;
    private boolean isSelectionMode = false;

    private final Handler longPressHandler = new Handler();
    private final Runnable longPressReceiver = new Runnable() {
        @Override
        public void run() {
            isLongPress = true;
        }
    };
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
//            int start = wordBoundary.first();
//            for (int end = wordBoundary.next();
//                 end != BreakIterator.DONE;
//                 start = end, end = wordBoundary.next()) {
//                Log.d("set", "[" + t.toString().substring(start, end) + "]");
//            }
        }

        //文字のまとまりごとにカーソルを移動
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
            //Log.d("cur", "[" + text.toString().substring(wordBoundary.previous(), wordLimit) + "]");
            return wordLimit;
        }

        //句点のまとまりごとにカーソルを移動
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
                //return BreakIterator.DONE;
            }

            //Log.d("cur", "[" + text.toString().substring(sentenceBoundary.previous(), wordLimit) + "]");
            return wordLimit;
        }
    }

    private Memo memo;
    private int currentStartIndex, currentEndIndex, currentIndex;
    private int keyDirection = 0;//left=1, up=2, right=3, down=4

    private MyCandidateView candidateView = null;
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
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("Hoge", e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String jsonStr = response.body().string();
                    Log.d("Hoge","jsonStr=" + jsonStr);
                    try{
                        //jsonパース
                        JSONArray array = new JSONArray(jsonStr);
                        candidateview.clear();
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                for(int i=0; i< array.length(); i++){
                                    try {
                                        JSONArray s = array.getJSONArray(i);
                                        s = s.getJSONArray(1);
                                        //Log.d("Hoge","jsonStr=" + s.toString());
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
            //view.add("ああ");
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
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        keyboard = new Keyboard(this, R.xml.keys_layout);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        keylist = keyboardView.getKeyboard().getKeys();
        keyboardView.setPreviewEnabled(false);
        keyboardView.setOnTouchListener(new View.OnTouchListener() {//フリック操作のためのリスナー
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                float x = motionEvent.getX();
                float y = motionEvent.getY();

                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK){
                    case MotionEvent.ACTION_DOWN:
                        tapX = x;
                        tapY = y;
                        keyDirection=0;
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        Log.d("onTouch", "move");
                        if(tapX-x>=keyboardView.getWidth()/8){
                            Log.d("TouchDir", "left");
                            keyDirection=1;
                        }else if(tapX-x<=-keyboardView.getHeight()/8){
                            Log.d("TouchDir", "right");
                            keyDirection = 3;
                        }else if(tapY-y<=-keyboardView.getWidth()/8){
                            Log.d("TouchDir", "down");
                            keyDirection=4;
                        }else if(tapY-y>=keyboardView.getHeight()/8){
                            Log.d("TouchDir", "up");
                            keyDirection=2;
                        }else{
                            keyDirection = 0;
                        }
                        return true;
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
        //longPressHandler.postDelayed(longPressReceiver, 1000);
    }

    @Override
    public void onRelease(int i) {
        //longPressHandler.removeCallbacks(longPressReceiver);
        //isLongPress = false;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {

            ExtractedText extractedText = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);


            memo.setText(extractedText.text);

            currentIndex = extractedText.startOffset;
            currentStartIndex = extractedText.startOffset + extractedText.selectionStart;
            currentEndIndex = extractedText.startOffset + extractedText.selectionEnd;

            switch(primaryCode) {
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
                case Keyboard.KEYCODE_SHIFT:
                    caps = !caps;
                    keyboard.setShifted(caps);
                    keyboardView.invalidateAllKeys();
                    break;
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
                case CustomCode.KEYCODE_UNDO:
                    Log.d("UNDO", "push UNDO");
                    //isLongPress = !isLongPress;
                    if (isSelectionMode) {
                        if(keyDirection==2){
                            inputConnection.setSelection(currentStartIndex, memo.getNearestCursorNum(currentStartIndex, memo.LEFT));
                        }else {
                            inputConnection.setSelection(currentStartIndex, memo.getNearestSentenceCursor(currentStartIndex, memo.LEFT));
                        }
                    }else {
                        if(keyDirection==2){
                            inputConnection.setSelection(memo.getNearestCursorNum(currentStartIndex, memo.LEFT), memo.getNearestCursorNum(currentStartIndex, memo.LEFT));
                        }else {
                            inputConnection.setSelection(memo.getNearestSentenceCursor(currentStartIndex, memo.LEFT), memo.getNearestSentenceCursor(currentStartIndex, memo.LEFT));
                        }
                    }
                    break;
                case CustomCode.KEYCODE_REDO:
                    Log.d("REDO", "push REDO");
                    int end = memo.getNearestCursorNum(currentStartIndex, memo.RIGHT);
                    inputConnection.setSelection(end, end);
                    break;
                case CustomCode.KEYCODE_REGION:
                    //選択モードへのフラグを変更
                    if(isSelectionMode){
                        inputConnection.setSelection(currentStartIndex, currentStartIndex);
                    }
                    isSelectionMode = !isSelectionMode;
                    keyboard.setShifted(isSelectionMode);
                    keyboardView.invalidateAllKeys();
                    break;
                case CustomCode.KEYCODE_TAB:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
                    break;
                case CustomCode.KEYCODE_LEFTCURSOR:
                    if(isSelectionMode) {//選択モードのとき
                        if(keyDirection==2){//左にフリックしているとき
                            //句点ごとのカーソル移動

                            inputConnection.setSelection(currentStartIndex, memo.getNearestSentenceCursor(currentStartIndex, memo.LEFT));
                        }else {
                            inputConnection.setSelection(currentStartIndex, currentEndIndex - 1);
                        }
                    }else {
                        if(keyDirection==2){
                            int leftSentenceEnd = memo.getNearestSentenceCursor(currentStartIndex, memo.LEFT);
                            inputConnection.setSelection(leftSentenceEnd, leftSentenceEnd);
                        }else if (isLongPress) {
                            inputConnection.setSelection(0, 0);
                        } else {
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
                case CustomCode.KEYCODE_DIACRITIC:
                    CharSequence diacritic_target = inputConnection.getTextBeforeCursor(1, 0);
                    Log.d("diacritic_target", diacritic_target.toString());
                    if(diacritic_target==null) {
                        break;
                    }
                    String converted_target = CustomCode.convert_diatric(diacritic_target.toString());
                    Log.d("converted_target", converted_target);
                    if(converted_target != null) {
                        inputConnection.deleteSurroundingText(1, 0);
                        inputConnection.commitText(converted_target, 1);
                    }
                    break;
                default :
                    if (primaryCode>=CustomCode.KEYCODE_kana && primaryCode <=CustomCode.KEYCODE_kana_end){
                        mComposing.append(CustomCode.NUM_TO_FIFTY[primaryCode-CustomCode.KEYCODE_kana + keyDirection]);
                        inputConnection.setComposingText(mComposing.toString(), mComposing.length());
                        Log.d("composing", mComposing.toString());
                        //Log.d("composing", inputConnection.compo);
                        //inputConnection.commitText(CustomCode.NUM_TO_FIFTY[primaryCode-CustomCode.KEYCODE_kana + keyDirection], 1);
                    }else{
                        char code = (char) primaryCode;
                        //if (Character.isLetter(code) && caps) {
                            //code = Character.toUpperCase(code);
                        //}
                        mComposing.append(String.valueOf(code));
                        inputConnection.setComposingText(mComposing.toString(), mComposing.length());
                    }

            }
            if(candidateView!=null&&mComposing.length()>0){
                dictionary.updateCandidateList(mComposing.toString(), candidateView);
                //setCandidatesViewShown(candidateView.size()>0);
            }else{
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
