package me.harshithgoka.youtubedl.lib.service;

import me.harshithgoka.youtubedl.lib.VideoInfo;

public interface DownloaderCallback {
    void run(VideoInfo videoInfo, Downloader.ProcessInfo processInfo);
}
