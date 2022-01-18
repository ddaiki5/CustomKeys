package com.example.customkeys;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.text.TextUtils;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard keyboard;

    private ClipboardManager clipboardManager;

    private boolean caps = false;

    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        keyboard = new Keyboard(this, R.xml.keys_layout);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        return keyboardView;
    }

    @Override
    public void onPress(int i) {

    }

    @Override
    public void onRelease(int i) {

    }



    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            switch(primaryCode) {
                case Keyboard.KEYCODE_DELETE :
                    CharSequence selectedText = inputConnection.getSelectedText(0);

                    if (TextUtils.isEmpty(selectedText)) {
                        inputConnection.deleteSurroundingText(1, 0);
                    } else {
                        inputConnection.commitText("", 1);
                    }
                    break;
                case Keyboard.KEYCODE_SHIFT:
                    caps = !caps;
                    keyboard.setShifted(caps);
                    keyboardView.invalidateAllKeys();
                    break;
                case Keyboard.KEYCODE_DONE:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));

                    break;
                case CustomCode.KEYCODE_COPY:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_COPY));
                    //CharSequence textToCopy = inputConnection.getSelectedText(0);
                    //if(textToCopy==null)break;
                   // ClipData clipCopy = ClipData.newPlainText(null, textToCopy.toString());
                    //clipboardManager.setPrimaryClip(clipCopy);
                    break;
                case CustomCode.KEYCODE_CUT:
//                    CharSequence textToCut;
//                    textToCut = inputConnection.getSelectedText(0);
//                    if(textToCut==null)break;
//                    ClipData clipCut = ClipData.newPlainText(null, textToCut.toString());
//                    clipboardManager.setPrimaryClip(clipCut);
//                    //inputConnection.deleteSurroundingText(textToCut.length(), 0);
//                    inputConnection.commitText("", 1);
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CUT));
                    break;
                case CustomCode.KEYCODE_PASTE:
                    //clipboardから文字データを取得
//                    CharSequence textToPaste;
//                    try {
//                        textToPaste = clipboardManager.getPrimaryClip().getItemAt(0).getText();
//                    } catch (Exception e) {
//                        break;
//                    }
//                    inputConnection.setComposingText(textToPaste, 0);
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PASTE));
                    break;
                case CustomCode.KEYCODE_UNDO:
                    break;
                case CustomCode.KEYCODE_REDO:
                    break;
                case CustomCode.KEYCODE_REGION:
                    //inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_));
                    break;
                case CustomCode.KEYCODE_TAB:
                    //inputConnection.commitText("\t", 1);
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB));
                    break;
                case CustomCode.KEYCODE_LEFTCURSOR:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                    break;
                case CustomCode.KEYCODE_RIGHTCURSOR:
                    inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
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
                        inputConnection.commitText(CustomCode.NUM_TO_FIFTY[primaryCode-CustomCode.KEYCODE_kana], 1);
                    }else{
                        char code = (char) primaryCode;
                        if (Character.isLetter(code) && caps) {
                            code = Character.toUpperCase(code);
                        }
                        inputConnection.commitText(String.valueOf(code), 1);
                    }
            }
        }

    }


    @Override
    public void onText(CharSequence charSequence) {

    }

    @Override
    public void swipeLeft() {

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
