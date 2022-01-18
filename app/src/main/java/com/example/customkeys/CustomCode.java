package com.example.customkeys;

import java.util.HashMap;
import java.util.Map;

public class CustomCode {
    public static final int KEYCODE_COPY = 10000;
    public static final int KEYCODE_CUT = 10001;
    public static final int KEYCODE_PASTE = 10002;
    public static final int KEYCODE_UNDO = 10003;
    public static final int KEYCODE_REDO = 10004;
    public static final int KEYCODE_REGION = 10005;
    public static final int KEYCODE_LEFTCURSOR = 10007;
    public static final int KEYCODE_RIGHTCURSOR = 10008;
    public static final int KEYCODE_TAB = 61;
    public static final int KEYCODE_kana = 10100;
    public static final String[] NUM_TO_FIFTY = {
            "あ", "い", "う", "え", "お",
            "か", "き", "く", "け", "こ",
            "さ", "し", "す", "せ", "そ",
            "た", "ち", "つ", "て", "と",
            "な", "に", "ぬ", "ね", "の",
            "は", "ひ", "ふ", "へ", "ほ",
            "ま", "み", "む", "め", "も",
            "や", "ぃ", "ゆ", "ぇ", "よ",
            "ら", "り", "る", "れ", "ろ",
            "わ", "を", "ん", "", "",
            "が", "ぎ", "ぐ", "げ", "ご",
            "ざ", "じ", "ず", "ぜ", "ぞ",
            "だ", "ぢ", "づ", "で", "ど",
            "ば", "び", "ぶ", "べ", "ぼ",
            "ぱ", "ぴ", "ぷ", "ぺ", "ぽ",
            "ぁ", "ぅ", "ぉ", "っ", "ゃ",
            "ゅ", "ょ"
    };
    public static final Map<String, Integer> FIFTY_TO_NUM = new HashMap<String, Integer>(){
        {
            put("あ", 0); put("い", 1); put("う", 2); put("え", 3); put("お", 4);
            put("か", 5); put("き", 6); put("く", 7); put("け", 8); put("こ", 9);
            put("さ", 10); put("し", 11); put("す", 12); put("せ", 13); put("そ", 14);
            put("た", 15); put("ち", 16); put("つ", 17); put("て", 18); put("と", 19);
            put("な", 20); put("に", 21); put("ぬ", 22); put("ね", 23); put("の", 24);
            put("は", 25); put("ひ", 26); put("ふ", 27); put("へ", 28); put("ほ", 29);
            put("ま", 30); put("み", 31); put("む", 32); put("め", 33); put("も", 34);
            put("や", 35); put("ぃ", 36); put("ゆ", 37); put("ぇ", 38); put("よ", 39);
            put("ら", 40); put("り", 41); put("る", 42); put("れ", 43); put("ろ", 44);
            put("わ", 45); put("を", 46); put("ん", 47);
            put("が", 50); put("ぎ", 51); put("ぐ", 52); put("げ", 53); put("ご", 54);
            put("ざ", 55); put("じ", 56); put("ず", 57); put("ぜ", 58); put("ぞ", 59);
            put("だ", 60); put("ぢ", 61); put("づ", 62); put("で", 63); put("ど", 64);
            put("ば", 65); put("び", 67); put("ぶ", 68); put("べ", 69); put("ぼ", 70);
            put("ぱ", 70); put("ぴ", 71); put("ぷ", 72); put("ぺ", 73); put("ぽ", 74);
            put("ぁ", 75); put("ぅ", 76); put("ぉ", 77); put("っ", 78); put("ゃ", 79);
            put("ゅ", 80); put("ょ", 81);
        }
    };
    //濁点、半濁点、促音への変換テーブル
    public static int CONVERT_TABLE[] = {
            75, 36, 76, 38, 77,
            50, 51, 52, 53, 54,
            55, 56, 57, 58, 59,
            60, 61, 62, 63, 64,
            -1, -1, -1, -1, -1,
            65, 66, 67, 68, 69,
            -1, -1, -1, -1, -1,
            79, 1, 80, 3, 81,
            -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1,
            5, 6, 7, 8, 9,
            10, 11, 12, 13, 14,
            15, 16, 78, 18, 19,
            70, 71, 72, 73, 74,
            25, 26, 27, 28, 29,
            0, 2, 4, 17, 35,
            37, 39
    };
}
