package com.example.eagler

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class MainActivity : Activity() {

    // ===== SETTINGS YOU CAN CHANGE =====
    private val RESOLUTION = 0.7f   // 0.5 = fastest/blurry, 1.0 = sharpest/slowest
    private val LOOK_SENS = 1.6f    // camera swipe speed
    // ===================================

    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private var dp = 1f

    private val helperJs = """
(function(){
 if(window.__ec) return; window.__ec=1;
 var locked=false;
 function cv(){return document.querySelector('canvas')||document.body;}
 try{Object.defineProperty(window,'devicePixelRatio',{get:function(){return ${RESOLUTION};}});}catch(e){}
 HTMLElement.prototype.requestPointerLock=function(){locked=true;document.dispatchEvent(new Event('pointerlockchange'));};
 document.exitPointerLock=function(){locked=false;document.dispatchEvent(new Event('pointerlockchange'));};
 try{Object.defineProperty(document,'pointerLockElement',{get:function(){return locked?cv():null;}});}catch(e){}
 window.__k=function(code,key,kc,down){
  var e=new KeyboardEvent(down?'keydown':'keyup',{key:key,code:code,bubbles:true,cancelable:true});
  try{Object.defineProperty(e,'keyCode',{get:function(){return kc;}});Object.defineProperty(e,'which',{get:function(){return kc;}});}catch(x){}
  cv().dispatchEvent(e);
 };
 window.__b=function(btn,down){
  var e=new MouseEvent(down?'mousedown':'mouseup',{button:btn,buttons:down?(btn==0?1:2):0,bubbles:true,cancelable:true,clientX:innerWidth/2,clientY:innerHeight/2});
  cv().dispatchEvent(e);
 };
 window.__m=function(dx,dy,x,y){
  var o={bubbles:true,cancelable:true,clientX:x,clientY:y};
  if(locked){o.movementX=dx;o.movementY=dy;}
  cv().dispatchEvent(new MouseEvent('mousemove',o));
 };
 window.__t=function(x,y){
  if(locked) return;
  var o={bubbles:true,cancelable:true,clientX:x,clientY:y,button:0,buttons:1};
  var c=cv();
  c.dispatchEvent(new MouseEvent('mousemove',o));
  c.dispatchEvent(new MouseEvent('mousedown',o));
  o.buttons=0;
  c.dispatchEvent(new MouseEvent('mouseup',o));
  c.dispatchEvent(new MouseEvent('click',o));
 };
})();
"""

    private fun js(s: String) = web.evaluateJavascript(s, null)
    private fun key(code: String, k: String, kc: Int, down: Boolean) = js("__k('$code','$k',$kc,$down)")
    private fun mouse(btn: Int, down: Boolean) = js("__b($btn,$down)")

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dp = resources.displayMetrics.density
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        root = FrameLayout(this)
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        web.webChromeClient = WebChromeClient()

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(web, helperJs, setOf("*"))
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(r.url)

            override fun onPageStarted(v: WebView, url: String?, f: android.graphics.Bitmap?) {
                js(helperJs)
            }
            override fun onPageFinished(v: WebView, url: String?) {
                js(helperJs)
            }
        }

        try {
            assets.open("index.html").close()
        } catch (e: Exception) {
            Toast.makeText(this, "index.html not found in assets folder!", Toast.LENGTH_LONG).show()
        }
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        buildControls()
    }

    private fun d(v: Int) = (v * dp).toInt()

    private fun makeBtn(label: String, size: Int, down: () -> Unit, up: () -> Unit): TextView {
        val t = TextView(this)
        t.text = label
        t.setTextColor(Color.WHITE)
        t.textSize = 14f
        t.gravity = Gravity.CENTER
        val bg = GradientDrawable()
        bg.setColor(Color.argb(110, 90, 90, 90))
        bg.setStroke(d(2), Color.argb(160, 200, 200, 200))
        bg.cornerRadius = d(6).toFloat()
        t.background = bg
        t.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { v.alpha = 0.55f; down() }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.alpha = 1f; up() }
            }
            true
        }
        t.layoutParams = FrameLayout.LayoutParams(d(size), d(size))
        return t
    }

    private fun place(v: View, gravity: Int, l: Int, t: Int, r: Int, b: Int) {
        val lp = v.layoutParams as FrameLayout.LayoutParams
        lp.gravity = gravity
        lp.setMargins(d(l), d(t), d(r), d(b))
        root.addView(v, lp)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildControls() {
        // ---- Look / tap area (full screen, under the buttons) ----
        val look = View(this)
        var lx = 0f; var ly = 0f; var sx = 0f; var sy = 0f; var t0 = 0L
        look.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { lx = e.x; ly = e.y; sx = e.x; sy = e.y; t0 = e.eventTime }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.x - lx) * LOOK_SENS / dp * 1f
                    val dy = (e.y - ly) * LOOK_SENS / dp * 1f
                    lx = e.x; ly = e.y
                    js("__m(${dx},${dy},${e.x / dp},${e.y / dp})")
                }
                MotionEvent.ACTION_UP -> {
                    val moved = Math.hypot((e.x - sx).toDouble(), (e.y - sy).toDouble())
                    if (moved < d(12) && e.eventTime - t0 < 300) js("__t(${e.x / dp},${e.y / dp})")
                }
            }
            true
        }
        root.addView(look, FrameLayout.LayoutParams(-1, -1))

        val BL = Gravity.BOTTOM or Gravity.START
        val BR = Gravity.BOTTOM or Gravity.END
        val TR = Gravity.TOP or Gravity.END

        // ---- D-pad (bottom left) ----
        val s = 58
        place(makeBtn("▲", s, { key("KeyW", "w", 87, true) }, { key("KeyW", "w", 87, false) }), BL, 16 + s, 0, 0, 16 + 2 * s)
        place(makeBtn("◀", s, { key("KeyA", "a", 65, true) }, { key("KeyA", "a", 65, false) }), BL, 16, 0, 0, 16 + s)
        place(makeBtn("▶", s, { key("KeyD", "d", 68, true) }, { key("KeyD", "d", 68, false) }), BL, 16 + 2 * s, 0, 0, 16 + s)
        place(makeBtn("▼", s, { key("KeyS", "s", 83, true) }, { key("KeyS", "s", 83, false) }), BL, 16 + s, 0, 0, 16)

        // sprint toggle (center of D-pad)
        var run = false
        val runBtn = makeBtn("RUN", s, {}, {})
        runBtn.setOnTouchListener { v, e ->
            if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                run = !run
                v.alpha = if (run) 0.55f else 1f
                key("ControlLeft", "Control", 17, run)
            }
            true
        }
        place(runBtn, BL, 16 + s, 0, 0, 16 + s)

        // ---- Right side buttons ----
        place(makeBtn("HIT", 76, { mouse(0, true) }, { mouse(0, false) }), BR, 0, 0, 120, 60)
        place(makeBtn("JUMP", 76, { key("Space", " ", 32, true) }, { key("Space", " ", 32, false) }), BR, 0, 0, 20, 60)
        place(makeBtn("USE", 58, { mouse(2, true) }, { mouse(2, false) }), BR, 0, 0, 130, 150)
        place(makeBtn("SNEAK", 58, { key("ShiftLeft", "Shift", 16, true) }, { key("ShiftLeft", "Shift", 16, false) }), BR, 0, 0, 30, 150)

        // ---- Top right: pause / inventory / camera ----
        place(makeBtn("ESC", 44, { key("Escape", "Escape", 27, true) }, { key("Escape", "Escape", 27, false) }), TR, 0, 8, 8, 0)
        place(makeBtn("INV", 44, { key("KeyE", "e", 69, true) }, { key("KeyE", "e", 69, false) }), TR, 0, 8, 58, 0)
        place(makeBtn("F5", 44, { key("F5", "F5", 116, true) }, { key("F5", "F5", 116, false) }), TR, 0, 8, 108, 0)
        place(makeBtn("CHAT", 44, { key("KeyT", "t", 84, true) }, { key("KeyT", "t", 84, false) }), TR, 0, 8, 158, 0)

        // ---- Hotbar 1-9 (bottom center) ----
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        for (i in 1..9) {
            val kc = 48 + i
            val b = makeBtn("$i", 34, { key("Digit$i", "$i", kc, true) }, { key("Digit$i", "$i", kc, false) })
            row.addView(b, LinearLayout.LayoutParams(d(34), d(34)).also { it.setMargins(d(2), 0, d(2), 0) })
        }
        row.layoutParams = FrameLayout.LayoutParams(-2, -2)
        place(row, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        key("Escape", "Escape", 27, true)
        key("Escape", "Escape", 27, false)
    }
}
