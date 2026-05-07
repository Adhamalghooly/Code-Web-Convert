package com.adhamalghouli.projectmanagement;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.getcapacitor.BridgeActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends BridgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = getBridge().getWebView();
        webView.addJavascriptInterface(new PrintBridge(webView, this), "AndroidPrint");
        webView.addJavascriptInterface(new DownloadBridge(this), "AndroidDownload");
    }

    // ── Print Bridge: استدعاء نظام الطباعة في Android ──
    private static class PrintBridge {
        private final WebView webView;
        private final Context context;

        PrintBridge(WebView webView, Context context) {
            this.webView = webView;
            this.context = context;
        }

        @JavascriptInterface
        public void print(final String jobName) {
            ((android.app.Activity) context).runOnUiThread(() -> {
                try {
                    PrintManager printManager =
                        (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
                    String name = (jobName != null && !jobName.isEmpty())
                        ? jobName : "تقرير المشاريع";
                    PrintDocumentAdapter adapter =
                        webView.createPrintDocumentAdapter(name);
                    printManager.print(name, adapter,
                        new PrintAttributes.Builder().build());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @JavascriptInterface
        public boolean isAvailable() {
            return true;
        }
    }

    // ── Download Bridge: حفظ ملفات Excel إلى مجلد التنزيلات ──
    private static class DownloadBridge {
        private final Context context;

        DownloadBridge(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public boolean isAvailable() {
            return true;
        }

        @JavascriptInterface
        public void saveFile(final String base64Data, final String filename, final String mimeType) {
            ((android.app.Activity) context).runOnUiThread(() -> {
                try {
                    byte[] bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+ (API 29+): MediaStore API — لا يحتاج صلاحية كتابة
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                        values.put(MediaStore.Downloads.MIME_TYPE,
                            mimeType != null ? mimeType :
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                        values.put(MediaStore.Downloads.IS_PENDING, 1);

                        Uri uri = context.getContentResolver().insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                        if (uri != null) {
                            try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                                if (os != null) os.write(bytes);
                            }
                            values.clear();
                            values.put(MediaStore.Downloads.IS_PENDING, 0);
                            context.getContentResolver().update(uri, values, null, null);

                            Toast.makeText(context,
                                "✅ تم حفظ الملف في التنزيلات: " + filename,
                                Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Android 9 وأقدم: حفظ في مجلد Downloads العام
                        File downloadsDir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);
                        if (!downloadsDir.exists()) downloadsDir.mkdirs();
                        File file = new File(downloadsDir, filename);
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            fos.write(bytes);
                        }
                        Toast.makeText(context,
                            "✅ تم حفظ الملف في التنزيلات: " + filename,
                            Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context,
                        "❌ خطأ في حفظ الملف: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
