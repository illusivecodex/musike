package app.musike.android;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * The app's side of PlaybackService (web/src/native/media.ts): the player reports the track and its progress
 * through update(), and the buttons pressed outside the app come back as "action" events.
 */
@CapacitorPlugin(name = "MusikePlayback")
public class MusikePlaybackPlugin extends Plugin {

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public void load() {
        PlaybackService.listener = (action, position) -> {
            JSObject event = new JSObject();
            event.put("action", action);
            if (position >= 0) event.put("position", position);
            notifyListeners("action", event);
        };
        PlaybackService.setAudioListener((event, position, duration, buffered, message) -> {
            JSObject data = new JSObject();
            data.put("position", position);
            data.put("duration", duration);
            data.put("buffered", buffered);
            if (message != null) data.put("message", message);
            notifyListeners("audio", new JSObject().put("event", event).put("data", data));
        });
    }

    @PluginMethod
    public void update(PluginCall call) {
        String title = call.getString("title");
        String artist = call.getString("artist");
        String album = call.getString("album");
        String artwork = call.getString("artwork");
        Boolean playing = call.getBoolean("playing");
        Double position = call.getDouble("position");
        Double duration = call.getDouble("duration");
        main.post(() -> {
            PlaybackService.NowPlaying state = PlaybackService.state;
            long now = SystemClock.elapsedRealtime();
            if (position != null) {
                state.positionMs = Math.max(0, Math.round(position * 1000));
            } else {
                state.positionMs = state.currentPositionMs();
            }

            state.positionAt = now;
            if (title != null) state.title = title;
            if (artist != null) state.artist = artist;
            if (album != null) state.album = album;
            if (artwork != null) state.artwork = artwork;
            if (playing != null) state.playing = playing;
            if (duration != null) state.durationMs = Math.max(0, Math.round(duration * 1000));
            else if (title != null) state.durationMs = 0; // a new track, not loaded yet
            PlaybackService.refresh(getContext());
        });
        call.resolve();
    }

    @PluginMethod
    public void audio(PluginCall call) {
        String command = call.getString("cmd", "");
        String url = call.getString("url", "");
        Double value = call.getDouble("value", 0);
        PlaybackService.audioCommand(getContext(), command, url, value == null ? 0 : value);
        call.resolve();
    }

    @Override
    protected void handleOnDestroy() {
        PlaybackService.listener = null;
        PlaybackService.audioListener = null;
    }
}
