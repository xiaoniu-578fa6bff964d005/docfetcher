package net.sourceforge.docfetcher.gui;

import org.eclipse.swt.SWT;

/**
 * Code copied from class swingwt\awt\event\KeyEvent.java
 * in project swingwt http://swingwt.sourceforge.net
 *
 * Usage:
 *     JXGrabKey.getInstance().registerAWTHotkey(1,
 *			KeyCodeTranslator.translateSWTModifiers(SWT.CTRL),
 *			KeyCodeTranslator.translateSWTKey(SWT.F8));
 */
public final class KeyCodeTranslator {
	
	private KeyCodeTranslator() {
	}

	/** Returns the AWT modifier code for an SWT modifier */
    public static int translateSWTModifiers(int swtModifiers) {
        int awtModifiers = 0;
        if ((swtModifiers & SWT.CTRL) > 0)
            awtModifiers = awtModifiers | java.awt.event.InputEvent.CTRL_MASK;
        if ((swtModifiers & SWT.SHIFT) > 0)
            awtModifiers = awtModifiers | java.awt.event.InputEvent.SHIFT_MASK;
        if ((swtModifiers & SWT.ALT) > 0)
            awtModifiers = awtModifiers | java.awt.event.InputEvent.ALT_MASK;
        if ((swtModifiers & SWT.BUTTON1) > 0)
            awtModifiers = awtModifiers | java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
        if ((swtModifiers & SWT.BUTTON2) > 0)
            awtModifiers = awtModifiers | java.awt.event.InputEvent.BUTTON2_DOWN_MASK;
        if ((swtModifiers & SWT.BUTTON3) > 0)
            awtModifiers = awtModifiers | java.awt.event.InputEvent.BUTTON3_DOWN_MASK;
        if ((swtModifiers & SWT.COMMAND) > 0)
            awtModifiers = awtModifiers | java.awt.event.InputEvent.META_MASK;
        return awtModifiers;
    }

	/** Returns the AWT key code for an SWT key */
    public static int translateSWTKey(int swtKey) {
        int awt = 0;
        for (int i = 1; i < translationMap.length; i += 2) {
            if (translationMap[i] == swtKey) {
                awt = translationMap[i - 1];
                break;
            }
        }
        return awt;
    }

    /** Returns the SWT key code for an AWT key */
    public static int translateAWTKey(int awtKey) {
        int swt = 0;
        for (int i = 0; i < translationMap.length; i += 2) {
            if (translationMap[i] == awtKey) {
                swt = translationMap[i + 1];
                break;
            }
        }
        return swt;
    }

    private static final int VK_ENTER = '\n';
    private static final int VK_BACK_SPACE = '\b';
    private static final int VK_TAB  = '\t';
    private static final int VK_CANCEL = 0x03;
    private static final int VK_CLEAR = 0x0C;
    private static final int VK_SHIFT = 0x10;
    private static final int VK_CONTROL = 0x11;
    private static final int VK_ALT  = 0x12;
    private static final int VK_PAUSE = 0x13;
    private static final int VK_CAPS_LOCK = 0x14;
    private static final int VK_ESCAPE = 0x1B;
    private static final int VK_SPACE = 0x20;
    private static final int VK_PAGE_UP = 0x21;
    private static final int VK_PAGE_DOWN = 0x22;
    private static final int VK_END = 0x23;
    private static final int VK_HOME = 0x24;
    private static final int VK_LEFT = 0x25;
    private static final int VK_UP = 0x26;
    private static final int VK_RIGHT = 0x27;
    private static final int VK_DOWN = 0x28;
    private static final int VK_COMMA = 0x2C;
    private static final int VK_MINUS = 0x2D;
    private static final int VK_PERIOD = 0x2E;
    private static final int VK_SLASH = 0x2F;
    private static final int VK_0 = 0x30;
    private static final int VK_1 = 0x31;
    private static final int VK_2 = 0x32;
    private static final int VK_3 = 0x33;
    private static final int VK_4 = 0x34;
    private static final int VK_5 = 0x35;
    private static final int VK_6 = 0x36;
    private static final int VK_7 = 0x37;
    private static final int VK_8 = 0x38;
    private static final int VK_9 = 0x39;
    private static final int VK_SEMICOLON = 0x3B;
    private static final int VK_EQUALS = 0x3D;
    private static final int VK_A = 0x41;
    private static final int VK_B = 0x42;
    private static final int VK_C = 0x43;
    private static final int VK_D = 0x44;
    private static final int VK_E = 0x45;
    private static final int VK_F = 0x46;
    private static final int VK_G = 0x47;
    private static final int VK_H = 0x48;
    private static final int VK_I = 0x49;
    private static final int VK_J = 0x4A;
    private static final int VK_K = 0x4B;
    private static final int VK_L = 0x4C;
    private static final int VK_M = 0x4D;
    private static final int VK_N = 0x4E;
    private static final int VK_O = 0x4F;
    private static final int VK_P = 0x50;
    private static final int VK_Q = 0x51;
    private static final int VK_R = 0x52;
    private static final int VK_S = 0x53;
    private static final int VK_T = 0x54;
    private static final int VK_U = 0x55;
    private static final int VK_V = 0x56;
    private static final int VK_W = 0x57;
    private static final int VK_X = 0x58;
    private static final int VK_Y = 0x59;
    private static final int VK_Z = 0x5A;
    private static final int VK_OPEN_BRACKET = 0x5B;
    private static final int VK_BACK_SLASH = 0x5C;
    private static final int VK_CLOSE_BRACKET  = 0x5D;
    private static final int VK_NUMPAD0 = 0x60;
    private static final int VK_NUMPAD1 = 0x61;
    private static final int VK_NUMPAD2 = 0x62;
    private static final int VK_NUMPAD3 = 0x63;
    private static final int VK_NUMPAD4 = 0x64;
    private static final int VK_NUMPAD5 = 0x65;
    private static final int VK_NUMPAD6 = 0x66;
    private static final int VK_NUMPAD7 = 0x67;
    private static final int VK_NUMPAD8 = 0x68;
    private static final int VK_NUMPAD9 = 0x69;
    private static final int VK_MULTIPLY = 0x6A;
    private static final int VK_ADD = 0x6B;
    private static final int VK_SEPARATOR = 0x6C;
    private static final int VK_SUBTRACT = 0x6D;
    private static final int VK_DECIMAL = 0x6E;
    private static final int VK_DIVIDE = 0x6F;
    private static final int VK_DELETE = 0x7F;
    private static final int VK_NUM_LOCK = 0x90;
    private static final int VK_SCROLL_LOCK = 0x91;
    private static final int VK_F1 = 0x70;
    private static final int VK_F2 = 0x71;
    private static final int VK_F3 = 0x72;
    private static final int VK_F4 = 0x73;
    private static final int VK_F5 = 0x74;
    private static final int VK_F6 = 0x75;
    private static final int VK_F7 = 0x76;
    private static final int VK_F8 = 0x77;
    private static final int VK_F9 = 0x78;
    private static final int VK_F10 = 0x79;
    private static final int VK_F11 = 0x7A;
    private static final int VK_F12 = 0x7B;
    private static final int VK_PRINTSCREEN = 0x9A;
    private static final int VK_INSERT = 0x9B;
    private static final int VK_HELP = 0x9C;
    private static final int VK_META = 0x9D;
    private static final int VK_BACK_QUOTE = 0xC0;
    private static final int VK_QUOTE = 0xDE;
    private static final int VK_KP_UP = 0xE0;
    private static final int VK_KP_DOWN = 0xE1;
    private static final int VK_KP_LEFT = 0xE2;
    private static final int VK_KP_RIGHT = 0xE3;
    private static final int VK_AMPERSAND = 0x96;
    private static final int VK_ASTERISK = 0x97;
    private static final int VK_QUOTEDBL = 0x98;
    private static final int VK_LESS = 0x99;
    private static final int VK_GREATER  = 0xa0;
    private static final int VK_BRACELEFT = 0xa1;
    private static final int VK_BRACERIGHT = 0xa2;
    private static final int VK_AT = 0x0200;
    private static final int VK_COLON = 0x0201;
    private static final int VK_CIRCUMFLEX = 0x0202;
    private static final int VK_DOLLAR = 0x0203;
    private static final int VK_EURO_SIGN = 0x0204;
    private static final int VK_EXCLAMATION_MARK = 0x0205;
    private static final int VK_INVERTED_EXCLAMATION_MARK = 0x0206;
    private static final int VK_LEFT_PARENTHESIS = 0x0207;
    private static final int VK_NUMBER_SIGN = 0x0208;
    private static final int VK_PLUS = 0x0209;
    private static final int VK_RIGHT_PARENTHESIS = 0x020A;
    private static final int VK_UNDERSCORE = 0x020B;

    private static final int SWTVK_A = 'A';
    private static final int SWTVK_B = 'B';
    private static final int SWTVK_C = 'C';
    private static final int SWTVK_D = 'D';
    private static final int SWTVK_E = 'E';
    private static final int SWTVK_F = 'F';
    private static final int SWTVK_G = 'G';
    private static final int SWTVK_H = 'H';
    private static final int SWTVK_I = 'I';
    private static final int SWTVK_J = 'J';
    private static final int SWTVK_K = 'K';
    private static final int SWTVK_L = 'L';
    private static final int SWTVK_M = 'M';
    private static final int SWTVK_N = 'N';
    private static final int SWTVK_O = 'O';
    private static final int SWTVK_P = 'P';
    private static final int SWTVK_Q = 'Q';
    private static final int SWTVK_R = 'R';
    private static final int SWTVK_S = 'S';
    private static final int SWTVK_T = 'T';
    private static final int SWTVK_U = 'U';
    private static final int SWTVK_V = 'V';
    private static final int SWTVK_W = 'W';
    private static final int SWTVK_X = 'X';
    private static final int SWTVK_Y = 'Y';
    private static final int SWTVK_Z = 'Z';
    private static final int SWTVK_SPACE = ' ';
    private static final int SWTVK_0 = '0';
    private static final int SWTVK_1 = '1';
    private static final int SWTVK_2 = '2';
    private static final int SWTVK_3 = '3';
    private static final int SWTVK_4 = '4';
    private static final int SWTVK_5 = '5';
    private static final int SWTVK_6 = '6';
    private static final int SWTVK_7 = '7';
    private static final int SWTVK_8 = '8';
    private static final int SWTVK_9 = '9';
    private static final int SWTVK_SEMICOLON = ';';
    private static final int SWTVK_EQUALS = '=';
    private static final int SWTVK_COMMA = ',';
    private static final int SWTVK_MINUS = '-';
    private static final int SWTVK_PERIOD = '.';
    private static final int SWTVK_SLASH = '/';
    private static final int SWTVK_OPEN_BRACKET = '(';
    private static final int SWTVK_BACK_SLASH = '\\';
    private static final int SWTVK_CLOSE_BRACKET = ')';
    private static final int SWTVK_UNDERSCORE = '_';
    private static final int SWTVK_ADD = '+';
    private static final int SWTVK_PLUS = '+';
    private static final int SWTVK_NUMBER_SIGN = '+';
    private static final int SWTVK_MULTIPLY = '*';
    private static final int SWTVK_SUBTRACT = '-';
    private static final int SWTVK_DECIMAL = '.';
    private static final int SWTVK_DIVIDE = '.';
    private static final int SWTVK_BACK_QUOTE = '`';
    private static final int SWTVK_QUOTE = '\'';
    private static final int SWTVK_AMPERSAND = '&';
    private static final int SWTVK_ASTERISK = '*';
    private static final int SWTVK_QUOTEDBL = '"';
    private static final int SWTVK_LESS = '<';
    private static final int SWTVK_GREATER = '>';
    private static final int SWTVK_BRACELEFT = '{';
    private static final int SWTVK_BRACERIGHT = '}';
    private static final int SWTVK_AT = '@';
    private static final int SWTVK_CIRCUMFLEX = '~';
    private static final int SWTVK_DOLLAR = '$';
    private static final int SWTVK_EURO_SIGN = '$';
    private static final int SWTVK_EXCLAMATION_MARK = '!';
    private static final int SWTVK_INVERTED_EXCLAMATION_MARK = '!';
    private static final int SWTVK_LEFT_PARENTHESIS = '(';
    private static final int SWTVK_RIGHT_PARENTHESIS = ')';
    private static final int SWTVK_COLON = ':';
    private static final int SWTVK_TAB = '\t';
    private static final int SWTVK_F1 = SWT.F1;
    private static final int SWTVK_F2 = SWT.F2;
    private static final int SWTVK_F3 = SWT.F3;
    private static final int SWTVK_F4 = SWT.F4;
    private static final int SWTVK_F5 = SWT.F5;
    private static final int SWTVK_F6 = SWT.F6;
    private static final int SWTVK_F7 = SWT.F7;
    private static final int SWTVK_F8 = SWT.F8;
    private static final int SWTVK_F9 = SWT.F9;
    private static final int SWTVK_F10 = SWT.F10;
    private static final int SWTVK_F11 = SWT.F11;
    private static final int SWTVK_F12 = SWT.F12;
    private static final int SWTVK_ENTER = SWT.CR;
    private static final int SWTVK_BACK_SPACE = 8;
    private static final int SWTVK_DELETE = SWT.DEL;
    private static final int SWTVK_ESCAPE = SWT.ESC;

    // NOTE: I think this is right... should use the arrow ids instead of left/right/etc (intended for alignment)
    private static final int SWTVK_LEFT = SWT.ARROW_LEFT;
    private static final int SWTVK_RIGHT = SWT.ARROW_RIGHT;
    private static final int SWTVK_UP = SWT.ARROW_UP;
    private static final int SWTVK_DOWN = SWT.ARROW_DOWN;
    private static final int SWTVK_HOME = SWT.HOME;
    private static final int SWTVK_END = SWT.END;
    /*
    private static final int SWTVK_LEFT = SWT.LEFT;
    private static final int SWTVK_KP_LEFT = SWT.LEFT;
    private static final int SWTVK_RIGHT = SWT.RIGHT;
    private static final int SWTVK_KP_RIGHT = SWT.RIGHT;
    private static final int SWTVK_UP = SWT.UP;
    private static final int SWTVK_KP_UP = SWT.UP;
    private static final int SWTVK_DOWN = SWT.DOWN;
    private static final int SWTVK_KP_DOWN = SWT.DOWN;
    private static final int SWTVK_HOME = SWT.HOME;
    private static final int SWTVK_END = SWT.END;
    */

    private static final int SWTVK_PAGE_UP = SWT.PAGE_UP;
    private static final int SWTVK_PAGE_DOWN = SWT.PAGE_DOWN;
    private static final int SWTVK_INSERT = SWT.INSERT;
    private static final int SWTVK_SHIFT = SWT.SHIFT;
    private static final int SWTVK_CONTROL = SWT.CONTROL;
    private static final int SWTVK_ALT = SWT.ALT;
    private static final int SWTVK_META = SWT.ALT;
    private static final int SWTVK_CANCEL = SWT.CANCEL;
    private static final int SWTVK_CLEAR = SWT.NONE;
    private static final int SWTVK_PAUSE = SWT.PAUSE;
    private static final int SWTVK_CAPS_LOCK = SWT.CAPS_LOCK;
    private static final int SWTVK_SEPARATOR = SWT.SEPARATOR;
    private static final int SWTVK_NUM_LOCK = SWT.NUM_LOCK;
    private static final int SWTVK_SCROLL_LOCK = SWT.SCROLL_LOCK;
    private static final int SWTVK_PRINTSCREEN = SWT.PRINT_SCREEN;
    private static final int SWTVK_HELP = SWT.HELP;

    /** Map of SWT key constants to AWT constants. This is to
      * ensure binary compatibility for existing Swing/AWT apps.
      */
    private static int[] translationMap = new int[] {
        VK_ENTER,               SWTVK_ENTER,
        VK_BACK_SPACE,          SWTVK_BACK_SPACE,
        VK_TAB,                 SWTVK_TAB,
        VK_CANCEL,              SWTVK_CANCEL,
        VK_CLEAR,               SWTVK_CLEAR,
        VK_SHIFT,               SWTVK_SHIFT,
        VK_CONTROL,             SWTVK_CONTROL,
        VK_ALT,                 SWTVK_ALT,
        VK_PAUSE,               SWTVK_PAUSE,
        VK_CAPS_LOCK,           SWTVK_CAPS_LOCK,
        VK_ESCAPE,              SWTVK_ESCAPE,
        VK_SPACE,               SWTVK_SPACE,
        VK_PAGE_UP,             SWTVK_PAGE_UP,
        VK_PAGE_DOWN,           SWTVK_PAGE_DOWN,
        VK_END,                 SWTVK_END,
        VK_HOME,                SWTVK_HOME,
        VK_LEFT,                SWTVK_LEFT,
        VK_UP,                  SWTVK_UP,
        VK_RIGHT,               SWTVK_RIGHT,
        VK_DOWN,                SWTVK_DOWN,
        VK_COMMA,               SWTVK_COMMA,
        VK_MINUS,               SWTVK_MINUS,
        VK_PERIOD,              SWTVK_PERIOD,
        VK_SLASH,               SWTVK_SLASH,
        VK_0,                   SWTVK_0,
        VK_1,                   SWTVK_1,
        VK_2,                   SWTVK_2,
        VK_3,                   SWTVK_3,
        VK_4,                   SWTVK_4,
        VK_5,                   SWTVK_5,
        VK_6,                   SWTVK_6,
        VK_7,                   SWTVK_7,
        VK_8,                   SWTVK_8,
        VK_9,                   SWTVK_9,
        VK_SEMICOLON,           SWTVK_SEMICOLON,
        VK_EQUALS,              SWTVK_EQUALS,
        VK_A,                   SWTVK_A,
        VK_B,                   SWTVK_B,
        VK_C,                   SWTVK_C,
        VK_D,                   SWTVK_D,
        VK_E,                   SWTVK_E,
        VK_F,                   SWTVK_F,
        VK_G,                   SWTVK_G,
        VK_H,                   SWTVK_H,
        VK_I,                   SWTVK_I,
        VK_J,                   SWTVK_J,
        VK_K,                   SWTVK_K,
        VK_L,                   SWTVK_L,
        VK_M,                   SWTVK_M,
        VK_N,                   SWTVK_N,
        VK_O,                   SWTVK_O,
        VK_P,                   SWTVK_P,
        VK_Q,                   SWTVK_Q,
        VK_R,                   SWTVK_R,
        VK_S,                   SWTVK_S,
        VK_T,                   SWTVK_T,
        VK_U,                   SWTVK_U,
        VK_V,                   SWTVK_V,
        VK_W,                   SWTVK_W,
        VK_X,                   SWTVK_X,
        VK_Y,                   SWTVK_Y,
        VK_Z,                   SWTVK_Z,
        VK_OPEN_BRACKET,        SWTVK_OPEN_BRACKET,
        VK_BACK_SLASH,          SWTVK_BACK_SLASH,
        VK_CLOSE_BRACKET,       SWTVK_CLOSE_BRACKET,
        VK_NUMPAD0,             SWTVK_0,
        VK_NUMPAD1,             SWTVK_1,
        VK_NUMPAD2,             SWTVK_2,
        VK_NUMPAD3,             SWTVK_3,
        VK_NUMPAD4,             SWTVK_4,
        VK_NUMPAD5,             SWTVK_5,
        VK_NUMPAD6,             SWTVK_6,
        VK_NUMPAD7,             SWTVK_7,
        VK_NUMPAD8,             SWTVK_8,
        VK_NUMPAD9,             SWTVK_9,
        VK_MULTIPLY,            SWTVK_MULTIPLY,
        VK_ADD,                 SWTVK_ADD,
        VK_SEPARATOR,           SWTVK_SEPARATOR,
        VK_SUBTRACT,            SWTVK_SUBTRACT,
        VK_DECIMAL,             SWTVK_DECIMAL,
        VK_DIVIDE,              SWTVK_DIVIDE,
        VK_DELETE,              SWTVK_DELETE,
        VK_NUM_LOCK,            SWTVK_NUM_LOCK,
        VK_SCROLL_LOCK,         SWTVK_SCROLL_LOCK,
        VK_F1,                  SWTVK_F1,
        VK_F2,                  SWTVK_F2,
        VK_F3,                  SWTVK_F3,
        VK_F4,                  SWTVK_F4,
        VK_F5,                  SWTVK_F5,
        VK_F6,                  SWTVK_F6,
        VK_F7,                  SWTVK_F7,
        VK_F8,                  SWTVK_F8,
        VK_F9,                  SWTVK_F9,
        VK_F10,                 SWTVK_F10,
        VK_F11,                 SWTVK_F11,
        VK_F12,                 SWTVK_F12,
        VK_PRINTSCREEN,         SWTVK_PRINTSCREEN,
        VK_INSERT,              SWTVK_INSERT,
        VK_HELP,                SWTVK_HELP,
        VK_META,                SWTVK_META,
        VK_BACK_QUOTE,          SWTVK_BACK_QUOTE,
        VK_QUOTE,               SWTVK_QUOTE,
        VK_KP_UP,               SWTVK_UP,
        VK_KP_DOWN,             SWTVK_DOWN,
        VK_KP_LEFT,             SWTVK_LEFT,
        VK_KP_RIGHT,            SWTVK_RIGHT,
        VK_AMPERSAND,           SWTVK_AMPERSAND,
        VK_ASTERISK,            SWTVK_ASTERISK,
        VK_QUOTEDBL,            SWTVK_QUOTEDBL,
        VK_LESS,                SWTVK_LESS,
        VK_GREATER,             SWTVK_GREATER,
        VK_BRACELEFT,           SWTVK_BRACELEFT,
        VK_BRACERIGHT,          SWTVK_BRACERIGHT,
        VK_AT,                  SWTVK_AT,
        VK_COLON,               SWTVK_COLON,
        VK_CIRCUMFLEX,          SWTVK_CIRCUMFLEX,
        VK_DOLLAR,              SWTVK_DOLLAR,
        VK_EURO_SIGN,           SWTVK_EURO_SIGN,
        VK_EXCLAMATION_MARK,    SWTVK_EXCLAMATION_MARK,
        VK_INVERTED_EXCLAMATION_MARK, SWTVK_INVERTED_EXCLAMATION_MARK,
        VK_LEFT_PARENTHESIS,    SWTVK_LEFT_PARENTHESIS,
        VK_NUMBER_SIGN,         SWTVK_NUMBER_SIGN,
        VK_PLUS,                SWTVK_PLUS,
        VK_RIGHT_PARENTHESIS,   SWTVK_RIGHT_PARENTHESIS,
        VK_UNDERSCORE,          SWTVK_UNDERSCORE,
        VK_A,                   SWTVK_A + 32,
        VK_B,                   SWTVK_B + 32,
        VK_C,                   SWTVK_C + 32,
        VK_D,                   SWTVK_D + 32,
        VK_E,                   SWTVK_E + 32,
        VK_F,                   SWTVK_F + 32,
        VK_G,                   SWTVK_G + 32,
        VK_H,                   SWTVK_H + 32,
        VK_I,                   SWTVK_I + 32,
        VK_J,                   SWTVK_J + 32,
        VK_K,                   SWTVK_K + 32,
        VK_L,                   SWTVK_L + 32,
        VK_M,                   SWTVK_M + 32,
        VK_N,                   SWTVK_N + 32,
        VK_O,                   SWTVK_O + 32,
        VK_P,                   SWTVK_P + 32,
        VK_Q,                   SWTVK_Q + 32,
        VK_R,                   SWTVK_R + 32,
        VK_S,                   SWTVK_S + 32,
        VK_T,                   SWTVK_T + 32,
        VK_U,                   SWTVK_U + 32,
        VK_V,                   SWTVK_V + 32,
        VK_W,                   SWTVK_W + 32,
        VK_X,                   SWTVK_X + 32,
        VK_Y,                   SWTVK_Y + 32,
        VK_Z,                   SWTVK_Z + 32,
    };
}
