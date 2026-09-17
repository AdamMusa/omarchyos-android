/*
 * Copyright 2026 OmarchyOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.terminal;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toolbar;

import java.io.File;
import java.io.FileInputStream;

/** Sandboxed viewer for apps generated in the Code workspace. */
public final class ProjectActivity extends Activity {
    private static final String LOCAL_HOST = "app.omarchy.local";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            ProjectManifest project = ProjectManifest.load(
                    this, getIntent().getStringExtra("kind"), getIntent().getStringExtra("path"));
            if ("app".equals(project.kind)) {
                showApp(project);
            } else {
                showStagedProject(project);
            }
        } catch (Exception exception) {
            finish();
        }
    }

    private Toolbar toolbar(String title) {
        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(getColor(R.color.code_text));
        toolbar.setSubtitle("Omarchy Code");
        toolbar.setSubtitleTextColor(getColor(R.color.code_text_secondary));
        toolbar.setBackgroundColor(getColor(R.color.code_surface));
        int backIcon = getResources().getIdentifier(
                "ic_ab_back_material", "drawable", "android");
        if (backIcon != 0) {
            toolbar.setNavigationIcon(backIcon);
        }
        toolbar.setNavigationContentDescription(
                com.android.internal.R.string.action_bar_up_description);
        toolbar.setNavigationOnClickListener(view -> finish());
        toolbar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));
        return toolbar;
    }

    private LinearLayout root(String title) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(getColor(R.color.code_background));
        root.addView(toolbar(title));
        return root;
    }

    private void showApp(ProjectManifest project) throws Exception {
        LinearLayout root = root(project.name);
        WebView webView = new WebView(this);
        webView.setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        webView.setWebViewClient(new LocalProjectClient(project.directory));
        root.addView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        String html = ProjectManifest.readText(project.entry, 2 * 1024 * 1024);
        webView.loadDataWithBaseURL(
                "https://" + LOCAL_HOST + "/", html, "text/html", "UTF-8", null);
    }

    private void showStagedProject(ProjectManifest project) {
        LinearLayout root = root(project.name);
        TextView status = new TextView(this);
        status.setPadding(dp(24), dp(30), dp(24), dp(24));
        status.setTextColor(getColor(R.color.code_text));
        status.setTextSize(18);
        status.setText(project.name + " is staged as a " + project.kind
                + " project. Privileged OS installation still requires review, signing, and an image build.");
        root.addView(status, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(root);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class LocalProjectClient extends WebViewClient {
        private final File mDirectory;

        LocalProjectClient(File directory) {
            mDirectory = directory;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (!"https".equals(request.getUrl().getScheme())
                    || !LOCAL_HOST.equals(request.getUrl().getHost())) {
                return super.shouldInterceptRequest(view, request);
            }
            try {
                String relative = request.getUrl().getPath();
                relative = relative == null ? "" : relative.replaceFirst("^/", "");
                File target = new File(mDirectory, relative).getCanonicalFile();
                if (!target.isFile() || !ProjectManifest.isWithin(mDirectory, target)) {
                    return new WebResourceResponse("text/plain", "UTF-8", 404,
                            "Not Found", null, null);
                }
                String extension = MimeTypeMap.getFileExtensionFromUrl(target.getName());
                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                return new WebResourceResponse(
                        mime == null ? "application/octet-stream" : mime,
                        "UTF-8", new FileInputStream(target));
            } catch (Exception exception) {
                return new WebResourceResponse("text/plain", "UTF-8", 404,
                        "Not Found", null, null);
            }
        }
    }
}
