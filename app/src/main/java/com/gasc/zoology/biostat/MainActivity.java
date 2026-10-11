package com.gasc.zoology.biostat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "BiostatProgress";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        webView = findViewById(R.id.webView);
        setupWebView();

        webView.loadUrl("file:///android_asset/index.html");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("file".equals(uri.getScheme()) && uri.getPath() != null && uri.getPath().startsWith("/android_asset/")) {
                    return false;
                }
                // Block or open external URLs in browser
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try { startActivity(intent); } catch (Exception ignored) {}
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        webView.addJavascriptInterface(new NativeBridge(), "Android");
    }

    public class NativeBridge {
        @JavascriptInterface
        public void saveProgress(String key, String value) {
            if (key == null || key.length() > 100) return;
            prefs.edit().putString(key, value != null ? value : "").apply();
        }

        @JavascriptInterface
        public String loadProgress(String key) {
            if (key == null) return "";
            return prefs.getString(key, "");
        }

        @JavascriptInterface
        public void shareApp() {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, "Biostatistics Mastery");
            share.putExtra(Intent.EXTRA_TEXT,
                "Check out Biostatistics Mastery \u2014 an offline app for Zoology students!\n" +
                "Learn biostatistics with biological examples, calculators, quizzes & Tamil support.");
            startActivity(Intent.createChooser(share, "Share via"));
        }

        @JavascriptInterface
        public void openCalculator() {
            Intent i = new Intent(MainActivity.this, CalculatorActivity.class);
            startActivity(i);
        }

        @JavascriptInterface
        public void showToast(String message) {
            if (message == null || message.length() > 200) return;
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
