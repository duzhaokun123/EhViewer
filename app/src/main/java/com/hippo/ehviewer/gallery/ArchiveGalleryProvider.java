/*
 * Copyright 2019 Hippo Seven
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

package com.hippo.ehviewer.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.duzhaokun123.galleryview.GalleryPageAdapter;
import com.duzhaokun123.galleryview.GalleryProvider;
import com.hippo.a7zip.ArchiveException;
import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.unifile.UniFile;
import com.hippo.unifile.UniRandomAccessFile;
import com.hippo.util.NaturalComparator;
import com.hippo.yorozuya.thread.PriorityThread;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

public class ArchiveGalleryProvider extends GalleryProvider {

    private static final AtomicInteger sIdGenerator = new AtomicInteger();
    private static final Comparator<A7ZipArchive.A7ZipArchiveEntry> naturalComparator = new Comparator<A7ZipArchive.A7ZipArchiveEntry>() {
        private final NaturalComparator comparator = new NaturalComparator();

        @Override
        public int compare(A7ZipArchive.A7ZipArchiveEntry o1, A7ZipArchive.A7ZipArchiveEntry o2) {
            return comparator.compare(o1.getPath(), o2.getPath());
        }
    };
    private final UniFile file;
    private final Stack<Integer> requests = new Stack<>();
    private final AtomicInteger extractingIndex = new AtomicInteger(GalleryPageAdapter.INVALID_INDEX);
    private final LinkedHashMap<Integer, InputStream> streams = new LinkedHashMap<>();
    private final AtomicInteger decodingIndex = new AtomicInteger(GalleryPageAdapter.INVALID_INDEX);
    private Thread archiveThread;
    private Thread decodeThread;
    private volatile int size = 0;
    private String error;

    public ArchiveGalleryProvider(Context context, Uri uri) {
        file = UniFile.fromMediaUri(context, uri);
    }

    @Override
    public void start() {
        int id = sIdGenerator.incrementAndGet();

        archiveThread = new PriorityThread(
                new ArchiveTask(), "ArchiveTask" + '-' + id, Process.THREAD_PRIORITY_BACKGROUND);
        archiveThread.start();

        decodeThread = new PriorityThread(
                new DecodeTask(), "DecodeTask" + '-' + id, Process.THREAD_PRIORITY_BACKGROUND);
        decodeThread.start();
    }

    @Override
    public void stop() {
        if (archiveThread != null) {
            archiveThread.interrupt();
            archiveThread = null;
        }
        if (decodeThread != null) {
            decodeThread.interrupt();
            decodeThread = null;
        }
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public void request(int index) {
        if (stateOf(index) == PageState.READY) {
            try {
                notifyPageSucceed(index, get(index));
                return;
            } catch (CannotGetException e) {
                e.printStackTrace();
            }
        }
        boolean inDecodeTask;
        synchronized (streams) {
            inDecodeTask = streams.containsKey(index) || index == decodingIndex.get();
        }

        synchronized (requests) {
            boolean inArchiveTask = requests.contains(index) || index == extractingIndex.get();
            if (!inArchiveTask && !inDecodeTask) {
                requests.add(index);
                requests.notify();
            }
        }
        notifyPageWait(index);
    }

    @Override
    public void forceRequest(int index) {
        request(index);
    }

    @Override
    public void cancelRequest(int index) {
        synchronized (requests) {
            requests.remove(Integer.valueOf(index));
        }
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public boolean save(int index, @NonNull UniFile file) {
        // TODO
        return false;
    }

    @Nullable
    @Override
    public UniFile save(int index, @NonNull UniFile dir, @NonNull String filename) {
        // TODO
        return null;
    }

    @NonNull
    @Override
    public String getImageFilenameWithExtension(int index) {
        // TODO
        return Integer.toString(index);
    }

    private class ArchiveTask implements Runnable {
        @Override
        public void run() {
            UniRandomAccessFile uraf = null;
            try {
                uraf = file.createRandomAccessFile("r");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (uraf == null) {
                size = 0;
                error = GetText.getString(R.string.error_reading_failed);
                notifyStateChange(State.ERROR, error);
                return;
            }

            A7ZipArchive archive = null;
            try {
                archive = A7ZipArchive.create(uraf);
            } catch (ArchiveException e) {
                e.printStackTrace();
            }
            if (archive == null) {
                size = 0;
                error = GetText.getString(R.string.error_invalid_archive);
                notifyStateChange(State.ERROR, error);
                return;
            }

            List<A7ZipArchive.A7ZipArchiveEntry> entries = archive.getArchiveEntries();
            entries.sort(naturalComparator);

            // Update size and notify changed
            size = entries.size();
            notifyStateChange(State.READY);

            while (!Thread.currentThread().isInterrupted()) {
                int index;
                synchronized (requests) {
                    if (requests.isEmpty()) {
                        try {
                            requests.wait();
                        } catch (InterruptedException e) {
                            // Interrupted
                            break;
                        }
                        continue;
                    }
                    index = requests.pop();
                    extractingIndex.lazySet(index);
                }

                // Check index valid
                if (index < 0 || index >= entries.size()) {
                    extractingIndex.lazySet(GalleryPageAdapter.INVALID_INDEX);
                    notifyPageFailed(index, GetText.getString(R.string.error_out_of_range));
                    continue;
                }

                Pipe pipe = new Pipe(4 * 1024);

                synchronized (streams) {
                    if (streams.get(index) != null) {
                        continue;
                    }
                    streams.put(index, pipe.getInputStream());
                    streams.notify();
                }

                try {
                    entries.get(index).extract(pipe.getOutputStream());
                } catch (ArchiveException e) {
                    e.printStackTrace();
                } finally {
                    extractingIndex.lazySet(GalleryPageAdapter.INVALID_INDEX);
                }
            }
        }
    }

    private class DecodeTask implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                int index;
                InputStream stream;
                synchronized (streams) {
                    if (streams.isEmpty()) {
                        try {
                            streams.wait();
                        } catch (InterruptedException e) {
                            // Interrupted
                            break;
                        }
                        continue;
                    }

                    Iterator<Map.Entry<Integer, InputStream>> iterator = streams.entrySet().iterator();
                    Map.Entry<Integer, InputStream> entry = iterator.next();
                    iterator.remove();
                    index = entry.getKey();
                    stream = entry.getValue();
                    decodingIndex.lazySet(index);
                }

                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    if (bitmap != null) {
                        notifyPageSucceed(index, bitmap);
                    } else {
                        notifyPageFailed(index, GetText.getString(R.string.error_decoding_failed));
                    }
                } finally {
                    decodingIndex.lazySet(GalleryPageAdapter.INVALID_INDEX);
                }
            }
        }
    }
}
