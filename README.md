# 編集機能に注目したAndroid端末向けソフトウェアキーボード

## 概要

近年、スマートフォンの普及によりパソコンではなくスマートフォンを用いての文章作成の機械が増えてきている。

しかし、スマートフォンでの文章入力はパソコンでの入力と比べて不便な部分が多数ある。そのうちの一つとして編集能力に注目した。

パソコンではコピーや貼り付けといった編集操作をマウスでの選択以外にショートカットを使うことで行える。
スマートフォンでコピーや貼り付けといった動作を行うためには、指を選択したい箇所までもっていき長押して表示されるポップアップから選択するというのが一般的である。

そこで、スマートフォンでもできるだけ負担を減らしつつ高速に編集操作が可能なキーボードの制作を行う。

以下は主な編集機能
1. 編集操作は指の移動を減らすためにキーボード上のボタンから選択できるようにした。 
2. カーソル操作もキーボード上から行えるようにした。通常の一文字移動に加えて、単語のまとまりごとの移動、一文単位の移動を加えることでキーボードのカーソル操作だけでストレスなく操作できるようにする。
3. 選択モードへの変更ボタンを設置した。選択モード中も上のカーソル操作で選択範囲を選べる。
4. Tabキーを追加した。現在は対応しているアプリは少ないが、対応するアプリが増えると便利だと考えた。

## 今後の改良点

- バグがたくさんあるので直す
- 普通のキーボードとしてストレスがないように実装する
- AndroidのAPI30で非推奨となったクラスを使っている部分があるので書き直す
- カーソル操作を売りにしているが、やはりまだまだ改善点があるので、最適なキー配置や必要な機能、不要な機能の評価を行う
- 大衆向けの機能ではなく、ニッチな分野の専用キーボードにする。例えばプログラミング専用モバイル向けキーボードは需要がそのうちでそう

## 謝辞

本プロジェクトはイノベイティブ総合コミュニケーションデザイン(ICCD)のプロジェクトとして[mochi-pettan](https://github.com/mochi-pettan)と共同で提案したものである。助言をいただいた皆様には感謝申し上げます。

## スライド

!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド1.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド2.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド3.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド4.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド5.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド6.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド7.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド8.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド9.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド10.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド11.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド12.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド13.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド14.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド15.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド16.PNG)
!(https://github.com/ddaiki5/CustomKeys/tree/images/スライド17.PNG)


## 参考

### 実装にあたり参考にした記事、本

Android Studio Reference

- https://developer.android.com/reference

Android全般

- [基礎＆応用力をしっかり育成！Androidアプリ開発の教科書 第2版 Java対応 なんちゃって開発者にならないための実践ハンズオン](https://www.amazon.co.jp/dp/B08WX4K7G4)

基にしたキーボード

- https://www.androidauthority.com/lets-build-custom-keyboard-android-832362/
  
日本語のIMEの実装記事

- https://qiita.com/Kyome/items/e654363bad7f69e6b0fd
- https://qiita.com/Dooteeen/items/5df3b4e9b11f60651c95
- https://kehalife.com/android-studio-keyboard2/
- https://allabout.co.jp/gm/gc/442982/2/
  
テキストの選択

- https://techbooster.org/android/ui/2613/
  
StackOverFlow（主にInputConnection関連)

- https://stackoverflow.com/questions/19177231/android-copy-paste-from-clipboard-manager
- https://stackoverflow.com/questions/20239524/android-ime-set-cursor-position-in-edittext
- https://stackoverflow.com/questions/42219292/how-does-breakiterator-work-in-android
- https://stackoverflow.com/questions/44507838/breakiterator-not-working-correctly-with-chinese-text?noredirect=1&lq=1
- https://stackoverflow.com/questions/41270091/breakiterator-in-android-counts-character-wrongly?noredirect=1&lq=1
  
長押し時の処理について

- http://harumi.sakura.ne.jp/wordpress/2019/05/29/android%E3%81%AE%E3%82%BF%E3%83%83%E3%83%97%E5%87%A6%E7%90%86%E3%81%A8%E9%95%B7%E6%8A%BC%E3%81%97%E3%81%AB%E3%81%A4%E3%81%84%E3%81%A6/
- https://dev.classmethod.jp/articles/android-tas/

BreakIteratorについて

- https://kaorobo.hatenadiary.org/entry/20071128/1196222399

フリック操作について

- https://qiita.com/dev-tatsuya/items/330374ad2749d66ac66b
- https://ohmyenter.com/flick-listener-in-android-dev/
- https://techbooster.org/android/application/715/

候補変換

- https://zenn.dev/shiena/scraps/35b2e1e73e1519
- https://www.google.co.jp/ime/cgiapi.html

HTTP通信とJSON処理

- https://qiita.com/zaburo/items/8e44f80313ec7f795c53
- http://www.dicre.com/android/json.html
- https://github.com/square/okhttp

候補ビューの作成（がちで情報がなかったのでありがたかった）

- https://sites.google.com/site/cobgroupsite/android/programing/ime_2?tmpl=%2Fsystem%2Fapp%2Ftemplates%2Fprint%2F&showPrintDialog=1

StringBuilder

- https://qiita.com/Sirloin/items/2455ee3a8bbbfb447abb
- https://www.milk-island.net/document/java/kihon/i2/

Gradle

- https://blog.goo.ne.jp/odohuran/e/251da7270a2f9c4aae80d181c817cf17









