/*
 * Copyright 2026 OmarchyOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.terminal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;

import java.util.Collections;

/** User-visible gate between an AI-authored workspace and the Android launcher. */
public final class PublishActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String kind = getIntent().getStringExtra("kind");
        String path = getIntent().getStringExtra("path");
        try {
            ProjectManifest project = ProjectManifest.load(this, kind, path);
            confirmPublish(project);
        } catch (Exception exception) {
            new AlertDialog.Builder(this)
                    .setTitle("Cannot publish project")
                    .setMessage(exception.getMessage())
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                    .setOnCancelListener(dialog -> finish())
                    .show();
        }
    }

    private void confirmPublish(ProjectManifest project) {
        String destination = "app".equals(project.kind)
                ? "the launcher as a sandboxed local app"
                : "Code as a staged " + project.kind + " project";
        new AlertDialog.Builder(this)
                .setTitle("Publish " + project.name + "?")
                .setMessage("This will add the project to " + destination
                        + ". It will not receive privileged phone permissions.")
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                .setPositiveButton("Publish", (dialog, which) -> publish(project))
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void publish(ProjectManifest project) {
        ShortcutManager shortcuts = getSystemService(ShortcutManager.class);
        String id = "project-" + Integer.toHexString(project.directory.getAbsolutePath().hashCode());
        Intent launch = new Intent(this, ProjectActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .putExtra("kind", project.kind)
                .putExtra("path", project.directory.getAbsolutePath());
        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, id)
                .setShortLabel(project.name)
                .setLongLabel(project.name + " • Omarchy Code")
                .setIcon(Icon.createWithResource(this, R.drawable.ic_code))
                .setIntent(launch)
                .build();
        shortcuts.addDynamicShortcuts(Collections.singletonList(shortcut));
        if (shortcuts.isRequestPinShortcutSupported()) {
            shortcuts.requestPinShortcut(shortcut, null);
        }
        finish();
    }
}
