package shop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import settings.Settings;

/**
 * 플레이어별 코인 / 구매한 아이템 / 장착한 테마 / 위클리 미션 진행도를 저장한다. (싱글톤: PlayerData.get())
 * - 프로필(Settings.currentProfile())마다 따로 저장
 * - 파일: 사용자 홈 폴더의 .tetris2026/player.properties (Settings, Records와 같은 폴더)
 */
public final class PlayerData {
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".tetris2026", "player.properties");
    private static final PlayerData INSTANCE = new PlayerData();

    public static PlayerData get() { return INSTANCE; }

    /** 미션 달성 같은 알림을 화면(GamePanel)이 받아서 잠깐 보여줄 때 쓴다. */
    public interface Toast { void show(String message); }

    private final Properties props = new Properties();
    private final List<Toast> toasts = new CopyOnWriteArrayList<>();

    private PlayerData() { load(); }

    private static String p() { return Settings.get().currentProfile(); }
    private static String k(String field) { return "u." + p() + "." + field; }

    public void addToastListener(Toast t) { toasts.add(t); }
    public void removeToastListener(Toast t) { toasts.remove(t); }
    private void toast(String msg) { for (Toast t : toasts) t.show(msg); }

    // ===== 코인 =====

    public synchronized int coins() { return getInt(k("coins"), 0); }

    public synchronized void addCoins(int amount) {
        props.setProperty(k("coins"), String.valueOf(Math.max(0, coins() + amount)));
        save();
    }

    // ===== 아이템 구매 / 장착 =====

    public synchronized boolean owns(ShopItem item) {
        if (item.isFree()) return true;
        for (String s : props.getProperty(k("owned"), "").split(",")) if (s.equals(item.name())) return true;
        return false;
    }

    /** 코인이 충분하고 아직 없으면 구매하고 true. */
    public synchronized boolean buy(ShopItem item) {
        if (owns(item) || coins() < item.price) return false;
        String owned = props.getProperty(k("owned"), "");
        props.setProperty(k("owned"), owned.isEmpty() ? item.name() : owned + "," + item.name());
        props.setProperty(k("coins"), String.valueOf(coins() - item.price));
        save();
        return true;
    }

    /** 가지고 있는 아이템만 장착된다. */
    public synchronized boolean equip(ShopItem item) {
        if (!owns(item)) return false;
        props.setProperty(k(item.type == ShopItem.Type.MINO ? "mino" : "bg"), item.name());
        save();
        return true;
    }

    public synchronized ShopItem equipped(ShopItem.Type type) {
        String v = props.getProperty(k(type == ShopItem.Type.MINO ? "mino" : "bg"), "");
        ShopItem it = ShopItem.byName(v, type);
        return owns(it) ? it : ShopItem.defaultOf(type);
    }

    // ===== 위클리 미션 =====

    /** 주가 바뀌었으면 진행도를 초기화한다. (미션 조회/기록 전에 항상 호출) */
    private void ensureWeek() {
        int week = WeeklyMissions.currentWeekId();
        if (getInt(k("week"), 0) == week) return;
        props.setProperty(k("week"), String.valueOf(week));
        for (Mission m : Mission.values()) {
            props.remove(k("m." + m.name()));
            props.remove(k("done." + m.name()));
        }
        save();
    }

    public synchronized int progress(Mission m) {
        ensureWeek();
        return Math.min(m.target, getInt(k("m." + m.name()), 0));
    }

    public synchronized boolean isDone(Mission m) {
        ensureWeek();
        return "1".equals(props.getProperty(k("done." + m.name()), "0"));
    }

    /**
     * 이번 주 미션 중 해당 지표의 진행도를 올린다. 누적 지표는 더하고, 최댓값 지표는 더 클 때만 바꾼다.
     * 목표를 처음 채우면 코인 10개를 지급하고 알림을 보낸다.
     */
    public void record(Mission.Metric metric, int value) {
        if (value <= 0) return;
        List<String> messages = new ArrayList<>();
        synchronized (this) {
            ensureWeek();
            boolean changed = false;
            for (Mission m : WeeklyMissions.current()) {
                if (m.metric != metric || isDone(m)) continue;
                int now = getInt(k("m." + m.name()), 0);
                int next = metric.isMax ? Math.max(now, value) : now + value;
                if (next == now) continue;
                props.setProperty(k("m." + m.name()), String.valueOf(next));
                changed = true;
                if (next >= m.target) {
                    props.setProperty(k("done." + m.name()), "1");
                    props.setProperty(k("coins"), String.valueOf(coins() + Mission.REWARD_COINS));
                    messages.add("MISSION CLEAR! " + m.label + "  +" + Mission.REWARD_COINS + " COIN");
                }
            }
            if (changed) save();
        }
        for (String msg : messages) toast(msg);
    }

    /** 이번 주 미션 진행 현황 (화면용). */
    public synchronized Map<Mission, int[]> weeklyStatus() {
        ensureWeek();
        Map<Mission, int[]> out = new EnumMap<>(Mission.class);
        for (Mission m : WeeklyMissions.current()) out.put(m, new int[]{progress(m), isDone(m) ? 1 : 0});
        return out;
    }

    // ===== 저장 / 불러오기 =====

    private int getInt(String key, int def) {
        try { return Integer.parseInt(props.getProperty(key, String.valueOf(def)).trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private void load() {
        if (!Files.exists(FILE)) return;
        try (InputStream in = Files.newInputStream(FILE)) { props.load(in); }
        catch (IOException e) { System.err.println("플레이어 데이터 불러오기 실패: " + e.getMessage()); }
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) { props.store(out, "Tetris player data"); }
        } catch (IOException e) {
            System.err.println("플레이어 데이터 저장 실패: " + e.getMessage());
        }
    }
}
