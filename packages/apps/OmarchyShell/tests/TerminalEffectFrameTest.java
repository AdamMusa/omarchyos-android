package os.omarchy.shell;

public final class TerminalEffectFrameTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        TerminalEffectFrame frame = TerminalEffectFrame.decode("\033[38;2;12;34;56mA\033[0mB\n\033[48;5;196;1;7mC", 2, 2);
        check(frame.foreground[0] == 0xff0c2238, "true color");
        check(frame.foreground[1] == 0xffffffff, "reset color");
        check(frame.background[2] == 0xffff0000, "xterm color");
        check(frame.styles[2] == (TerminalEffectFrame.BOLD | TerminalEffectFrame.REVERSE), "combined styles");
        check(frame.glyphs[3] == ' ', "unused cells clear");
        frame = TerminalEffectFrame.decode("ABC\nD\033[3;4;9mE\033[23;24;29mF", 2, 2);
        check(new String(frame.glyphs).equals("ABDE"), "clip each row without wrapping");
        check(frame.styles[3] == (TerminalEffectFrame.ITALIC | TerminalEffectFrame.UNDERLINE | TerminalEffectFrame.STRIKE), "style rendering");
        frame = TerminalEffectFrame.decode("\033[999999999999999999999mX\033[2JY", 2, 1);
        check(new String(frame.glyphs).equals("XY"), "ignore unsupported control commands");
        try { TerminalEffectFrame.decode("", 10000, 10000); throw new AssertionError("unbounded canvas"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("PASS: ANSI colors, styles, clipping, reset, unsupported controls, canvas bounds");
    }
}
