package com.example.pulsestepapplication;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

public class MusicPlayer {
    private static final String TAG = "MusicPlayer";
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private boolean isPaused = false;
    private boolean isMuted = false;
    private int previousVolume;

    // Constructor to initialize MediaPlayer
    public MusicPlayer(Context context, int musicResId) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer = MediaPlayer.create(context, musicResId);

        if (mediaPlayer == null) {
            Log.e(TAG, "Failed to initialize MediaPlayer. Check the audio resource.");
            return;
        }

        // Enable looping playback
        mediaPlayer.setLooping(true);

        // Reset to start on completion if looping is turned off
        mediaPlayer.setOnCompletionListener(mp -> {
            if (!mediaPlayer.isLooping()) {
                mp.seekTo(0);  // Reset to start
                Log.d(TAG, "Music playback completed.");
            }
        });
    }

    // Play the music
    public void play() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPaused = false;
            Log.d(TAG, "Music started.");
        }
    }

    // Pause the music
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPaused = true;
            Log.d(TAG, "Music paused.");
        }
    }

    // Check if the music is playing
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    // Check if the music is paused
    public boolean isPaused() {
        return isPaused;
    }

    // Check if the music is muted
    public boolean isMuted() {
        return isMuted;
    }

    // Mute the music
    public void mute() {
        if (!isMuted) {
            previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            isMuted = true;
            Log.d(TAG, "Music muted.");
        }
    }

    // Unmute the music
    public void unmute() {
        if (isMuted) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0);
            isMuted = false;
            Log.d(TAG, "Music unmuted.");
        }
    }

    // Release MediaPlayer resources
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "MediaPlayer resources released.");
        }
    }

    // Enable looping
    public void setLooping(boolean shouldLoop) {
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(shouldLoop);
            Log.d(TAG, "Looping set to: " + shouldLoop);
        }
    }

    // Check if music is looping
    public boolean isLooping() {
        return mediaPlayer != null && mediaPlayer.isLooping();
    }
}