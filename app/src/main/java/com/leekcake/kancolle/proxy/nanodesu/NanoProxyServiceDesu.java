package com.leekcake.kancolle.proxy.nanodesu;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.leekcake.kancolle.proxy.nanodesu.container.Account;
import com.leekcake.kancolle.proxy.nanodesu.container.Fleet;
import com.leekcake.kancolle.proxy.nanodesu.receiver.AccountReceiver;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import co.kr.be.nanoproxy.NanoProxyServer;
import co.kr.be.nanoproxy.filter.FilterInformation;
import co.kr.be.nanoproxy.filter.SessionFilter;
import co.kr.be.nanoproxy.http.HeaderBuilder;
import co.kr.be.nanoproxy.http.HeaderParser;
import co.kr.be.nanoproxy.http.PostFDParser;
import co.kr.be.nanoproxy.util.Util;

/**
 * Created by fkdlx on 2015-12-05.
 */
public class NanoProxyServiceDesu extends Service {
    private final IBinder mBinder = new LocalBinder(); // 컴포넌트에 반환되는 IBinder
    public class LocalBinder extends Binder {
        NanoProxyServiceDesu getService() {
            return NanoProxyServiceDesu.this;
        }
    }

    NotificationManager nm;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private NanoProxyServer server = new NanoProxyServer();
    private NotificationUpdateThread updateThread = new NotificationUpdateThread();

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder mCompatBuilder = new NotificationCompat.Builder( getApplicationContext() );
            mCompatBuilder.setSmallIcon(R.mipmap.ic_launcher);
            mCompatBuilder.setContentTitle("나노데스!");
            mCompatBuilder.setContentText("NanoProxy is Running :p");
            startForeground(13939, mCompatBuilder.build());

            server.registerFilter( new KancolleFilter() );
            server.Listen(3952);

            updateThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            server.stopListen();
            updateThread.stopNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AccountReceiver receiver = null;
    public final void registerAccountReceiver(AccountReceiver receiver) {
        this.receiver = receiver;
    }

    public final void clearAccounts() {
        accounts.clear();
        if(receiver != null) {
            receiver.onAccountCleared();
        }
    }

    public final void resendAccountsToReceiver() {
        if(receiver != null) {
            receiver.onAccountCleared();
            for(Account account : accounts.values()) {
                receiver.onAccountDetected(account);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateThread.interrupt();
        return super.onStartCommand(intent, flags, startId);
    }

    public final class NotificationUpdateThread extends Thread {
        public boolean isStoping = false;

        private long shortestMissionComplete;

        @Override
        public final void run() {
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
            while( !isStoping ) {
                wl.acquire();
                synchronized (accounts) {
                    for (Account account : accounts.values()) {
                        List<Fleet> fleets = account.getFleets();

                        int workingFleet = 0;
                        int workedFleet = 0;
                        long beforeMissionComplete = shortestMissionComplete;
                        shortestMissionComplete = Long.MAX_VALUE;
                        for (Fleet fleet : fleets) {
                            if (fleet.isPerformingMission) {
                                if (fleet.Mission_finishAt <= System.currentTimeMillis()) {
                                    workedFleet++;
                                } else {
                                    if(shortestMissionComplete > fleet.Mission_finishAt) {
                                        shortestMissionComplete = fleet.Mission_finishAt;
                                    }
                                    workingFleet++;
                                }
                            }
                        }
                        if( beforeMissionComplete != shortestMissionComplete ) {
                            AlarmManager am =( AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                            Intent i = new Intent(getApplicationContext(), NanoProxyServiceDesu.class);
                            PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, i, 0);
                            System.out.println("Alarm setted: " + shortestMissionComplete);
                            am.set(AlarmManager.RTC_WAKEUP, shortestMissionComplete, pi);
                        }

                        NotificationCompat.Builder mCompatBuilder = new NotificationCompat.Builder( getApplicationContext() );
                        mCompatBuilder.setSmallIcon(R.mipmap.ic_launcher);
                        mCompatBuilder.setDefaults(0);
                        if (workedFleet != 0) {
                            mCompatBuilder.setTicker("원정을 완료한 함대가 있습니다!");
                            mCompatBuilder.setLights(Color.CYAN, 5000, 5000);
                            mCompatBuilder.setVibrate(new long[]{500, 500, 1000, 500, 1500, 500});
                            mCompatBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND);
                        }
                        mCompatBuilder.setWhen(System.currentTimeMillis());
                        mCompatBuilder.setNumber(account.getFleets().size());
                        mCompatBuilder.setContentTitle("제독명: " + account.nickName);
                        mCompatBuilder.setContentText("총/원정중/(완료) 함대 수: " + fleets.size() + "/" + workingFleet + "/" + workedFleet);

                        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle(mCompatBuilder);
                        for (Fleet fleet : fleets) {
                            style.addLine(fleet.getLeftTime() + "/" + fleet.name);
                        }
                        style.setSummaryText("총/원정중/(완료) 함대 수: " + fleets.size() + "/" + workingFleet + "/" + workedFleet);
                        mCompatBuilder.setStyle(style);

                        mCompatBuilder.setOngoing(false);
                        mCompatBuilder.setAutoCancel(false);

                        nm.notify(account.tokenID.hashCode(), mCompatBuilder.build());
                    }
                }
                wl.release();
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public final void stopNow() {
            isStoping = true;
            while( isAlive() ) {
                interrupt();
            }
        }
    }

    private static final HashMap<String, String> MIMEs = new HashMap<>();
    static {
        // Bitmap
        MIMEs.put("bm", "image/bmp");
        MIMEs.put("bmp", "image/bmp");
        // JPEG
        MIMEs.put("jpeg", "image/jpeg");
        MIMEs.put("jpg", "image/jpeg");
        // PNG
        MIMEs.put("png", "image/png");
        // GIF
        MIMEs.put("gif", "image/gif");

        MIMEs.put("mp3", "audio/mpeg");
        MIMEs.put("wav", "audio/wav");

        MIMEs.put("avi", "video/x-msvideo");
        MIMEs.put("wmv", "x-ms-wmv");
        MIMEs.put("asf", "video/x-ms-asf");
        MIMEs.put("mp4", "video/mp4");
        MIMEs.put("mpeg", "video/mpeg");
        MIMEs.put("m3u8", "application/x-mpegURL");
        MIMEs.put("3gp", "video/3gpp");
        MIMEs.put("mov", "video/quicktime");

        MIMEs.put("zip", "application/zip");
        MIMEs.put("swf", "application/x-shockwave-flash");

        MIMEs.put("xml", "text/xml");
        MIMEs.put("txt", "text/plain");

        // Java Script
        MIMEs.put("js", "application/x-javascript");
    }

    public final HashMap<String, Account> accounts = new HashMap<>();

    final class KancolleFilter extends SessionFilter {
        private final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

        @Override
        public boolean needRequestPostData(final FilterInformation info) {
            return info.getRequestURL().getPath().startsWith("/kcsapi");
        }

        @Override
        public void onClientGetRequest(final FilterInformation info) {
            checkDMM(info, info.getClientRequestHeader(), info.getServerRequestHeader());
        }

        @Override
        public void onClientPostRequest(final FilterInformation info) {
            checkDMM(info, info.getClientRequestHeader(), info.getServerRequestHeader());
        }

        @Override
        public void onClientPostRequestwithPostData(final FilterInformation info) {
            checkDMM(info, info.getClientRequestHeader(), info.getServerRequestHeader());
        }

        @Override
        public void onServerResponse(final FilterInformation info) {
            checkDMM(info, info.getServerResponseHeader(), info.getClientResponseHeader());
        }

        @Override
        public boolean needResponseBody(final FilterInformation info) {
            // System.out.println("needResponseBody, getPath:" + getRequestURL().getPath());
            return info.getRequestURL().getPath().startsWith("/kcs");
        }

        private final File getCacheFile(final FilterInformation info) {
            return new File(getExternalFilesDir("kancolle_cache"), info.getRequestURL().getPath());
        }

        private final long getLastModifyofFile(final File file) throws IOException {
            final File LM = new File(file.getPath() + ".lm");
            if (!LM.exists())
                return file.lastModified();
            final DataInputStream dis = new DataInputStream(new FileInputStream(LM));
            final long result = dis.readLong();
            dis.close();
            return result;
        }

        private final void setLastModifyofFile(final File file, final long lastModify) throws Exception {
            final File LM = new File(file.getPath() + ".lm");
            final DataOutputStream dos = new DataOutputStream(new FileOutputStream(LM));
            dos.writeLong(lastModify);
            dos.flush();
            dos.close();
        }

        private final String getVersionofFile(final File file) throws IOException {
            final File Version = new File(file.getPath() + ".version");
            if (!Version.exists())
                return null;
            final FileInputStream fis = new FileInputStream(Version);
            final byte[] buf = new byte[fis.available()];
            fis.read(buf);
            fis.close();
            return new String(buf, "UTF-8");
        }

        private final void setVersionofFile(final File file, final String version) throws Exception {
            final File Version = new File(file.getPath() + ".version");
            final DataOutputStream dos = new DataOutputStream(new FileOutputStream(Version));
            dos.write(version.getBytes());
            dos.flush();
            dos.close();
        }

        @Override
        public void onServerResponseBody(final FilterInformation info, final byte[] body) {
            try {
                // API
                if (info.getRequestURL().getPath().startsWith("/kcsapi")) {
                    if(info.postData == null)
                        return;

                    PostFDParser parser = new PostFDParser( URLDecoder.decode(new String(info.postData), "UTF-8") );
                    String tokenID = parser.getFD("api_token");
                    if(tokenID == null)
                        return;

                    Account account;
                    boolean isNew = false;
                    account = accounts.get(tokenID);
                    if(account == null) {
                        account = new Account(tokenID);
                        accounts.put(tokenID, account);
                        isNew = true;
                    }

                    String json = new String(body, "UTF-8");
                    json = json.substring(json.indexOf('{'));
                    final JSONObject obj = (JSONObject) JSONValue.parse(json);
                    System.out.println(info.getRequestURL().getPath());
                    if (info.getRequestURL().getPath().equals("/kcsapi/api_get_member/deck")) {
                        account.parseDeck((JSONArray) obj.get("api_data"));
                    } else if (info.getRequestURL().getPath().equals("/kcsapi/api_port/port")) {
                        account.parsePort(obj);
                    } else if( info.getRequestURL().getPath().equals("/kcsapi/api_get_member/basic") ) {
                        account.nickName = (String) ((JSONObject) obj.get("api_data")).get("api_nickname");
                        System.out.println("setted nickname as " + account.nickName);
                    } else {
                        return;
                    }

                    if(receiver != null) {
                        if (isNew) {
                            receiver.onAccountDetected(account);
                        } else {
                            receiver.onAccountChanged(account);
                        }
                    }
                    updateThread.interrupt();
                } else {
                    if (info.getRequestURL().getPath().contains("mainD2.swf") || info.getRequestURL().getPath().contains("Core.swf"))
                        return;

                    final File file = getCacheFile(info);
                    file.getParentFile().mkdirs();

                    if (info.getClientRequestHeader().getQuery("Version") != null) {
                        setVersionofFile(file, info.getClientRequestHeader().getQuery("Version"));
                    } else if (info.getServerResponseHeader().getHeader("Last-Modified") != null) {
                        setLastModifyofFile(file, format.parse(info.getServerResponseHeader().getHeader("Last-Modified")).getTime());
                    } else
                        return;

                    final FileOutputStream fos = new FileOutputStream(file);
                    final BufferedOutputStream bos = new BufferedOutputStream(fos);
                    bos.write(body);
                    bos.close();
                    fos.close();
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean needResponseOverride(final FilterInformation info) {
            try {
                if (!info.getRequestURL().getPath().startsWith("/kcs"))
                    return false;
                // System.out.println("needResponsePassed: " + getRequestURL().getPath());

                final File cache = getCacheFile(info);
                if (!cache.exists() && !cache.getAbsoluteFile().exists())
                    // System.out.println("Cache not exist: " + cache.getPath());
                    return false;

                final HttpURLConnection connection = (HttpURLConnection) info.getRequestURL().toURL().openConnection();
                connection.setRequestMethod("HEAD");
                final long lastModify = getLastModifyofFile(cache);
                connection.setRequestProperty("If-Modified-Since", format.format(new Date(getLastModifyofFile(cache))));
                connection.setRequestProperty("Referer", info.getClientRequestHeader().getHeader("Referer"));

                final int response = connection.getResponseCode();
                // System.out.println("try catch, result? " + response + " for " + getRequestURL().getPath());
                if (info.getClientRequestHeader().getQuery("Version") != null) {
                    final String version = getVersionofFile(cache);
                    if (version != null && info.getClientRequestHeader().getQuery("Version").equals(version))
                        return true;
                } else if (connection.getHeaderField("Last-Modified") != null) {
                    // System.out.println("LM, " + connection.getHeaderField("Last-Modified") + " =? " + format.format(new Date(lastModify)));
                    // System.out.println("LM, " + format.parse(connection.getHeaderField("Last-Modified")).getTime() + " =? " + lastModify);
                    if (format.parse(connection.getHeaderField("Last-Modified")).getTime() == lastModify)
                        return true;
                } else if (response == 304)
                    return true;
                connection.disconnect();
            } catch (final Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public void onServerResponseOverride(final FilterInformation info, final BufferedOutputStream bos) throws IOException {
            final File cache = getCacheFile(info);
            final long lastModify = getLastModifyofFile(cache);
            final HeaderBuilder builder = new HeaderBuilder();

            if (info.getClientRequestHeader().getHeader("If-Last-Modified") != null) {
                try {
                    final long clientLastModify = format.parse(info.getClientRequestHeader().getHeader("If-Last-Modified")).getTime();
                    if (lastModify <= clientLastModify) {
                        builder.addHeader("HTTP/1.1 304 Not Modified");
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
            if (builder.Headers.size() == 0) {
                builder.addHeader("HTTP/1.1 200 OK");

                builder.addHeader("Content-Length", "" + cache.length());
                String type = "application/octet-stream";
                final String serveName = cache.getName();

                if (serveName.contains(".")) {
                    final String exten = serveName.substring(serveName.lastIndexOf(".") + 1);
                    if (MIMEs.containsKey(exten)) {
                        type = MIMEs.get(exten);
                    }
                }
                builder.addHeader("Content-Type", type);
                builder.addHeader("Last-Modified", format.format(new Date(lastModify)));
            }

            builder.addHeader("Accept-Ranges", "bytes");
            builder.addHeader("Date", format.format(new Date()));
            builder.addHeader("Pragma", "public");
            builder.addHeader("Cache-Control", "public");
            builder.addHeader("Server", "NanoProxy");

            bos.write(builder.getHeader());

            final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(cache));
            Util.copyStream(bis, bos);
            bis.close();
        }

        public final void checkDMM(final FilterInformation info, final HeaderParser parser, final HeaderBuilder builder) {
            final URI URL = info.getRequestURL();
            if (URL.getHost().contains("dmm.com")) {
                try {
                    for (int i = 0; i < builder.Headers.size(); i++) {
                        builder.Headers.set(i, builder.Headers.get(i).replace("ckcy=2", "ckcy=1"));
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
