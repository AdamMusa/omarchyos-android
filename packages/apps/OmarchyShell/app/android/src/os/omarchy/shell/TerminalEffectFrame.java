package os.omarchy.shell;

import java.util.Arrays;

/** Bounded, full-frame SGR decoder. Never executes terminal control sequences. */
final class TerminalEffectFrame {
    final int columns, rows;
    final char[] glyphs;
    final int[] foreground, background;
    final byte[] styles;
    static final int BOLD = 1, DIM = 2, UNDERLINE = 4, REVERSE = 8, HIDDEN = 16,
            STRIKE = 32, ITALIC = 64;

    private TerminalEffectFrame(int columns, int rows) {
        this.columns = columns; this.rows = rows;
        int size = columns * rows;
        glyphs = new char[size]; Arrays.fill(glyphs, ' ');
        foreground = new int[size]; Arrays.fill(foreground, 0xffffffff);
        background = new int[size]; Arrays.fill(background, 0xff000000);
        styles = new byte[size];
    }

    static TerminalEffectFrame decode(String source, int columns, int rows) {
        if (columns < 1 || columns > 120 || rows < 1 || rows > 160 || source.length() > 4194304)
            throw new IllegalArgumentException("Invalid terminal frame");
        TerminalEffectFrame frame = new TerminalEffectFrame(columns, rows);
        int x = 0, y = 0, fg = 0xffffffff, bg = 0xff000000, style = 0;
        for (int i = 0; i < source.length() && y < rows; i++) {
            char c = source.charAt(i);
            if (c == '\n') { y++; x = 0; continue; }
            if (c == '\r') { x = 0; continue; }
            if (c == '\033') {
                if (i + 1 >= source.length() || source.charAt(i + 1) != '[') continue;
                int end = i + 2;
                while (end < source.length() && end - i < 80
                        && (Character.isDigit(source.charAt(end)) || source.charAt(end) == ';')) end++;
                if (end >= source.length() || end - i >= 80) break;
                if (source.charAt(end) == 'm') {
                    String[] codes = source.substring(i + 2, end).split(";", -1);
                    for (int n = 0; n < codes.length; n++) {
                        int code = number(codes[n]);
                        switch (code) {
                            case 0: fg = 0xffffffff; bg = 0xff000000; style = 0; break;
                            case 1: style |= BOLD; break;
                            case 2: style |= DIM; break;
                            case 3: style |= ITALIC; break;
                            case 4: style |= UNDERLINE; break;
                            case 7: style |= REVERSE; break;
                            case 8: style |= HIDDEN; break;
                            case 9: style |= STRIKE; break;
                            case 22: style &= ~(BOLD | DIM); break;
                            case 23: style &= ~ITALIC; break;
                            case 24: style &= ~UNDERLINE; break;
                            case 27: style &= ~REVERSE; break;
                            case 28: style &= ~HIDDEN; break;
                            case 29: style &= ~STRIKE; break;
                            case 39: fg = 0xffffffff; break;
                            case 49: bg = 0xff000000; break;
                            case 38: case 48:
                                int color = -1;
                                if (n + 4 < codes.length && number(codes[n + 1]) == 2) {
                                    int r = number(codes[n + 2]), g = number(codes[n + 3]), b = number(codes[n + 4]);
                                    if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255)
                                        color = 0xff000000 | r << 16 | g << 8 | b;
                                    n += 4;
                                } else if (n + 2 < codes.length && number(codes[n + 1]) == 5) {
                                    int index = number(codes[n + 2]);
                                    if (index >= 0 && index < 256) color = xterm(index);
                                    n += 2;
                                }
                                // -1 is also valid opaque white.
                                if (code == 38) fg = color; else bg = color;
                                break;
                            default:
                                if (code >= 30 && code <= 37) fg = xterm(code - 30);
                                else if (code >= 40 && code <= 47) bg = xterm(code - 40);
                                else if (code >= 90 && code <= 97) fg = xterm(code - 90 + 8);
                                else if (code >= 100 && code <= 107) bg = xterm(code - 100 + 8);
                        }
                    }
                }
                i = end;
                continue;
            }
            if (c < 32 || c == 127) continue;
            if (x < columns) {
                int at = y * columns + x;
                frame.glyphs[at] = c; frame.foreground[at] = fg;
                frame.background[at] = bg; frame.styles[at] = (byte) style;
            }
            x++;
        }
        return frame;
    }

    private static int number(String code) {
        if (code.isEmpty()) return 0;
        try { return Integer.parseInt(code); } catch (NumberFormatException error) { return -1; }
    }
    private static int xterm(int index) {
        int[] basic = {0x000000, 0x800000, 0x008000, 0x808000, 0x000080, 0x800080, 0x008080, 0xc0c0c0,
                0x808080, 0xff0000, 0x00ff00, 0xffff00, 0x0000ff, 0xff00ff, 0x00ffff, 0xffffff};
        if (index < 16) return 0xff000000 | basic[index];
        if (index >= 232) { int v = 8 + 10 * (index - 232); return 0xff000000 | v << 16 | v << 8 | v; }
        int cube = index - 16;
        int[] level = {0, 95, 135, 175, 215, 255};
        return 0xff000000 | level[cube / 36] << 16 | level[cube / 6 % 6] << 8 | level[cube % 6];
    }
}
