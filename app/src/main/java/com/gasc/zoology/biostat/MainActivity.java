package com.gasc.zoology.biostat;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "BiostatProgress";
    private static final int MAX_IMPORT_BYTES = 2_000_000;
    private static final int MAX_EXPORT_CHARS = 2_000_000;
    private ValueCallback<Uri[]> filePathCallback;
    private String pendingExportData;

    private final ActivityResultLauncher<String> importLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) readAndImport(uri);
        });

    private final ActivityResultLauncher<String> exportLauncher =
        registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
            if (uri != null && pendingExportData != null) writeExport(uri, pendingExportData);
            pendingExportData = null;
        });

    private final ActivityResultLauncher<Intent> fileChooserLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri data = result.getData().getData();
                if (data != null) results = new Uri[]{data};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        });

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyWindowInsets();

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        webView = findViewById(R.id.webView);
        setupWebView();
        setupBackHandler();

        webView.loadUrl("file:///android_asset/index.html");
    }

    /**
     * targetSdk 35+ draws edge-to-edge. Pad the root so the WebView sits between
     * the system bars and the display cutout (and above the keyboard).
     */
    private void applyWindowInsets() {
        View root = findViewById(R.id.root);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), root).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), root).setAppearanceLightNavigationBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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
                // Only hand well-known safe schemes to other apps; block intent:, content:, file: etc.
                String scheme = uri.getScheme();
                if ("https".equals(scheme) || "http".equals(scheme) || "mailto".equals(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (ActivityNotFoundException ignored) {
                        // No app can handle this link; stay in the app.
                    }
                }
                return true;
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                // The WebView renderer crashed or was killed: recreate instead of taking the app down.
                if (webView != null) {
                    ((android.view.ViewGroup) webView.getParent()).removeView(webView);
                    webView.destroy();
                    webView = null;
                }
                recreate();
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = params.createIntent();
                try {
                    fileChooserLauncher.launch(intent);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.addJavascriptInterface(new NativeBridge(), "Android");
    }

    private void readAndImport(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new java.io.IOException("no stream");
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = is.read(chunk)) != -1) {
                if (buf.size() + n > MAX_IMPORT_BYTES) {
                    Toast.makeText(this, R.string.toast_import_too_large, Toast.LENGTH_SHORT).show();
                    return;
                }
                buf.write(chunk, 0, n);
            }
            String json = new String(buf.toByteArray(), StandardCharsets.UTF_8);
            // JSONObject.quote() yields a correctly escaped JavaScript string literal.
            String js = "if(typeof importProgressFromNative==='function')importProgressFromNative("
                + JSONObject.quote(json) + ");";
            if (webView != null) webView.evaluateJavascript(js, null);
            Toast.makeText(this, R.string.toast_import_started, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_import_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void writeExport(Uri uri, String data) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                os.write(data.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, R.string.toast_export_ok, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_export_failed, Toast.LENGTH_SHORT).show();
        }
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
            share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
            share.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
            try {
                startActivity(Intent.createChooser(share, getString(R.string.share_chooser)));
            } catch (ActivityNotFoundException ignored) { }
        }

        @JavascriptInterface
        public void showToast(String message) {
            if (message == null || message.length() > 200) return;
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void startImport() {
            runOnUiThread(() -> importLauncher.launch("application/json"));
        }

        @JavascriptInterface
        public void startExport(String jsonData) {
            if (jsonData == null) return;
            if (jsonData.length() > MAX_EXPORT_CHARS) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, R.string.toast_export_too_large, Toast.LENGTH_LONG).show());
                return;
            }
            pendingExportData = jsonData;
            runOnUiThread(() -> exportLauncher.launch("BCA_Progress.json"));
        }

        @JavascriptInterface
        public void startPrint() {
            runOnUiThread(() -> {
                PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);
                if (printManager != null && webView != null) {
                    PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter("Biostatistics");
                    printManager.print("Biostatistics Lesson", adapter, new PrintAttributes.Builder().build());
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
