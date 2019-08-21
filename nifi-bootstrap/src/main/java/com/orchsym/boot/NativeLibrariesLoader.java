/*
 * Licensed to the Orchsym Runtime under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * this file to You under the Orchsym License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orchsym.boot;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 
 * @author GU Guoqiang
 *
 */
public final class NativeLibrariesLoader {
    public static final String KEY_LIB_PATH = "java.library.path";

    private static final String OSNAME = System.getProperty("os.name").toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");

    private static boolean isWindows() {
        return OSNAME.startsWith("windows");
    }

    private static boolean isOSX() {
        return OSNAME.startsWith("macosx") || OSNAME.startsWith("osx");
    }

    private static final String PREFIX;
    private static final String[] EXTS;
    static {
        if (isWindows()) {
            PREFIX = "";
            EXTS = new String[] { ".dll", ".drv" };
        } else if (isOSX()) {
            PREFIX = "lib";
            EXTS = new String[] { ".jnilib", ".dylib" };
        } else {
            PREFIX = "lib";
            EXTS = new String[] { ".so" };
        }
    }

    private static final FileFilter EXT_FILTER = new FileFilter() {

        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return false;
            }
            final String name = f.getName();
            for (String ext : EXTS) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }
    };
    private static final FileFilter DIR_FILTER = new FileFilter() {

        @Override
        public boolean accept(File f) {
            return f.isDirectory();
        }
    };

    public static void load(File nativeLibDir) {
        final List<File> nativeLibraries = retrieveNativeLibraries(nativeLibDir);
        for (File lib : nativeLibraries) {
            final String libname = getLibName(lib);
            System.load(lib.getAbsolutePath());
            System.mapLibraryName(libname);
        }
    }

    public static String getLibraryPaths(File nativeLibDir) {
        final String javaLibPath = System.getProperty(KEY_LIB_PATH);

        final List<File> nativeLibraries = retrieveNativeLibraries(nativeLibDir);
        final String nativePaths = nativeLibraries.stream().map(f -> f.getParentFile().getAbsolutePath()).distinct().collect(Collectors.joining(File.pathSeparator));

        if (javaLibPath != null && !nativePaths.isEmpty()) { // if have native libs
            if (javaLibPath == null || javaLibPath.isEmpty()) {
                return nativePaths;
            } else {
                return nativePaths + File.pathSeparator + javaLibPath;
            }
        }
        return null;
    }

    private static List<File> retrieveNativeLibraries(File parentDir) {
        if (!parentDir.exists()) {
            return Collections.emptyList();
        }
        List<File> files = new ArrayList<>();
        final File[] nativeLibs = parentDir.listFiles(EXT_FILTER);
        if (nativeLibs != null && nativeLibs.length > 0) {
            files.addAll(Arrays.asList(nativeLibs));
        }
        final File[] subFolders = parentDir.listFiles(DIR_FILTER);
        if (subFolders != null && subFolders.length > 0) {
            for (File folder : subFolders) {
                files.addAll(retrieveNativeLibraries(folder));
            }
        }
        return files;
    }

    private static String getLibName(File file) {
        final String name = file.getName();
        String libName = name;
        if (!PREFIX.isEmpty() && libName.startsWith(PREFIX)) {
            libName = libName.substring(PREFIX.length());
        }
        for (String ext : EXTS) {
            if (libName.endsWith(ext)) {
                libName = libName.substring(0, libName.length() - ext.length());
                break;
            }
        }
        return libName;
    }
}
