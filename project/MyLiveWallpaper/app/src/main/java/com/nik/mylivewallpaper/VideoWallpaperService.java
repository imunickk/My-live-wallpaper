package com.nik.mylivewallpaper;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.media.MediaPlayer;
import android.net.Uri;
import android.content.SharedPreferences;

import java.io.IOException;

public class VideoWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new VideoEngine();
    }

    private class VideoEngine extends Engine {

        private MediaPlayer player;
        private boolean visible;
        private boolean surfaceReady;

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

            if (player != null) {
                player.setDisplay(holder);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            surfaceReady = false;
            releasePlayer();
        }

        private void startPlayer() {
            if (!visible || !surfaceReady || player != null) {
                return;
            }

            SharedPreferences prefs =
                    getSharedPreferences("MyLiveWallpaper", MODE_PRIVATE);

            String chosen = prefs.getString("video_uri", null);

            if (chosen == null) {
                return;
            }

            try {
                player = new MediaPlayer();
                player.setDataSource(
                        VideoWallpaperService.this,
                        Uri.parse(chosen));

                player.setDisplay(getSurfaceHolder());
                player.setLooping(true);
                player.setVolume(0f, 0f);

                player.setOnPreparedListener(mp -> {
                    if (player == mp && visible && surfaceReady) {
                        mp.start();
                    }
                });

                player.setOnErrorListener((mp, what, extra) -> {
                    releasePlayer();
                    return true;
                });

                player.prepareAsync();

            } catch (IOException | IllegalArgumentException e) {
                releasePlayer();
            }
        }

        private void pausePlayer() {
            if (player != null && player.isPlaying()) {
                player.pause();
            }
        }

        private void releasePlayer() {
            if (player != null) {
                try {
                    player.setOnPreparedListener(null);
                    player.setOnErrorListener(null);
                    player.release();
                } catch (Exception ignored) {
                }

                player = null;
            }
        }

        @Override
        public void onDestroy() {
            visible = false;
            surfaceReady = false;
            releasePlayer();
            super.onDestroy();
        }
    }
}
