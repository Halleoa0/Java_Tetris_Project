package settings;

import settings.Settings.Action;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 사용자 프로필의 키 할당과 전환 규칙을 관리한다. */
public final class KeyBindingProfiles {
    static final String DEFAULT_PROFILE = "Player1";

    private final Map<String, EnumMap<Action, Integer>> profiles = new LinkedHashMap<>();
    private String currentProfile = DEFAULT_PROFILE;

    KeyBindingProfiles() {
        profiles.put(DEFAULT_PROFILE, defaultKeys());
    }

    String currentProfile() {
        return currentProfile;
    }

    List<String> profileNames() {
        return new ArrayList<>(profiles.keySet());
    }

    int keyOf(Action action) {
        return profiles.get(currentProfile).get(action);
    }

    static boolean isReserved(int keyCode) {
        return keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_M
                || keyCode == KeyEvent.VK_UNDEFINED;
    }

    boolean setKey(Action action, int keyCode) {
        if (isReserved(keyCode)) return false;

        EnumMap<Action, Integer> keys = profiles.get(currentProfile);
        int oldKey = keys.get(action);
        for (Action other : Action.values()) {
            if (other != action && keys.get(other) == keyCode) {
                keys.put(other, oldKey);
                break;
            }
        }
        keys.put(action, keyCode);
        return true;
    }

    void resetKeys() {
        profiles.put(currentProfile, defaultKeys());
    }

    boolean createProfile(String name) {
        name = name == null ? "" : name.trim();
        if (name.isEmpty() || name.length() > 16 || name.matches(".*[=:\\s#!\\\\].*")
                || profiles.containsKey(name)) {
            return false;
        }

        profiles.put(name, new EnumMap<>(profiles.get(currentProfile)));
        currentProfile = name;
        return true;
    }

    boolean switchProfile(String name) {
        if (!profiles.containsKey(name)) return false;

        currentProfile = name;
        return true;
    }

    boolean deleteProfile(String name) {
        if (profiles.size() <= 1 || !profiles.containsKey(name)) return false;

        profiles.remove(name);
        if (currentProfile.equals(name)) {
            currentProfile = profiles.keySet().iterator().next();
        }
        return true;
    }

    Map<String, EnumMap<Action, Integer>> snapshot() {
        Map<String, EnumMap<Action, Integer>> snapshot = new LinkedHashMap<>();
        profiles.forEach((name, keys) -> snapshot.put(name, new EnumMap<>(keys)));
        return snapshot;
    }

    void restore(Map<String, EnumMap<Action, Integer>> savedProfiles, String savedCurrentProfile) {
        profiles.clear();
        savedProfiles.forEach((name, keys) -> profiles.put(name, new EnumMap<>(keys)));
        currentProfile = savedCurrentProfile;
    }

    private static EnumMap<Action, Integer> defaultKeys() {
        EnumMap<Action, Integer> keys = new EnumMap<>(Action.class);
        for (Action action : Action.values()) {
            keys.put(action, action.defaultKey);
        }
        return keys;
    }
}
