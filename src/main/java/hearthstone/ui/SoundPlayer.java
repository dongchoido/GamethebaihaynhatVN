package hearthstone.ui;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small, optional sound effects using only the JDK. Audio never blocks Swing. */
public final class SoundPlayer {
    public enum Effect {
        START("Start"), SELECT("HeroSelect"), PLAY("Play"),
        CARD("PlayCard"), ATTACK("Attack"), END_TURN("EndTurn"), VICTORY("Victory");

        private final String file;
        Effect(String file) { this.file = file; }
    }

    private static final ExecutorService AUDIO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "game-audio");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile boolean enabled = true;
    private static Clip activeClip;

    private SoundPlayer() { }

    public static boolean isEnabled() { return enabled; }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            AUDIO.execute(SoundPlayer::stopCurrent);
        }
    }

    public static void play(Effect effect) {
        if (!enabled) {
            return;
        }
        AUDIO.execute(() -> {
            if (!enabled) {
                return;
            }
            URL resource = SoundPlayer.class.getResource("/sounds/" + effect.file + ".wav");
            if (resource == null) {
                return;
            }
            stopCurrent();
            Clip clip = null;
            try (var input = new BufferedInputStream(resource.openStream());
                 AudioInputStream audio = AudioSystem.getAudioInputStream(input)) {
                clip = AudioSystem.getClip();
                Clip playing = clip;
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        playing.close();
                    }
                });
                clip.open(audio);
                activeClip = clip;
                if (enabled) {
                    clip.start();
                } else {
                    stopCurrent();
                }
            } catch (Exception ignored) {
                if (clip != null) {
                    clip.close();
                }
                // Missing audio devices or unsupported files must not interrupt a match.
            }
        });
    }

    private static void stopCurrent() {
        if (activeClip != null) {
            activeClip.close();
            activeClip = null;
        }
    }
}
