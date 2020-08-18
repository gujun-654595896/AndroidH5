package com.gujun.androidh5

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initWebView()
        btn1.setOnClickListener { login1Click(it) }
        btn2.setOnClickListener { login2Click(it) }
        btn3.setOnClickListener { login3Click(it) }
        btn4.setOnClickListener { login4Click(it) }
    }


    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun initWebView() {
        var settings = webView.settings
        //设置了这个属性后我们才能在 WebView 里与我们的 Js 代码进行交互
        //Lint 会警告Using setJavaScriptEnabled can introduce XSS vulnerabilities into your application, review carefully.
        //在方法或类级别上方写入@SuppressLint("SetJavaScriptEnabled")来抑制此警告。
        settings.javaScriptEnabled = true

        webView.webViewClient = WebViewClient()
        //Lint 会警告WebView.addJavascriptInterface should not be called with minSdkVersion < 17 for security
        //reasons: JavaScript can use reflection to manipulate application
        //"tag"与H5中定义的window.tag.xxx();保持一致
        webView.addJavascriptInterface(JsInterface(), "tag")
        //加载assets文件夹下的html
        webView.loadUrl("file:///android_asset/index.html")
        webView.webViewClient = webViewClient
        webView.webChromeClient = webChromeClient

    }

    /**
     * 无参无返回值的调用H5
     */
    private fun login1Click(v: View) {
        webView.loadUrl("javascript:show1()")
    }

    /**
     * 无参有返回值的调用H5
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun login2Click(v: View) {
        webView.evaluateJavascript("javascript:show()") {
            Toast.makeText(
                this,
                it,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 有参无返回值的调用H5
     */
    private fun login3Click(v: View) {
        val a = "我是Android给H5的参数"
        webView.loadUrl("javascript:showMsg('$a')")
    }

    /**
     * 有参有返回值的调用H5,安卓4.4以上才能用这个方法
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun login4Click(v: View) {
        val a = 1
        val b = 3
        webView.evaluateJavascript("javascript:sum($a,$b)") {
            Toast.makeText(
                this,
                "$a+$b=$it",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    class JsInterface {

        /**
         * H5有参调用Android
         */
        @JavascriptInterface
        public fun h5GetBack(num: String): String {
            return "参数：$num,返回值：456"
        }

        /**
         * H5无参调用Android
         */
        @JavascriptInterface
        public fun h5Get() {
            println("H5调用了我。。。")
        }
    }

    private val webViewClient = object : WebViewClient() {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            //此方法在24及以上有效
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                doShouldOverrideUrlLoading(view, request?.url?.toString() ?: "")
            } else {
                super.shouldOverrideUrlLoading(view, request)
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            //此方法在24及以上失效
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                doShouldOverrideUrlLoading(view, url)
            } else {
                super.shouldOverrideUrlLoading(view, url)
            }
        }
    }

    /**
     * 通过shouldOverrideUrlLoading方法H5通知Android进行交互
     */
    fun doShouldOverrideUrlLoading(
        view: WebView?,
        url: String?
    ): Boolean {
        //返回值：
        // false代表由WebView处理该url，即用WebView加载该url
        // true代表由应用的代码处理该url，WebView不处理，也就是程序员自己做处理。
        return doData(url)
    }

    private val webChromeClient = object : WebChromeClient() {
        /**
         * 弹出警告框
         */
        override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            val back = doData(message)
            result?.confirm()
            //返回值：
            // false代表H5弹对应的框
            // true代表H5不弹对应的框，由Android处理，result?.confirm()是关键
            return back
        }

        /**
         * 弹出确认框
         */
        override fun onJsConfirm(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            val back = doData(message)
            //是确认还是取消
            result?.confirm()
//            result?.cancel()
            //返回值：
            // false代表H5弹对应的框
            // true代表H5不弹对应的框，由Android处理，result?.confirm()是关键
            return back
        }

        /**
         * 弹出输入框
         */
        override fun onJsPrompt(
            view: WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?
        ): Boolean {
            val back = doData(message)
            result?.confirm("我是Android返回的结果")
            //返回值：
            // false代表H5弹对应的框
            // true代表H5不弹对应的框，由Android处理，result?.confirm()是关键
            return back
        }
    }

    fun doData(message: String?): Boolean {
        if (TextUtils.isEmpty(message)) return false
        ///*约定的url协议为：test://webview?arg1=111&arg2=222*/
        //解析url
        val uri: Uri = Uri.parse(message)
        if (uri.scheme.equals("test")) {
            if (uri.authority.equals("webview")) {
                // 可以在协议上带有参数并传递到Android上
                val arg1 = uri.getQueryParameter("arg1")
                val arg2 = uri.getQueryParameter("arg2")
                Toast.makeText(this, "arg1=$arg1,arg2=$arg2", Toast.LENGTH_LONG).show()
                return true
            }
        }
        return false
    }
}