/*
 * Copyright 2026 OmarchyOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.terminal;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Validated, deliberately small manifest for sandboxed Code projects. */
final class ProjectManifest {
    static final String FILE_NAME = "omarchy.json";
    private static final int MAX_MANIFEST_BYTES = 64 * 1024;

    final File directory;
    final String kind;
    final String name;
    final File entry;

    private ProjectManifest(File directory, String kind, String name, File entry) {
        this.directory = directory;
        this.kind = kind;
        this.name = name;
        this.entry = entry;
    }

    static ProjectManifest load(Context context, String requestedKind, String requestedPath)
            throws IOException, JSONException {
        File workspace = new File(context.getFilesDir(), "workspace").getCanonicalFile();
        File directory = new File(requestedPath).getCanonicalFile();
        if (!directory.isDirectory() || !isWithin(workspace, directory)) {
            throw new IOException("Project must stay inside the Code workspace");
        }

        File manifestFile = new File(directory, FILE_NAME);
        JSONObject json = manifestFile.isFile()
                ? new JSONObject(readText(manifestFile, MAX_MANIFEST_BYTES)) : new JSONObject();
        String kind = json.optString("type", requestedKind).trim();
        if (!requestedKind.equals(kind)) {
            throw new IOException("Project type does not match the publish command");
        }

        String name = json.optString("name", directory.getName()).trim();
        if (name.isEmpty() || name.length() > 40) {
            throw new IOException("Project name must contain 1 to 40 characters");
        }

        File entry = null;
        if ("app".equals(kind)) {
            entry = new File(directory, json.optString("entry", "index.html")).getCanonicalFile();
            if (!entry.isFile() || !isWithin(directory, entry) || entry.length() > 2 * 1024 * 1024) {
                throw new IOException("App entry must be a file inside the project under 2 MB");
            }
        }
        return new ProjectManifest(directory, kind, name, entry);
    }

    static String readText(File file, int limit) throws IOException {
        if (!file.isFile() || file.length() > limit) {
            throw new IOException("File is missing or too large: " + file.getName());
        }
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > limit) {
                    throw new IOException("File is too large: " + file.getName());
                }
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    static boolean isWithin(File parent, File child) {
        String parentPath = parent.getAbsolutePath();
        String childPath = child.getAbsolutePath();
        return childPath.equals(parentPath) || childPath.startsWith(parentPath + File.separator);
    }
}
