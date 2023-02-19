/*
 * Copyright 2015 Hippo Seven
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

package com.hippo.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.hippo.android.resource.AttrResources;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.LinearDividerItemDecoration;
import com.hippo.ehviewer.R;
import com.hippo.ripple.Ripple;
import com.hippo.yorozuya.LayoutUtils;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DirExplorer extends EasyRecyclerView implements EasyRecyclerView.OnItemClickListener {

    private static final DirFilter DIR_FILTER = new DirFilter();
    private static final FileSort FILE_SORT = new FileSort();

    private static final File PARENT_DIR = null;
    private static final String PARENT_DIR_NAME = "..";

    //mCurrentFile为下载目录
    private File mCurrentFile;
    private final List<File> mFiles = new ArrayList<>();

    private DirAdapter mAdapter;

    private OnChangeDirListener mOnChangeDirListener;

    public DirExplorer(Context context) {
        super(context);
        init(context);
    }

    public DirExplorer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DirExplorer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mAdapter = new DirAdapter();
        setAdapter(mAdapter);
        setLayoutManager(new LinearLayoutManager(context));
        //dp2pix(), dp转pix函数
        //LinearDividerItemDecoration extends RecyclerView.ItemDecoration
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL, AttrResources.getAttrColor(context, R.attr.dividerColor),
                LayoutUtils.dp2pix(context, 1));
        decoration.setShowLastDivider(true);
        addItemDecoration(decoration);
        setSelector(Ripple.generateRippleDrawable(context, !AttrResources.getAttrBoolean(context, R.attr.isLightTheme), new ColorDrawable(Color.TRANSPARENT)));
        setOnItemClickListener(this);

        mCurrentFile = Environment.getExternalStorageDirectory();
        //如果外部储存没有装载，或者mCurrentFile为null
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || mCurrentFile == null) {
            mCurrentFile = new File("/");
        }
        updateFileList();
    }

    public void setOnChangeDirListener(OnChangeDirListener listener) {
        mOnChangeDirListener = listener;
    }

    public void updateFileList() {
        //File可以是一个文件夹，也可以是一个文件
        //mCurrentFile是选择的一个路径，DIR_FILTER过滤出了所有是文件夹的文件
        //File[] files是一个文件夹（路径）集合
        //mCurrentFile = Environment.getExternalStorageDirectory()或者是自定义路径;
        //找出该路径下的所有目录
        File[] files = mCurrentFile.listFiles(DIR_FILTER);

        //mFiles是一个List<File>
        mFiles.clear();

        //把父级目录加进去(文件夹选择菜单， 包含上一级和子目录菜单菜单)
        if (mCurrentFile.getParent() != null) {
            mFiles.add(PARENT_DIR);
        }
        if (files != null) {
            Collections.addAll(mFiles, files);
        }
        // sort
        Collections.sort(mFiles, FILE_SORT);
    }

    public File getCurrentFile() {
        return mCurrentFile;
    }

    public void setCurrentFile(File file) {
        if (file != null && file.isDirectory()) {
            //目前选择的路径
            mCurrentFile = file;
            //更新列表
            updateFileList();
            //通知数据改变
            mAdapter.notifyDataSetChanged();

            if (mOnChangeDirListener != null) {
                mOnChangeDirListener.onChangeDir(mCurrentFile);
            }
        }
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        //从一个list<file>中，获取特定的一个file
        File file = mFiles.get(position);
        if (file == PARENT_DIR) {
            file = mCurrentFile.getParentFile();
        }
        setCurrentFile(file);
        return true;
    }

    private class DirHolder extends ViewHolder {

        public TextView textView;

        public DirHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }

    //DirHolder是里面有一个TextView的ViewHolder
    private class DirAdapter extends Adapter<DirHolder> {

        //创建每个Holder中的内容
        //DirHolder的构造函数为一个TextView，把TextView赋值给DirHolder自带的TextView中去
        @Override
        public DirHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DirHolder( LayoutInflater.from(getContext()).inflate(R.layout.item_dir_explorer, parent, false));
        }


        @Override
        public void onBindViewHolder(DirHolder holder, int position) {
            //滑动到holder之后， 需要对holder进行操作
            File file = mFiles.get(position);
            // 对holder中对textView进行赋值
            // File PARENT_DIR = null;
            // String PARENT_DIR_NAME = "..";
            holder.textView.setText(file == PARENT_DIR ? PARENT_DIR_NAME : file.getName());
        }

        @Override
        public int getItemCount() {
            return mFiles.size();
        }
    }


    static class DirFilter implements FileFilter {
        //检查是否是一个目录
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }

    static class FileSort implements Comparator<File> {
        @Override
        public int compare(File lhs, File rhs) {
            if (lhs == null) {
                return Integer.MIN_VALUE;
            } else if (rhs == null) {
                return Integer.MAX_VALUE;
            } else {
                //按照名称排序
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        }
    }

    public interface OnChangeDirListener {

        void onChangeDir(File dir);
    }
}
