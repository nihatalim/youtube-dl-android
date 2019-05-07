package me.harshithgoka.youtubedl.lib.service;

import android.os.AsyncTask;
import java.util.regex.Pattern;
import me.harshithgoka.youtubedl.lib.Extractor;
import me.harshithgoka.youtubedl.lib.VideoInfo;

public class Downloader extends AsyncTask<Void, Void, VideoInfo>{
    private Extractor extractor;
    private Pattern youtubeUrlPattern;
    private DownloaderCallback callback;
    private String url;

    public Downloader(String url, DownloaderCallback callback) {
        this.url = url;
        this.extractor = new Extractor();
        this.youtubeUrlPattern = Pattern.compile(extractor._VALID_URL);
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        url = preprocess(url);
        java.util.regex.Matcher m = youtubeUrlPattern.matcher(url);

        if (!m.find()) {
            this.callback.run(null, new ProcessInfo("URL pattern couldn't matched.", false));
            this.cancel(true);
        }
    }

    @Override
    protected VideoInfo doInBackground(Void... voids) {
        return extractor.getFormats(this.url);
    }

    @Override
    protected void onPostExecute(VideoInfo videoInfo) {
        if (videoInfo != null) {
            this.callback.run(videoInfo, new ProcessInfo("Successful.", true));
        }
        else {
            this.callback.run(null, new ProcessInfo("VideoInfo object returned as null.", false ));
        }
    }

    private static String preprocess (String s) {
        int index = s.lastIndexOf("#");
        if (index > 0) {
            s = s.substring(0, index);
        }

        s = s.replaceFirst("m.youtube.com", "www.youtube.com");
        s = s.replaceFirst("&.*", "");

        return s;
    }

    public class ProcessInfo{
        public String info;
        public boolean status;
        ProcessInfo(String info, boolean status){}
    }
}
