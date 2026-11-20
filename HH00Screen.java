package dev.hh00.gui;

import dev.hh00.Client;
import dev.hh00.core.Module;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * hh00 menu v3: black and white, smooth, English only.
 * Everything is drawn in "design pixels" (1 design px = 1 real screen pixel at k = 1/guiScale).
 */
public class HH00Screen extends Screen {
    public static final Identifier FONT = Identifier.of("hh00", "ui"), FONT_BOLD = Identifier.of("hh00", "ub");
    /** Fallback toggles for features that have no {@link Module} implementation yet. */
    public static final Set<String> ENABLED = new HashSet<>(List.of(
        "ESP", "Fullbright", "Nametags", "Armor HUD", "Sprint", "Auto Tool", "Auto Totem", "Blur", "Sounds", "Animations"));

    // ---- palette: black and white only (ARGB) ----
    static final int PANEL = 0xF20A0A0C, EDGE = 0xF2232327, SIDE = 0x55000000, LINE = 0x14FFFFFF;
    static final int CARD = 0x10FFFFFF, CARD2 = 0x1CFFFFFF, TEXT = 0xFFF2F2F4, DIM = 0xFF86868E, NAV = 0xFFABABB3, NAME = 0xFFC9C9CF;
    static final int WHITE = 0xFFFFFFFF, INK = 0xFF0A0A0C, OFF = 0xFF34343A, KNOB_OFF = 0xFFB9B9C0, GREY = 0xFF8A8A92;
    static final int FIELD = 0xFF111114, FIELD_EDGE = 0xFF26262B, FIELD_FOCUS = 0xFFC4C4CA;

    // ---- layout (design px) ----
    static final int PW = 980, PH = 580, SB = 224, TOP = 68, COL_W = 353, GAP = 14, ROW = 42, OPT = 56;
    static final int FONT_DY = 3; // baseline fix for the TTF font

    record Group(String title, List<String> items) {}
    static final Map<String, List<Group>> DATA = new LinkedHashMap<>();
    static Group g(String t, String... i) { return new Group(t, List.of(i)); }
    static {
        DATA.put("Combat", List.of(g("Melee", "Aura", "Reach", "Criticals", "Auto Block"), g("Defense", "Velocity", "Auto Totem", "Auto Armor")));
        DATA.put("Visuals", List.of(g("World", "ESP", "Fullbright", "Tracers", "Zoom"), g("Interface", "Nametags", "Armor HUD", "Keystrokes", "Hit Colors")));
        DATA.put("Player", List.of(g("Movement", "Sprint", "Step", "No Slow", "Safe Walk"), g("Survival", "No Fall", "Auto Eat", "Scaffold")));
        DATA.put("Utilities", List.of(g("Inventory", "Auto Tool", "Chest Stealer", "Inventory Sort"), g("Misc", "Timer", "Auto Reconnect", "Chat Filter")));
        DATA.put("Settings", List.of(g("Menu", "Blur", "Sounds", "Animations", "Show Keybinds"), g("Client", "Notifications", "hh00+ features")));
    }

    enum Kind { TAB, LINK, TOGGLE, MORE, FOLD, BIND, SLIDER, SEARCH }
    record Hit(Kind kind, String key, int x, int y, int w, int h, boolean scrolls) {}

    // ---- state ----
    private String tab = "Settings", query = "", waitingBind = null, dragKey = null, user = "Player";
    private boolean searchFocus = false, closing = false, opened = false;
    private final Set<String> open = new HashSet<>(), folded = new HashSet<>();
    private final Map<String, Integer> strength = new HashMap<>(), binds = new HashMap<>();
    private final Map<String, Float> anim = new HashMap<>();
    private final Map<String, Text> texts = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private float scroll, scrollTarget, maxScroll, openT, contentT = 1f, alpha = 1f, dt, elapsed;
    private long last = Util.getMeasuringTimeMs(), openStart = last;
    private double k, ox, oy;
    private int mx, my;

    public HH00Screen() { super(Text.literal("hh00")); }

    @Override public boolean shouldPause() { return false; }

    /** Real module state when the module exists, fallback set otherwise. */
    static boolean isOn(String name) {
        Module module = Client.get().getModuleManager().get(name);
        return module != null ? module.isEnabled() : ENABLED.contains(name);
    }

    static void toggleModule(String name) {
        Module module = Client.get().getModuleManager().get(name);
        if (module != null) {
            module.toggle();
        } else if (!ENABLED.remove(name)) {
            ENABLED.add(name);
        }
    }

    @Override
    protected void init() {
        double s = client.getWindow().getScaleFactor();
        k = Math.min(1.0 / s, Math.min((width - 12.0) / PW, (height - 12.0) / PH));
        ox = (width - PW * k) / 2.0;
        oy = (height - PH * k) / 2.0;
        try { user = client.getSession().getUsername(); } catch (Throwable ignored) {}
        if (!opened) { opened = true; openStart = Util.getMeasuringTimeMs(); snd(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.35f, 0.10f); }
    }

    /** Animated close: fade out first, then really close in tick(). */
    @Override
    public void close() {
        if (closing) return;
        closing = true;
        snd(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.05f, 0.08f);
    }

    @Override
    public void tick() { if (closing && openT < 0.02f) client.setScreen(null); }

    // ================= sounds (soft, quiet, respects the "Sounds" module) =================
    private boolean soundsOn() { return isOn("Sounds"); }
    private void snd(SoundEvent e, float pitch, float vol) { if (soundsOn()) client.getSoundManager().play(PositionedSoundInstance.ui(e, pitch, vol)); }
    private void snd(RegistryEntry<SoundEvent> e, float pitch, float vol) { snd(e.value(), pitch, vol); }

    // ================= drawing primitives =================
    private static int mix(int a, int b, float t) {
        int r = 0;
        for (int s = 24; s >= 0; s -= 8) { int x = (a >>> s) & 255, y = (b >>> s) & 255; r |= Math.round(x + (y - x) * t) << s; }
        return r;
    }
    private static int fade(int col, float f) { return ((int) ((col >>> 24) * Math.max(0, Math.min(1, f))) << 24) | (col & 0xFFFFFF); }
    private int A(int col) { return fade(col, alpha); }
    private static float smooth(float x) { x = Math.max(0, Math.min(1, x)); return x * x * (3 - 2 * x); }
    private float st(int i, float base) { return smooth((elapsed - base - i * 0.045f) / 0.30f); }

    private void fillA(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        if (x2 > x1 && y2 > y1 && (A(col) >>> 24) > 0) c.fill(x1, y1, x2, y2, A(col));
    }

    /** Horizontal span with anti-aliased ends. */
    private void spanAA(DrawContext c, int y, float x1, float x2, int col) {
        if (x2 <= x1) return;
        int a = (int) Math.ceil(x1), b = (int) Math.floor(x2);
        if (b < a) { fillA(c, (int) Math.floor(x1), y, (int) Math.floor(x1) + 1, y + 1, fade(col, x2 - x1)); return; }
        if (b > a) fillA(c, a, y, b, y + 1, col);
        if (a - x1 > 0.02f) fillA(c, a - 1, y, a, y + 1, fade(col, a - x1));
        if (x2 - b > 0.02f) fillA(c, b, y, b + 1, y + 1, fade(col, x2 - b));
    }

    private static final Map<Integer, float[]> INS = new HashMap<>();
    private static float[] insets(float r) {
        return INS.computeIfAbsent(Math.round(r * 4), key -> {
            float[] a = new float[(int) Math.ceil(r)];
            for (int i = 0; i < a.length; i++) { float dy = r - i - 0.5f; a[i] = r - (float) Math.sqrt(Math.max(0, r * r - dy * dy)); }
            return a;
        });
    }

    /** Anti-aliased rounded rectangle, optional vertical gradient. */
    private void rr(DrawContext c, int x, int y, int w, int h, float r, int c1, int c2) {
        r = Math.min(r, Math.min(w, h) / 2f);
        float[] in = insets(r);
        int n = in.length;
        for (int row = 0; row < h; row++) {
            if (c1 == c2 && row == n && h - n > n) { fillA(c, x, y + n, x + w, y + h - n, c1); row = h - n - 1; continue; }
            float ins = row < n ? in[row] : row >= h - n ? in[h - 1 - row] : 0;
            int col = c1 == c2 ? c1 : mix(c1, c2, row / (float) Math.max(1, h - 1));
            spanAA(c, y + row, x + ins, x + w - ins, col);
        }
    }
    private void rr(DrawContext c, int x, int y, int w, int h, float r, int col) { rr(c, x, y, w, h, r, col, col); }
    private void dot(DrawContext c, int cx, int cy, int r, int col) { rr(c, cx - r, cy - r, r * 2, r * 2, r, col); }

    /** Anti-aliased ellipse ring; topOnly draws just the upper half (for the logo arch). */
    private void ring(DrawContext c, float cx, float cy, float rx, float ry, float t, int col, boolean topOnly) {
        int h = (int) Math.ceil(ry);
        for (int dy = -h; dy < (topOnly ? 0 : h); dy++) {
            float yy = dy + 0.5f, o = 1 - yy * yy / (ry * ry);
            if (o <= 0) continue;
            float wo = rx * (float) Math.sqrt(o), wi = 0, irx = rx - t, iry = ry - t;
            if (irx > 0 && iry > 0) { float ii = 1 - yy * yy / (iry * iry); if (ii > 0) wi = irx * (float) Math.sqrt(ii); }
            int row = Math.round(cy + dy);
            if (wi <= 0) spanAA(c, row, cx - wo, cx + wo, col);
            else { spanAA(c, row, cx - wo, cx - wi, col); spanAA(c, row, cx + wi, cx + wo, col); }
        }
    }

    /** Smooth line with round ends, per-pixel coverage. */
    private void aaLine(DrawContext c, float x1, float y1, float x2, float y2, float th, int col) {
        int minX = (int) Math.floor(Math.min(x1, x2) - th), maxX = (int) Math.ceil(Math.max(x1, x2) + th);
        int minY = (int) Math.floor(Math.min(y1, y2) - th), maxY = (int) Math.ceil(Math.max(y1, y2) + th);
        float dx = x2 - x1, dy = y2 - y1, len2 = dx * dx + dy * dy;
        for (int py = minY; py < maxY; py++)
            for (int px = minX; px < maxX; px++) {
                float cx = px + .5f, cy = py + .5f;
                float t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((cx - x1) * dx + (cy - y1) * dy) / len2));
                float qx = x1 + t * dx - cx, qy = y1 + t * dy - cy;
                float cov = th / 2 - (float) Math.sqrt(qx * qx + qy * qy) + .5f;
                if (cov > 0.02f) fillA(c, px, py, px + 1, py + 1, fade(col, cov));
            }
    }

    private Text T(String s, boolean bold) {
        return texts.computeIfAbsent((bold ? "b" : "r") + s, v -> Text.literal(s).setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(bold ? FONT_BOLD : FONT))));
    }
    private int tw(String s) { return textRenderer.getWidth(T(s, false)); }
    private int twb(String s) { return textRenderer.getWidth(T(s, true)); }
    private void text(DrawContext c, String s, int x, int cy, int col, boolean bold) {
        if ((A(col) >>> 24) < 8) return;
        c.drawText(textRenderer, T(s, bold), x, cy - FONT_DY, A(col), false);
    }
    private void text(DrawContext c, String s, int x, int cy, int col) { text(c, s, x, cy, col, false); }

    /** The hh logo: rounded "h", same as the avatar. h = overall height in px. */
    private void logoH(DrawContext c, float cx, float cy, float h, int col) {
        float u = h / 340f, t = 62 * u;
        java.util.function.DoubleUnaryOperator X = ax -> cx + (ax - 258) * u, Y = ay -> cy + (ay - 257) * u;
        int sx = (int) Math.round(X.applyAsDouble(172) - t / 2), sy = (int) Math.round(Y.applyAsDouble(118) - t / 2);
        rr(c, sx, sy, Math.round(t), Math.round(278 * u + t), t / 2, col);
        int lx = (int) Math.round(X.applyAsDouble(344) - t / 2), ly = (int) Math.round(Y.applyAsDouble(264));
        rr(c, lx, ly, Math.round(t), Math.round(132 * u + t / 2), t / 2, col);
        float R = 117 * u;
        ring(c, (float) X.applyAsDouble(258), (float) Y.applyAsDouble(264), R, R, t, col, true);
        // The small dot is part of the original hh00 mark.
        dot(c, (int) Math.round(X.applyAsDouble(344)), (int) Math.round(Y.applyAsDouble(118)),
            Math.max(1, Math.round(13 * u)), col);
    }

    private void icon(DrawContext c, String n, int cx, int cy, int col) {
        switch (n) {
            case "Combat" -> { ring(c, cx, cy, 8, 8, 1.8f, col, false); aaLine(c, cx, cy - 11, cx, cy - 5, 1.8f, col); aaLine(c, cx, cy + 5, cx, cy + 11, 1.8f, col);
                               aaLine(c, cx - 11, cy, cx - 5, cy, 1.8f, col); aaLine(c, cx + 5, cy, cx + 11, cy, 1.8f, col); }
            case "Visuals" -> { ring(c, cx, cy, 10, 6, 1.8f, col, false); dot(c, cx, cy, 3, col); }
            case "Player" -> { ring(c, cx, cy - 4, 4.5f, 4.5f, 1.8f, col, false); rr(c, cx - 8, cy + 3, 16, 7, 3.5f, col); }
            case "Utilities" -> { for (int i = 0; i < 4; i++) rr(c, cx - 8 + (i % 2) * 9, cy - 8 + (i / 2) * 9, 7, 7, 2.2f, col); }
            case "Settings" -> { rr(c, cx - 9, cy - 5, 18, 2, 1, col); rr(c, cx - 9, cy + 4, 18, 2, 1, col); dot(c, cx + 3, cy - 4, 3, col); dot(c, cx - 3, cy + 5, 3, col); }
            case "Help" -> { ring(c, cx, cy, 9, 9, 1.8f, col, false); text(c, "?", cx - twb("?") / 2, cy, col, true); }
            case "Updater" -> { aaLine(c, cx, cy - 8, cx, cy + 3, 2f, col); aaLine(c, cx - 5, cy - 2, cx, cy + 3, 2f, col); aaLine(c, cx + 5, cy - 2, cx, cy + 3, 2f, col);
                                aaLine(c, cx - 8, cy + 8, cx + 8, cy + 8, 2f, col); }
            case "dots" -> { dot(c, cx - 7, cy, 2, col); dot(c, cx, cy, 2, col); dot(c, cx + 7, cy, 2, col); }
            case "search" -> { ring(c, cx - 1, cy - 1, 6.5f, 6.5f, 1.8f, col, false); aaLine(c, cx + 4, cy + 4, cx + 8, cy + 8, 2f, col); }
        }
    }

    /** Chevron that rotates smoothly: open = pointing down, folded = pointing right. */
    private void chevron(DrawContext c, int cx, int cy, float open, int col) {
        double a = -(1 - open) * Math.PI / 2, co = Math.cos(a), si = Math.sin(a);
        float[][] p = {{-4, -2}, {0, 2}, {4, -2}};
        float[] px = new float[3], py = new float[3];
        for (int i = 0; i < 3; i++) { px[i] = cx + (float) (p[i][0] * co - p[i][1] * si); py[i] = cy + (float) (p[i][0] * si + p[i][1] * co); }
        aaLine(c, px[0], py[0], px[1], py[1], 1.8f, col);
        aaLine(c, px[1], py[1], px[2], py[2], 1.8f, col);
    }

    // ================= animation helpers =================
    private float an(String key, boolean on) {
        float tgt = on ? 1f : 0f, v = anim.computeIfAbsent(key, kk -> tgt);
        v += (tgt - v) * (1f - (float) Math.exp(-dt * 14f));
        if (Math.abs(tgt - v) < 0.003f) v = tgt;
        anim.put(key, v);
        return v;
    }
    private boolean in(int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    // ================= render =================
    @Override
    public void render(DrawContext c, int gmx, int gmy, float delta) {
        super.render(c, gmx, gmy, delta); // vanilla blur + dim behind the panel
        long now = Util.getMeasuringTimeMs();
        dt = Math.min(0.1f, (now - last) / 1000f); last = now;
        elapsed = (now - openStart) / 1000f;
        float tgt = closing ? 0f : 1f;
        openT += (tgt - openT) * (1f - (float) Math.exp(-dt * (closing ? 16f : 11f)));
        float e = smooth(openT);
        alpha = e;
        contentT += (1f - contentT) * (1f - (float) Math.exp(-dt * 13f));
        scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-dt * 16f));
        mx = (int) ((gmx - ox) / k); my = (int) ((gmy - oy) / k);
        hits.clear();

        var m = c.getMatrices();
        m.pushMatrix();
        m.translate((float) ox, (float) oy);
        m.scale((float) k, (float) k);
        float sc = 0.95f + 0.05f * e;
        m.translate(PW / 2f, PH / 2f + (1 - e) * 18f); m.scale(sc, sc); m.translate(-PW / 2f, -PH / 2f);

        rr(c, 0, 0, PW, PH, 22, EDGE);
        rr(c, 1, 1, PW - 2, PH - 2, 21, PANEL);
        drawSidebar(c);
        drawTop(c);
        drawContent(c);
        m.popMatrix();
    }

    private void nav(DrawContext c, int y, String label, String ic, boolean active, Kind kind, String key, int idx) {
        float a0 = alpha; alpha = a0 * st(idx, 0.06f);
        boolean hov = in(12, y, 200, 38);
        float t = an("nav:" + label, active), hv = an("hnav:" + label, hov);
        if (t > 0.01f) { rr(c, 12, y, 200, 38, 11, fade(WHITE, 0.10f * t)); rr(c, 12, y + 9, 3, 20, 1.5f, fade(WHITE, t)); }
        if (hv > 0.01f) rr(c, 12, y, 200, 38, 11, fade(WHITE, 0.05f * hv * (1 - t)));
        int col = mix(mix(NAV, TEXT, hv), WHITE, t);
        icon(c, ic, 32, y + 19, col);
        text(c, label, 52, y + 19, col);
        alpha = a0;
        hits.add(new Hit(kind, key, 12, y, 200, 38, false));
    }

    private void drawSidebar(DrawContext c) {
        c.enableScissor(0, 0, SB, PH);
        rr(c, 0, 0, SB + 30, PH, 22, SIDE);
        c.disableScissor();
        fillA(c, SB, 0, SB + 1, PH, LINE);

        // logo with a soft spring pop on open
        float pop = 1f - (float) (Math.exp(-6 * elapsed) * Math.cos(14 * elapsed)) * 0.3f;
        var m = c.getMatrices();
        m.pushMatrix(); m.translate(37, 37); m.scale(pop, pop); m.translate(-37, -37);
        rr(c, 20, 20, 34, 34, 11, WHITE, 0xFFC9C9CF);
        logoH(c, 37, 37, 20, INK);
        m.popMatrix();
        text(c, "hh00", 64, 37, WHITE, true);
        int bx = 64 + twb("hh00") + 6;
        rr(c, bx, 28, 18, 16, 6, CARD2);
        text(c, "+", bx + 9 - twb("+") / 2, 36, WHITE, true);

        text(c, "Modules", 22, 90, DIM);
        int y = 103, i = 0;
        for (String t : DATA.keySet()) { nav(c, y, t, t, t.equals(tab) && query.isEmpty(), Kind.TAB, t, i++); y += 38; }
        text(c, "More", 22, y + 18, DIM);
        y += 31;
        nav(c, y, "Help @fwoofiy", "Help", false, Kind.LINK, "help", i++); y += 38;
        nav(c, y, "Updater @fwoofiy", "Updater", false, Kind.LINK, "updater", i);

        rr(c, 12, PH - 66, 200, 54, 14, CARD);
        rr(c, 22, PH - 55, 32, 32, 16, WHITE, GREY);
        String ini = user.isEmpty() ? "P" : user.substring(0, 1).toUpperCase();
        text(c, ini, 22 + 16 - twb(ini) / 2, PH - 39, INK, true);
        text(c, user, 64, PH - 47, WHITE, true);
        text(c, "hh00 on 1.21.11", 64, PH - 30, DIM);
    }

    private void drawTop(DrawContext c) {
        String chip = "Right Shift to close";
        int cw = tw(chip) + 20, cx = PW - 18 - cw;
        rr(c, cx, 21, cw, 30, 9, CARD);
        text(c, chip, cx + 10, 36, DIM);

        int sx = SB + 18, sw = cx - 12 - sx;
        float f = an("focus", searchFocus);
        rr(c, sx, 16, sw, 40, 13, mix(FIELD_EDGE, FIELD_FOCUS, f));
        rr(c, sx + 1, 17, sw - 2, 38, 12, FIELD);
        icon(c, "search", sx + 21, 36, mix(DIM, WHITE, f));
        int tx = sx + 42;
        if (query.isEmpty()) text(c, "Search modules", tx, 36, DIM); else text(c, query, tx, 36, TEXT);
        if (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0) { int cxp = tx + (query.isEmpty() ? 0 : tw(query)) + 1; fillA(c, cxp, 28, cxp + 1, 44, WHITE); }
        hits.add(new Hit(Kind.SEARCH, "q", sx, 16, sw, 40, false));
    }

    private List<Group> search() {
        String q = query.toLowerCase();
        List<Group> out = new ArrayList<>();
        for (var e : DATA.entrySet())
            for (Group gr : e.getValue()) {
                List<String> mt = gr.items().stream().filter(n -> n.toLowerCase().contains(q)).toList();
                if (!mt.isEmpty()) out.add(new Group(e.getKey() + " / " + gr.title(), mt));
            }
        return out;
    }

    private void drawContent(DrawContext c) {
        int vx = SB + 1, vy = TOP, vw = PW - SB - 1, vh = PH - TOP;
        List<Group> groups = query.isEmpty() ? DATA.get(tab) : search();
        float outer = alpha;
        alpha = outer * contentT;
        c.enableScissor(vx, vy, vx + vw, vy + vh);
        if (groups.isEmpty()) {
            String s = "Nothing matches \u201C" + query + "\u201D. Try a module name.";
            text(c, s, vx + (vw - tw(s)) / 2, vy + 80, DIM);
        }
        int x0 = SB + 18, base = TOP + 4 - Math.round(scroll) + Math.round((1 - contentT) * 14);
        int[] colY = {0, 0};
        int gi = 0;
        for (Group gr : groups) {
            int col = colY[0] <= colY[1] ? 0 : 1;
            int gx = x0 + col * (COL_W + GAP), gy = base + colY[col];
            float ex = an("fold:" + gr.title(), !folded.contains(gr.title()));
            float[] op = new float[gr.items().size()];
            int bodyH = 0;
            for (int i = 0; i < op.length; i++) { op[i] = an("open:" + gr.items().get(i), open.contains(gr.items().get(i))); bodyH += ROW + Math.round(OPT * op[i]); }
            int vis = Math.round(bodyH * ex), gh = ROW + vis;
            colY[col] += gh + GAP;
            int idx = gi++;
            if (gy + gh < vy || gy > vy + vh) continue;

            float a1 = alpha; alpha = a1 * st(idx, 0.22f);
            rr(c, gx, gy, COL_W, gh, 16, CARD);
            int on = (int) gr.items().stream().filter(HH00Screen::isOn).count();
            text(c, gr.title(), gx + 14, gy + 21, WHITE, true);
            String cnt = on + "/" + gr.items().size();
            text(c, cnt, gx + COL_W - 40 - tw(cnt), gy + 21, DIM);
            chevron(c, gx + COL_W - 22, gy + 21, ex, DIM);
            hits.add(new Hit(Kind.FOLD, gr.title(), gx, gy, COL_W, ROW, true));

            if (vis > 0) {
                c.enableScissor(gx, gy + ROW, gx + COL_W, gy + ROW + vis);
                int ry = gy + ROW;
                for (int i = 0; i < op.length; i++) {
                    String n = gr.items().get(i);
                    float onT = an("on:" + n, isOn(n));
                    float hv = an("hrow:" + n, in(gx, ry, COL_W, ROW) && my >= vy && ex > 0.5f);
                    fillA(c, gx, ry, gx + COL_W, ry + 1, LINE);
                    if (hv > 0.01f) fillA(c, gx, ry + 1, gx + COL_W, ry + ROW, fade(WHITE, 0.035f * hv));
                    int sx = gx + COL_W - 10 - 36, dx = sx - 6 - 30;
                    boolean dh = in(dx, ry + 7, 30, 28);
                    float dv = an("hdots:" + n, dh);
                    text(c, n, gx + 14, ry + 21, mix(NAME, WHITE, onT));
                    float bg = Math.max(dv, op[i]);
                    if (bg > 0.01f) rr(c, dx, ry + 7, 30, 28, 8, fade(CARD2, bg));
                    icon(c, "dots", dx + 15, ry + 21, mix(DIM, TEXT, bg));
                    rr(c, sx, ry + 10, 36, 21, 10.5f, mix(OFF, TEXT, onT));
                    rr(c, sx + 3 + Math.round(15 * onT), ry + 13, 15, 15, 7.5f, mix(KNOB_OFF, INK, onT));
                    if (ex > 0.5f) {
                        hits.add(new Hit(Kind.TOGGLE, n, gx, ry, dx - gx, ROW, true));
                        hits.add(new Hit(Kind.MORE, n, dx, ry + 7, 30, 28, true));
                        hits.add(new Hit(Kind.TOGGLE, n, sx, ry + 5, 36, 32, true));
                    }
                    int oh = Math.round(OPT * op[i]);
                    if (oh > 0) {
                        c.enableScissor(gx, ry + ROW, gx + COL_W, ry + ROW + oh);
                        int cy = ry + ROW + 28, bw = 92, bx = gx + COL_W - 14 - bw, tx = gx + 14 + 76, tw2 = bx - 14 - tx;
                        int v = strength.getOrDefault(n, 60);
                        text(c, "Strength", gx + 14, cy, DIM);
                        rr(c, tx, cy - 2, tw2, 4, 2, 0x24FFFFFF);
                        int fw = Math.round(tw2 * v / 100f);
                        if (fw >= 4) rr(c, tx, cy - 2, fw, 4, 2, WHITE);
                        dot(c, tx + fw, cy, 6, WHITE);
                        rr(c, bx, cy - 13, bw, 26, 9, CARD2);
                        String bl = n.equals(waitingBind) ? "Press a key" : "Bind: " + (binds.containsKey(n) ? keyName(binds.get(n)) : "none");
                        text(c, bl, bx + (bw - tw(bl)) / 2, cy, TEXT);
                        c.disableScissor();
                        if (op[i] > 0.9f && ex > 0.5f) {
                            hits.add(new Hit(Kind.SLIDER, n, tx - 6, cy - 12, tw2 + 12, 24, true));
                            hits.add(new Hit(Kind.BIND, n, bx, cy - 13, bw, 26, true));
                        }
                    }
                    ry += ROW + oh;
                }
                c.disableScissor();
            }
            alpha = a1;
        }
        float contentH = TOP + 4 + Math.max(colY[0], colY[1]) + 18;
        maxScroll = Math.max(0, contentH - PH);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        c.disableScissor();

        float sbv = an("sbar", maxScroll > 0);
        if (sbv > 0.01f) {
            int th = Math.max(36, Math.round(vh * vh / (vh + maxScroll))), ty = vy + 8 + Math.round(scroll / maxScroll * (vh - th - 16));
            rr(c, PW - 9, ty, 3, th, 1.5f, fade(WHITE, 0.2f * sbv));
        }
        alpha = outer;
    }

    private static String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R-SHIFT"; case GLFW.GLFW_KEY_LEFT_SHIFT -> "L-SHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R-CTRL"; case GLFW.GLFW_KEY_LEFT_CONTROL -> "L-CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "L-ALT"; case GLFW.GLFW_KEY_RIGHT_ALT -> "R-ALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE"; case GLFW.GLFW_KEY_TAB -> "TAB";
            default -> "KEY " + key;
        };
    }

    // ================= input (1.21.9+ API) =================
    private void setSlider(Hit h, double dmx) {
        strength.put(h.key(), (int) Math.max(0, Math.min(100, (dmx - (h.x() + 6)) / (h.w() - 12) * 100)));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0 || closing) return false;
        double dmx = (click.x() - ox) / k, dmy = (click.y() - oy) / k;
        waitingBind = null;
        boolean hitSearch = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (dmx < h.x() || dmx >= h.x() + h.w() || dmy < h.y() || dmy >= h.y() + h.h()) continue;
            if (h.scrolls() && (dmy < TOP || dmx < SB)) continue;
            switch (h.kind()) {
                case TAB -> { if (!h.key().equals(tab) || !query.isEmpty()) { tab = h.key(); query = ""; scrollTarget = 0; contentT = 0f; snd(SoundEvents.UI_BUTTON_CLICK, 1.30f, 0.09f); } }
                case LINK -> { snd(SoundEvents.UI_BUTTON_CLICK, 1.30f, 0.09f); Util.getOperatingSystem().open("https://t.me/fwoofiy"); }
                case TOGGLE -> { boolean wasOn = isOn(h.key()); toggleModule(h.key()); snd(SoundEvents.UI_BUTTON_CLICK, wasOn ? 1.12f : 1.48f, 0.10f); }
                case MORE -> { if (!open.remove(h.key())) open.add(h.key()); snd(SoundEvents.UI_BUTTON_CLICK, 1.25f, 0.08f); }
                case FOLD -> { if (!folded.remove(h.key())) folded.add(h.key()); snd(SoundEvents.UI_BUTTON_CLICK, 1.20f, 0.08f); }
                case BIND -> { waitingBind = h.key(); snd(SoundEvents.UI_BUTTON_CLICK, 1.10f, 0.07f); }
                case SLIDER -> { dragKey = h.key(); setSlider(h, dmx); }
                case SEARCH -> hitSearch = true;
            }
            searchFocus = hitSearch;
            return true;
        }
        searchFocus = false;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragKey != null)
            for (Hit h : hits) if (h.kind() == Kind.SLIDER && h.key().equals(dragKey)) { setSlider(h, (click.x() - ox) / k); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) { dragKey = null; return super.mouseReleased(click); }

    @Override
    public boolean mouseScrolled(double x, double y, double horiz, double vert) {
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) vert * 48f));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (waitingBind != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) binds.remove(waitingBind); else binds.put(waitingBind, key);
            waitingBind = null;
            snd(SoundEvents.UI_BUTTON_CLICK, 1.30f, 0.08f);
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        if (searchFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) { query = query.substring(0, query.length() - 1); scrollTarget = 0; return true; }
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) { searchFocus = false; return true; }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchFocus && input.codepoint() >= 32 && input.codepoint() < 127 && query.length() < 24) {
            query += (char) input.codepoint();
            scrollTarget = 0;
            return true;
        }
        return super.charTyped(input);
    }
}
