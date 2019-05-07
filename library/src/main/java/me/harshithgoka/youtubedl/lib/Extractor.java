package me.harshithgoka.youtubedl.lib;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.harshithgoka.youtubedl.lib.Utils.Arg;
import me.harshithgoka.youtubedl.lib.Utils.Fun;

import static me.harshithgoka.youtubedl.lib.Utils.Arg.VAL;

public class Extractor {

    public String _VALID_URL = "(?x)^\n" +
            " (\n" +
            "     (?:https?://|//)                                    # http(s):// or protocol-independent URL\n" +
            "     (?:(?:(?:(?:\\w+\\.)?[yY][oO][uU][tT][uU][bB][eE](?:-nocookie)?\\.com/|\n" +
            "        (?:www\\.)?deturl\\.com/www\\.youtube\\.com/|\n" +
            "        (?:www\\.)?pwnyoutube\\.com/|\n" +
            "        (?:www\\.)?hooktube\\.com/|\n" +
            "        (?:www\\.)?yourepeat\\.com/|\n" +
            "        tube\\.majestyc\\.net/|\n" +
            "        youtube\\.googleapis\\.com/)                        # the various hostnames, with wildcard subdomains\n" +
            "     (?:.*?\\#/)?                                          # handle anchor (#/) redirect urls\n" +
            "     (?:                                                  # the various things that can precede the ID:\n" +
            "         (?:(?:v|embed|e)/(?!videoseries))                # v/ or embed/ or e/\n" +
            "         |(?:                                             # or the v= param in all its forms\n" +
            "             (?:(?:watch|movie)(?:_popup)?(?:\\.php)?/?)?  # preceding watch(_popup|.php) or nothing (like /?v=xxxx)\n" +
            "             (?:\\?|\\#!?)                                  # the params delimiter ? or # or #!\n" +
            "             (?:.*?[&;])??                                # any other preceding param (like /?s=tuff&v=xxxx or ?s=tuff&amp;v=V36LpHqtcDY)\n" +
            "             v=\n" +
            "         )\n" +
            "     ))\n" +
            "     |(?:\n" +
            "        youtu\\.be|                                        # just youtu.be/xxxx\n" +
            "        vid\\.plus|                                        # or vid.plus/xxxx\n" +
            "        zwearz\\.com/watch|                                # or zwearz.com/watch/xxxx\n" +
            "     )/\n" +
            "     |(?:www\\.)?cleanvideosearch\\.com/media/action/yt/watch\\?videoId=\n" +
            "     )\n" +
            " )?                                                       # all until now is optional -> you can pass the naked ID\n" +
            " ([0-9A-Za-z_-]{11})                                      # here is it! the YouTube video ID\n" +
            " (?!.*?\\blist=\n" +
            "    (?:\n" +
            "        %(playlist_id)s|                                  # combined list/video URLs are handled by the playlist IE\n" +
            "        WL                                                # WL are handled by the watch later IE\n" +
            "    )\n" +
            " )\n" +
            "                                                 # if we found the ID, everything can follow\n" +
            " $";


    private HashMap<Pair<String, String>, JSInterpreter> player_cache;
    private HttpGetter httpGetter;

    String js_code;
    String func_name;


    public Extractor() {
        player_cache = new HashMap<>();
        httpGetter = new HttpGetter();
    }



    public JSInterpreter parseSigJs(String response) {
        JSInterpreter jsInterpreter = new JSInterpreter(response);
        if(jsInterpreter != null) {
            js_code = response;
        }
        return jsInterpreter;
    }

    String signatureCacheId (String sig) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : sig.split("\\.")) {
            stringBuilder.append(s.length());
            stringBuilder.append(".");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    public String extractSignatureFunction(String video_id, String player_url, String s) {
        Pair<String, String> playerID = new Pair<>(player_url, signatureCacheId(s));

        if (!player_cache.containsKey(playerID)) {
            Pattern playerUrl = Pattern.compile(".*?-(?<id>[a-zA-Z0-9_-]+)(?:/watch_as3|/html5player(?:-new)?|(?:/[a-z]{2,3}_[A-Z]{2})?/base)?\\.(?<ext>[a-z]+)$");

            Matcher matcher = playerUrl.matcher(player_url);
            if (!matcher.find())
                return null;

            String player_id = matcher.group(1);
            String player_type = matcher.group(2);

            try {
                String response = httpGetter.run(player_url);
                JSInterpreter jsInterpreter = parseSigJs(response);
                if (jsInterpreter == null)
                    return null;

                Pattern funcNamePattern = Pattern.compile("([\"\\'])signature\\1\\s*,\\s*(?<sig>[a-zA-Z0-9$]+)\\(");
                Matcher m = funcNamePattern.matcher(response);

                String func_name;
                if (m.find())
                    func_name = m.group(2);
                else {
                    funcNamePattern = Pattern.compile("\\.sig\\|\\|(?<sig>[a-zA-Z0-9$]+)\\(");
                    m = funcNamePattern.matcher(response);
                    if (m.find())
                        func_name = m.group(2);
                    else {
                        funcNamePattern = Pattern.compile("yt\\.akamaized\\.net/\\)\\s*\\|\\|\\s*.*?\\s*c\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*(?:encodeURIComponent\\s*\\()?(?<sig>[a-zA-Z0-9$]+)\\(");
                        m = funcNamePattern.matcher(response) ;
                        if (m.find()){
                            func_name = m.group(1) ;
                        } else {
                            funcNamePattern = Pattern.compile("\\bc\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*(?:encodeURIComponent\\s*\\()?\\s*(?<sig>[a-zA-Z0-9$]+)\\(") ;
                            m = funcNamePattern.matcher(response) ;
                            if (m.find()){
                                func_name = m.group(1) ;
                            } else {
                                funcNamePattern = Pattern.compile("\\bc\\s*&&\\s*d\\.set\\([^,]+\\s*,\\s*\\([^)]*\\)\\s*\\(\\s*([a-zA-Z0-9$]+)\\(") ;
                                m = funcNamePattern.matcher(response) ;
                                if (m.find()){
                                    func_name = m.group(1) ;
                                } else {
                                    return null ;
                                }
                            }
                        }
                    }
                }



                if (!TextUtils.isEmpty(func_name)){
                    this.func_name = func_name;
                    Fun fun = jsInterpreter.extractFunction(func_name);
                    jsInterpreter.setSigFun(fun);

                    player_cache.put(playerID, jsInterpreter);
                } else {
                    return null ;
                }

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        JSInterpreter jsi = player_cache.get(playerID);
        Arg arg = new Arg(s);
        try {
            Arg ret = jsi.callFunction(jsi.getSigFun(), new Arg[]{arg});
            return ret.getString(Arg.VAL);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getSignatureCacheId (String s) {
        // in Python
        // return '.'.join(compat_str(len(part)) for part in example_sig.split('.'))
        StringBuilder stringBuilder = new StringBuilder();
        for (String part : s.split("\\.")) {
            stringBuilder.append(part.length() + ".");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    public String decryptSignature(String s, String video_id, String player_url) {
        String TAG = "decryptSig";

        if (player_url == null)
            return null;

        if (player_url.startsWith("//"))
            player_url = "https:" + player_url;

        if (!player_url.matches("https?://"))
            player_url = "https://www.youtube.com/" + player_url;

        Pair<String, String> player_id = new Pair<> (player_url, getSignatureCacheId(s));

        
        String sig = extractSignatureFunction(video_id, player_url, s);

        Log.d(TAG + "enc", s);
        Log.d(TAG + "dec", sig);
        Log.d(TAG, video_id);
        Log.d(TAG, player_url);

        return sig;
    }

    public String getID (String url) {
        Pattern pattern = Pattern.compile(_VALID_URL);
        Matcher m = pattern.matcher(url);
        if (!m.find()) {
            Log.d("ERR", "Not a valid URL, couldn't get ID");
            Log.d("ERR", url);
        }
        return m.group(2);
    }

    public VideoInfo getFormats(String you_url) {
        String response;
        String video_id = "";
        String thumbnail_url = "";
        String length = "";
        String view_count = "";
        String author = "";
        JSONObject ret = new JSONObject();

        try {
            ret.put("status", false);
            response = httpGetter.run(you_url);
            Pattern ytconf = Pattern.compile("ytplayer.config[ =]*");
            Pattern ytcond_end = Pattern.compile(";[ ]*ytplayer\\.load");
            String json = ytcond_end.split(ytconf.split(response)[1])[0];
            JSONObject ytconfig = new JSONObject(json);
            ret.put("data", ytconfig);
            ret.put("status", true);

            JSONObject args = ytconfig.getJSONObject("args");
            String fmts = args.getString("url_encoded_fmt_stream_map") + "," + args.getString("adaptive_fmts");
            String title = args.optString("title", "videoplayback");
            video_id = args.getString("video_id");
            length = args.optString("length_seconds", "-1");
            view_count = args.optString("view_count", "-1");
            author = args.optString("author", "~");
            thumbnail_url = args.optString("thumbnail_url", "");

            String[] fmts_enc = fmts.split(",");
            List<Format> formats = new ArrayList<>();

            for (String fmt : fmts_enc) {
                Format f = new Format(title);

                Map<String, String> query_pairs = new LinkedHashMap<String, String>();
                String[] pairs = fmt.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx != -1 && idx < pair.length()){
                        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                    }

                }

                String url = query_pairs.get("url");
                if (TextUtils.isEmpty(url)){
                    continue ;
                }
                Set<String> params = query_pairs.keySet();
                if (params.contains("sig"))
                    url += "&signature=" + query_pairs.get("sig");
                else if (params.contains("s")) {
                    Pattern pattern = Pattern.compile("\"assets\":.+?\"js\":\\s*(\"[^\"]+\")");
                    Matcher m = pattern.matcher(response);
                    m.find();
                    String player_url = m.group(1);
                    player_url = new JSONObject("{ \"str\" :" + player_url + "}").getString("str");
                    // < Only for verbose logging >
//                    String player_version = "unknown";
//                    String player_desc = "unknown";
//                    Pattern playerType = Pattern.compile("(html5player-([^/]+?)(?:/html5player(?:-new)?)?\\.js)|((?:www|player)-([^/]+)(?:/[a-z]{2}_[A-Z]{2})?/base\\.js)");
//                    m = playerType.matcher(player_url);
//                    if (!m.find()) {
//                        Log.d("ERR", "Couldn't find Player URL");
//                        Log.d("ERR", response);
//                    }
//                    else {
//                        player_version = m.group();
//                        player_desc = "html5 player " + player_version;
//                    }

                    String encrypted_signature = query_pairs.get("s");
                    String videoID = getID(you_url);

                    String decryptsig = decryptSignature(encrypted_signature, videoID, player_url);
                    url += "&signature=" + decryptsig;

                }

                if (!url.contains("ratebypass")) {
                    url += "&ratebypass=yes";
                }

                f.setUrl(url);

                for (String param : params) {
                    if (param.equals("itag")) {
                        f.setItag(Integer.parseInt(query_pairs.get(param)));
                    }

                    if (param.equals("type")) {
                        f.setType(query_pairs.get(param));
                    }

                    if (param.equals("quality")) {
                        f.setQuality(query_pairs.get(param));
                    }
                }

                formats.add(f);
            }

            return new VideoInfo(video_id, title, length, view_count, author, thumbnail_url, new Timestamp(new Date().getTime()), formats);

        } catch (IllegalStateException | IOException | JSONException e) {
            try {
                ret.put("message", e.toString());
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
            Log.d("Err", e.toString());
        }

        return null;
    }
}