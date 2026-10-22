package com.nik.mylivewallpaper;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.media.MediaPlayer;
import android.net.Uri;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;

public class VideoWallpaperService extends WallpaperService {

    private static final String TAG = "VideoWallpaperService";

    @Override
    public Engine onCreateEngine() {
        return new VideoEngine();
    }

    private class VideoEngine extends Engine {

        private MediaPlayer player;
        private boolean visible;
        private boolean surfaceReady;
        private boolean prepared;
        private SurfaceHolder currentHolder;

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;

            if (visible) {
                startPlayer();
            } else {
                pausePlayer();
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            currentHolder = holder;
            surfaceReady = true;
            startPlayer();
        }

        @Override
        public void onSurfaceChanged(
                SurfaceHolder holder,
                int format,
                int width,
                int height) {
            super.onSurfaceChanged(holder, format, width, height);
            currentHolder = holder;

            if (player != null) {
                try {
                    player.setDisplay(holder);
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Unable to attach wallpaper surface", e);
                }
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            surfaceReady = false;
            currentHolder = null;
            releasePlayer();
            super.onSurfaceDestroyed(holder);
        }

        private void startPlayer() {
            if (!visible || !surfaceReady || currentHolder == null) {
                return;
            }

            if (player != null) {
                if (prepared) {
                    try {
                        player.setDisplay(currentHolder);
                        if (!player.isPlaying()) {
                            player.start();
                        }
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Unable to restart video", e);
                        releasePlayer();
                    }
                }
                return;
            }

            SharedPreferences prefs =
                    getSharedPreferences("MyLiveWallpaper", MODE_PRIVATE);

            String chosen = prefs.getString("video_uri", null);

            if (chosen == null || chosen.isEmpty()) {
                Log.e(TAG, "No video URI saved");
                return;
            }

            try {
                MediaPlayer newPlayer = new MediaPlayer();
                player = newPlayer;
                prepared = false;

                newPlayer.setDataSource(
                        VideoWallpaperService.this,
                        Uri.parse(chosen));

                newPlayer.setDisplay(currentHolder);
                newPlayer.setLooping(true);
                newPlayer.setVolume(0f, 0f);

                newPlayer.setOnPreparedListener(mp -> {
                    if (player != mp) {
                        mp.release();
                        return;
                    }

                    prepared = true;

                    if (visible && surfaceReady && currentHolder != null) {
                        try {
                            mp.setDisplay(currentHolder);
                            mp.start();
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "Unable to start prepared video", e);
                            releasePlayer();
                        }
                    }
                });

                newPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
                    releasePlayer();
                    return true;
                });

                newPlayer.prepareAsync();

            } catch (IOException | IllegalArgumentException
                     | IllegalStateException e) {
                Log.e(TAG, "Unable to load selected video", e);
                releasePlayer();
            }
        }

        private void pausePlayer() {
            if (player != null && prepared) {
                try {
                    if (player.isPlaying()) {
                        player.pause();
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Unable to pause video", e);
                    releasePlayer();
                }
            }
        }

        private void releasePlayer() {
            MediaPlayer oldPlayer = player;
            player = null;
            prepared = false;

            if (oldPlayer != null) {
                try {
                    oldPlayer.setOnPreparedListener(null);
                    oldPlayer.setOnErrorListener(null);
                    oldPlayer.setDisplay(null);
                    oldPlayer.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error releasing MediaPlayer", e);
                }
            }
        }

        @Override
        public void onDestroy() {
            visible = false;
            surfaceReady = false;
            currentHolder = null;
            releasePlayer();
            super.onDestroy();
        }
    }
}
