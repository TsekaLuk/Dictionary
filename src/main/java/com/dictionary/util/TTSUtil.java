package com.dictionary.util;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class TTSUtil {
    private static Voice voice;
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) {
            return;
        }

        try {
            System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
            VoiceManager voiceManager = VoiceManager.getInstance();
            voice = voiceManager.getVoice("kevin16");
            
            if (voice != null) {
                voice.allocate();
                initialized = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error initializing TTS: " + e.getMessage());
        }
    }

    public static void speak(String text) {
        if (!initialized) {
            initialize();
        }

        try {
            if (voice != null) {
                voice.speak(text);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error speaking text: " + e.getMessage());
        }
    }

    public static void cleanup() {
        if (voice != null) {
            try {
                voice.deallocate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
} 