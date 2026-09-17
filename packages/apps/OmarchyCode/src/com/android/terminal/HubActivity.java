/*
 * Copyright 2026 OmarchyOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.terminal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Touch-first entry point for the real on-device coding runtimes. */
public final class HubActivity extends Activity {
    private static final String TAG = "OmarchyCode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);
        initializeWorkspace();

        bindSession(R.id.open_codex, TerminalService.SESSION_CODEX);
        bindSession(R.id.open_claude, TerminalService.SESSION_CLAUDE);
        bindSession(R.id.open_workspace, TerminalService.SESSION_SHELL);
    }

    private void bindSession(int viewId, String session) {
        View view = findViewById(viewId);
        view.setOnClickListener(clicked -> startActivity(
                new Intent(this, TerminalActivity.class)
                        .putExtra(TerminalActivity.EXTRA_SESSION, session)));
    }

    private void initializeWorkspace() {
        File home = homeDirectory(this);
        File workspace = workspaceDirectory(this);
        File temp = new File(home, "tmp");
        File shellProfile = new File(home, ".mkshrc");
        File codexSkill = new File(home, ".codex/skills/omarchy-mobile/SKILL.md");
        File claudeSkill = new File(home, ".claude/skills/omarchy-mobile/SKILL.md");

        home.mkdirs();
        workspace.mkdirs();
        temp.mkdirs();
        seedTextIfMissing(shellProfile, "PS1='omarchy $ '\n");
        seedIfMissing(codexSkill);
        seedIfMissing(claudeSkill);
    }

    private void seedTextIfMissing(File destination, String content) {
        if (destination.exists()) {
            return;
        }
        File parent = destination.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs())) {
            Log.e(TAG, "Unable to create directory for " + destination);
            return;
        }

        File temporary = new File(parent, destination.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
            if (!temporary.renameTo(destination)) {
                throw new IOException("Unable to publish seeded file");
            }
        } catch (IOException exception) {
            Log.e(TAG, "Unable to seed " + destination, exception);
            temporary.delete();
        }
    }

    private void seedIfMissing(File destination) {
        if (destination.exists()) {
            return;
        }
        File parent = destination.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs())) {
            Log.e(TAG, "Unable to create skill directory for " + destination);
            return;
        }

        File temporary = new File(parent, destination.getName() + ".tmp");
        try (InputStream input = getResources().openRawResource(R.raw.omarchy_mobile_skill);
             FileOutputStream output = new FileOutputStream(temporary)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.getFD().sync();
            if (!temporary.renameTo(destination)) {
                throw new IOException("Unable to publish seeded skill");
            }
        } catch (IOException exception) {
            Log.e(TAG, "Unable to seed Omarchy mobile skill", exception);
            temporary.delete();
        }
    }

    static File homeDirectory(Context context) {
        return new File(context.getFilesDir(), "home");
    }

    static File workspaceDirectory(Context context) {
        return new File(context.getFilesDir(), "workspace");
    }
}
