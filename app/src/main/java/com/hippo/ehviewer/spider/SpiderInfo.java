/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.spider;

import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.NumberUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class SpiderInfo {
    //下载时所需要的信息
    private static final String TAG = SpiderInfo.class.getSimpleName();

    private static final String VERSION_STR = "VERSION";
    private static final int VERSION = 2;

    static final String TOKEN_FAILED = "failed";

    public int startPage = 0;
    public long gid = -1;
    public String token = null;
    public int pages = -1;
    public int previewPages = -1;
    public int previewPerPage = -1;
    public SparseArray<String> pTokenMap = null;

    public static SpiderInfo read(@Nullable UniFile file) {
        if (file == null) {
            return null;
        }

        InputStream is = null;
        try {
            is = file.openInputStream();
            //重载，进入另一个自定义read
            return read(is);
        } catch (IOException e) {
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private static int getStartPage(String str) {
        if (null == str) {
            return 0;
        }

        int startPage = 0;
        for (int i = 0, n = str.length(); i < n; i++) {
            startPage *= 16;
            char ch = str.charAt(i);
            if (ch >= '0' && ch <= '9') {
                startPage += ch - '0';
            } else if (ch >= 'a' && ch <= 'f') {
                startPage += ch - 'a' + 10;
            }
        }

        return startPage >= 0 ? startPage : 0;
    }

    private static int getVersion(String str) {
        if (null == str) {
            return -1;
        }
        // VERSION_STR = "VERSION"
        if (str.startsWith(VERSION_STR)) {
            // 如果没有办法parseInt，则返回defaultValue -1
            return NumberUtils.parseIntSafely(str.substring(VERSION_STR.length()), -1);
        } else {
            return 1;
        }
    }

    @Nullable
    @SuppressWarnings("InfiniteLoopStatement")
    public static SpiderInfo read(@Nullable InputStream is) {
        if (null == is) {
            return null;
        }

        SpiderInfo spiderInfo = null;
        try {
            // SpiderInfo类为下载信息
            spiderInfo = new SpiderInfo();
            // Get version
            // 在Hippo的万事屋里，返回String，Returns the ASCII characters up to but not including the next "\r\n", or "\n".
            // 正常的inputStream.read()函数返回一个int， 并且是读取"\r\n", or "\n"的
            // IOUtils.readAsciiLine会跳过这些符号，并且返回一个组合完成的String
            String line = IOUtils.readAsciiLine(is);
            // 如果有错误返回-1，没有返回1
            int version = getVersion(line);
            // VERSION = 2
            if (version == VERSION) {
                // Read next line
                line = IOUtils.readAsciiLine(is);
            } else if (version == 1) {
                // pass
            } else {
                // Invalid version
                return null;
            }
            // Start page
            spiderInfo.startPage = getStartPage(line);
            // Gid
            spiderInfo.gid = Long.parseLong(IOUtils.readAsciiLine(is));
            // Token
            spiderInfo.token = IOUtils.readAsciiLine(is);
            // Deprecated, mode, skip it
            IOUtils.readAsciiLine(is);
            // Preview pages
            spiderInfo.previewPages = Integer.parseInt(IOUtils.readAsciiLine(is));
            // Preview pre page
            line = IOUtils.readAsciiLine(is);
            if (version == 1) {
                // Skip it
            } else {
                spiderInfo.previewPerPage = Integer.parseInt(line);
            }
            // Pages
            spiderInfo.pages = Integer.parseInt(IOUtils.readAsciiLine(is));
            // Check pages
            if (spiderInfo.pages <= 0) {
                return null;
            }
            // PToken
            // PTokenMap为SparseArray<String>
            spiderInfo.pTokenMap = new SparseArray<>(spiderInfo.pages);
            while (true) { // EOFException will raise
                line = IOUtils.readAsciiLine(is);
                int pos = line.indexOf(" ");
                if (pos > 0) {
                    int index = Integer.parseInt(line.substring(0, pos));
                    String pToken = line.substring(pos + 1);
                    if (!TextUtils.isEmpty(pToken)) {
                        spiderInfo.pTokenMap.put(index, pToken);
                    }
                } else {
                    Log.e(TAG, "Can't parse index and pToken, index = " + pos);
                }
            }
        } catch (IOException | NumberFormatException e) {
            // Ignore
        }

        if (spiderInfo == null || spiderInfo.gid == -1 || spiderInfo.token == null ||
                spiderInfo.pages == -1 || spiderInfo.pTokenMap == null) {
            return null;
        } else {
            return spiderInfo;
        }
    }

    public void write(@NonNull OutputStream os) {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(os);
            writer.write(VERSION_STR);
            writer.write(Integer.toString(VERSION));
            writer.write("\n");
            writer.write(String.format("%08x", startPage >= 0 ? startPage : 0)); // Avoid negative
            writer.write("\n");
            writer.write(Long.toString(gid));
            writer.write("\n");
            writer.write(token);
            writer.write("\n");
            writer.write("1");
            writer.write("\n");
            writer.write(Integer.toString(previewPages));
            writer.write("\n");
            writer.write(Integer.toString(previewPerPage));
            writer.write("\n");
            writer.write(Integer.toString(pages));
            writer.write("\n");
            for (int i = 0; i < pTokenMap.size(); i++) {
                Integer key = pTokenMap.keyAt(i);
                String value = pTokenMap.valueAt(i);
                if (TOKEN_FAILED.equals(value) || TextUtils.isEmpty(value)) {
                    continue;
                }
                writer.write(Integer.toString(key));
                writer.write(" ");
                writer.write(value);
                writer.write("\n");
            }
            writer.flush();
        } catch (IOException e) {
            // Ignore
        } finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(os);
        }
    }
}
