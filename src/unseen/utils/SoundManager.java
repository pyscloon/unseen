package unseen.utils;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton that manages one-shot sound effects (SFX).
 * Background music is still handled separately in GamePanel to allow looping/alternating.
 */
public class SoundManager {

    private static SoundManager instance;

    // Maps a sound name (e.g. "footstep") to its loaded audio data
    private final Map<String, byte[]> soundData = new HashMap<>();
    private final Map<String, AudioFormat> formats = new HashMap<>();

    private boolean sfxEnabled = true;
    private float globalSfxVolume = 0.5f;

    public void setSfxEnabled(boolean enabled) { this.sfxEnabled = enabled; }
    public boolean isSfxEnabled() { return sfxEnabled; }
    public void setGlobalSfxVolume(float volume) { this.globalSfxVolume = volume; }

    private SoundManager() {
        // Pre-load common sounds if they exist
        load("footstep1",    "unseen/assets/sound/footstep1.wav");
        load("footstep2",    "unseen/assets/sound/footstep2.wav");
        load("item_pickup",  "unseen/assets/sound/item_pickup_sf.wav");
        load("lantern",      "unseen/assets/sound/lantern_sf.wav");
        load("noisemaker",   "unseen/assets/sound/noisemaker_sf.wav");
        load("shuriken",     "unseen/assets/sound/shuriken_sf.wav");
        load("smoke",        "unseen/assets/sound/smoke_sf.wav");
    }

    public static SoundManager get() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * Loads a sound file into memory as a byte array to allow fast multiple playbacks.
     */
    public void load(String name, String path) {
        try {
            URL url = getClass().getClassLoader().getResource(path);
            if (url == null) return;

            InputStream is = new BufferedInputStream(url.openStream());
            AudioInputStream ais = AudioSystem.getAudioInputStream(is);
            
            AudioFormat format = ais.getFormat();
            byte[] data = ais.readAllBytes();
            
            soundData.put(name, data);
            formats.put(name, format);
            ais.close();
        } catch (Exception e) {
            System.err.println("SoundManager Error: Could not load " + path + " - " + e.getMessage());
        }
    }

    /**
     * Plays a sound once. Creates a new Clip from memory data each time to allow
     * sounds to overlap (e.g. multiple enemies walking or rapid item use).
     */
    public void play(String name) {
        play(name, 1.0f);
    }

    public void play(String name, float localVolume) {
        if (!sfxEnabled) return;
        
        byte[] data = soundData.get(name);
        AudioFormat format = formats.get(name);
        
        if (data == null || format == null) return;

        try {
            Clip clip = AudioSystem.getClip();
            clip.open(format, data, 0, data.length);
            
            // Apply volume
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float volume = globalSfxVolume * localVolume;
                // Convert linear scale (0-1) to decibels
                float dB = (float) (Math.log(Math.max(volume, 0.0001)) / Math.log(10.0) * 20.0);
                gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB)));
            }

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            
            clip.start();
        } catch (Exception e) {
            System.err.println("SoundManager Error: Could not play " + name);
        }
    }

    /**
     * Helper to play a random sound from a list (e.g. footsteps).
     */
    public void playRandom(float volume, String... names) {
        if (names.length == 0) return;
        int idx = (int) (Math.random() * names.length);
        play(names[idx], volume);
    }

    public void playRandom(String... names) {
        playRandom(1.0f, names);
    }
}
