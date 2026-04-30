package com.example.movieapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URL = "videoUrl";
    public static final String EXTRA_BUNNY_VIDEO_ID = "bunnyVideoId";
    public static final String EXTRA_TITLE = "title";
    private static final List<String> BLOCKED_WEB_URL_PARTS = Arrays.asList(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "pagead2.googlesyndication.com",
            "adservice.google.",
            "adnxs.com",
            "adsystem.com",
            "moatads.com",
            "popads",
            "popcash",
            "propellerads",
            "onclick",
            "taboola",
            "outbrain",
            "scorecardresearch",
            "/ads/",
            "?ads=",
            "&ads=",
            "prebid",
            "vpaid",
            "vast"
    );
    private static final String PLAYER_PREFS = "player_progress";
    private static final String PROGRESS_PREFIX = "progress_";
    private static final String TAG = "PlayerActivity";

    private Button retryButton;
    private ExoPlayer player;
    private LinearLayout errorContainer;
    private PlayerView playerView;
    private ProgressBar loadingSpinner;
    private TextView resizeButton;
    private TextView titleTextView;
    private View topBar;
    private WebView webView;
    private AudioManager audioManager;
    private String allowedEmbedRootHost;
    private String bunnyVideoId;
    private String videoUrl;
    private boolean usingWebEmbed;
    private int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        boostMediaVolume();

        try {
            setContentView(R.layout.activity_player);
        } catch (RuntimeException e) {
            finishWithPlaybackError(e);
            return;
        }

        playerView = findViewById(R.id.playerView);
        if (playerView == null) {
            finishWithPlaybackError();
            return;
        }

        videoUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        bunnyVideoId = getIntent().getStringExtra(EXTRA_BUNNY_VIDEO_ID);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        Log.d(TAG, "Bunny video id=" + bunnyVideoId);
        Log.d(TAG, "Playing url=" + videoUrl);

        bindOverlayViews();
        bindTitle(title);
        setupActions();
        hideSystemUi();

        if (TextUtils.isEmpty(videoUrl)) {
            Toast.makeText(this, "Video not available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player == null && !TextUtils.isEmpty(videoUrl)) {
            if (isDirectVideoUrl(videoUrl)) {
                initializePlayer(videoUrl);
            } else {
                initializeWebEmbed(videoUrl);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boostMediaVolume();
        hideSystemUi();
    }

    @Override
    protected void onStop() {
        saveProgress();
        releasePlayer();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        destroyWebView();
        super.onDestroy();
    }

    private void bindOverlayViews() {
        topBar = findViewById(R.id.player_top_bar);
        titleTextView = findViewById(R.id.player_title);
        resizeButton = findViewById(R.id.player_resize_button);
        loadingSpinner = findViewById(R.id.player_loading_spinner);
        errorContainer = findViewById(R.id.player_error_container);
        retryButton = findViewById(R.id.player_retry_button);
        webView = findViewById(R.id.player_web_view);

        playerView.setResizeMode(resizeMode);
        playerView.setControllerVisibilityListener(new PlayerView.ControllerVisibilityListener() {
            @Override
            public void onVisibilityChanged(int visibility) {
                if (topBar != null) {
                    topBar.setVisibility(visibility);
                }
            }
        });
    }

    private void bindTitle(String title) {
        if (titleTextView == null) {
            return;
        }
        if (TextUtils.isEmpty(title)) {
            titleTextView.setVisibility(View.GONE);
            return;
        }
        titleTextView.setText(title);
        titleTextView.setVisibility(View.VISIBLE);
    }

    private void boostMediaVolume() {
        if (audioManager == null) {
            return;
        }
        try {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
            Log.d(TAG, "Media volume boosted to max=" + maxVolume);
        } catch (RuntimeException error) {
            Log.w(TAG, "Unable to boost media volume", error);
        }
    }

    private void finishWithPlaybackError() {
        releasePlayer();
        Toast.makeText(this, "Unable to play video", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void finishWithPlaybackError(Throwable error) {
        Log.e(TAG, "Playback error", error);
        finishWithPlaybackError();
    }

    private String getProgressKey() {
        if (TextUtils.isEmpty(bunnyVideoId)) {
            return null;
        }
        return PROGRESS_PREFIX + bunnyVideoId;
    }

    private long getSavedProgress() {
        String progressKey = getProgressKey();
        if (progressKey == null) {
            return 0L;
        }
        SharedPreferences preferences = getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE);
        return preferences.getLong(progressKey, 0L);
    }

    private void hideError() {
        if (errorContainer != null) {
            errorContainer.setVisibility(View.GONE);
        }
    }

    private void hideSystemUi() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void initializeWebEmbed(String embedUrl) {
        usingWebEmbed = true;
        allowedEmbedRootHost = getRootHost(Uri.parse(embedUrl).getHost());
        showLoading(true);
        hideError();
        playerView.setVisibility(View.GONE);
        resizeButton.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                Log.d(TAG, "Blocked popup/new window from embed page");
                return false;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null && shouldBlockWebResource(request.getUrl().toString())) {
                    Log.d(TAG, "Blocked web resource=" + request.getUrl());
                    return createEmptyResponse();
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                showLoading(false);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) {
                    return true;
                }

                String requestedUrl = request.getUrl().toString();
                if (request.isForMainFrame() && shouldBlockMainFrameNavigation(requestedUrl)) {
                    Log.d(TAG, "Blocked navigation=" + requestedUrl);
                    return true;
                }

                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    showLoading(false);
                    showEmbedError();
                }
            }
        });
        webView.loadUrl(embedUrl);
    }

    private WebResourceResponse createEmptyResponse() {
        return new WebResourceResponse(
                "text/plain",
                "utf-8",
                new ByteArrayInputStream(new byte[0])
        );
    }

    private void initializePlayer(String videoUrl) {
        try {
            hideError();
            showLoading(true);
            player = new ExoPlayer.Builder(this)
                    .setSeekBackIncrementMs(10000)
                    .setSeekForwardIncrementMs(10000)
                    .build();
            player.setVolume(1.0f);
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    showLoading(playbackState == Player.STATE_BUFFERING);
                    if (playbackState == Player.STATE_READY) {
                        hideError();
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    showPlaybackError(error);
                }
            });
            playerView.setPlayer(player);
            MediaItem mediaItem = MediaItem.fromUri(videoUrl);
            player.setMediaItem(mediaItem);
            player.prepare();

            long savedProgress = getSavedProgress();
            if (savedProgress > 0L) {
                player.seekTo(savedProgress);
            }

            player.play();
        } catch (RuntimeException e) {
            finishWithPlaybackError(e);
        }
    }

    private void releasePlayer() {
        if (playerView != null) {
            playerView.setPlayer(null);
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void destroyWebView() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
    }

    private boolean isDirectVideoUrl(String url) {
        String normalizedUrl = url.toLowerCase();
        return normalizedUrl.contains(".m3u8")
                || normalizedUrl.contains(".mp4")
                || normalizedUrl.contains(".mpd")
                || normalizedUrl.contains(".webm");
    }

    private String getRootHost(String host) {
        if (TextUtils.isEmpty(host)) {
            return "";
        }
        String normalizedHost = host.toLowerCase(Locale.US);
        if (normalizedHost.startsWith("www.")) {
            normalizedHost = normalizedHost.substring(4);
        }

        String[] parts = normalizedHost.split("\\.");
        if (parts.length < 2) {
            return normalizedHost;
        }
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    private boolean shouldBlockMainFrameNavigation(String requestedUrl) {
        if (TextUtils.isEmpty(requestedUrl)) {
            return true;
        }

        Uri uri = Uri.parse(requestedUrl);
        String scheme = uri.getScheme();
        if ("about".equals(scheme) || "data".equals(scheme)) {
            return false;
        }
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return true;
        }

        String requestedRootHost = getRootHost(uri.getHost());
        return TextUtils.isEmpty(requestedRootHost)
                || TextUtils.isEmpty(allowedEmbedRootHost)
                || !requestedRootHost.equals(allowedEmbedRootHost);
    }

    private boolean shouldBlockWebResource(String requestedUrl) {
        String normalizedUrl = requestedUrl.toLowerCase(Locale.US);
        for (String blockedPart : BLOCKED_WEB_URL_PARTS) {
            if (normalizedUrl.contains(blockedPart)) {
                return true;
            }
        }
        return false;
    }

    private void retryPlayback() {
        hideError();
        showLoading(true);
        if (TextUtils.isEmpty(videoUrl)) {
            return;
        }
        if (usingWebEmbed) {
            webView.loadUrl(videoUrl);
        } else {
            releasePlayer();
            initializePlayer(videoUrl);
        }
    }

    private void saveProgress() {
        if (player == null) {
            return;
        }
        String progressKey = getProgressKey();
        if (progressKey == null) {
            return;
        }
        getSharedPreferences(PLAYER_PREFS, MODE_PRIVATE)
                .edit()
                .putLong(progressKey, player.getCurrentPosition())
                .apply();
    }

    private void setupActions() {
        findViewById(R.id.player_back_button).setOnClickListener(v -> finish());
        resizeButton.setOnClickListener(v -> toggleResizeMode());
        retryButton.setOnClickListener(v -> retryPlayback());
    }

    private void showLoading(boolean show) {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showPlaybackError(PlaybackException error) {
        Log.e(TAG, "Playback error: " + error.getMessage(), error);
        showLoading(false);
        if (errorContainer != null) {
            errorContainer.setVisibility(View.VISIBLE);
        }
        Toast.makeText(
                PlayerActivity.this,
                "Unable to play video: " + error.getErrorCodeName(),
                Toast.LENGTH_LONG
        ).show();
    }

    private void showEmbedError() {
        if (errorContainer != null) {
            errorContainer.setVisibility(View.VISIBLE);
        }
        Toast.makeText(this, "Unable to open stream page", Toast.LENGTH_LONG).show();
    }

    private void toggleResizeMode() {
        resizeMode = resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT
                ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                : AspectRatioFrameLayout.RESIZE_MODE_FIT;
        playerView.setResizeMode(resizeMode);
        resizeButton.setText(resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT ? "Fit" : "Zoom");
    }
}
