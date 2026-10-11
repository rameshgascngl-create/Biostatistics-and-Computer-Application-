package com.gasc.zoology.biostat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "BiostatProgress";
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

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        webView = findViewById(R.id.webView);
        setupWebView();
        setupBackHandler();

        webView.loadUrl("file:///android_asset/index.html");
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
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try { startActivity(intent); } catch (Exception ignored) {}
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
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            String json = sb.toString().replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n");
            webView.evaluateJavascript("if(typeof importProgressFromNative==='function')importProgressFromNative('" + json + "');", null);
            Toast.makeText(this, "Import started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeExport(Uri uri, String data) {
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                os.write(data.getBytes());
                Toast.makeText(this, "Exported successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
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
            share.putExtra(Intent.EXTRA_SUBJECT, "Biostatistics Mastery");
            share.putExtra(Intent.EXTRA_TEXT,
                "Check out Biostatistics Mastery \u2014 an offline app for Zoology students!\n" +
                "Learn biostatistics with biological examples, calculators, quizzes & Tamil support.");
            startActivity(Intent.createChooser(share, "Share via"));
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
            if (jsonData == null || jsonData.length() > 500000) return;
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
