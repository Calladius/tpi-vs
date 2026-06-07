package me.calladius.tpi_vs.tab;

import java.util.*;

public class TabAlignmentTest {

    private static final int[] CHAR_WIDTHS = new int[65536];
    static {
        for (int i = 0; i < CHAR_WIDTHS.length; i++) CHAR_WIDTHS[i] = 6;
        CHAR_WIDTHS[' '] = 4;
        CHAR_WIDTHS['!'] = 2;
        CHAR_WIDTHS['.'] = 2;
        CHAR_WIDTHS[':'] = 2;
        CHAR_WIDTHS[','] = 2;
        CHAR_WIDTHS[';'] = 2;
        CHAR_WIDTHS['|'] = 2;
        CHAR_WIDTHS['\''] = 2;
        CHAR_WIDTHS['i'] = 2;
        CHAR_WIDTHS['l'] = 3;
        CHAR_WIDTHS['`'] = 3;
        CHAR_WIDTHS['['] = 4;
        CHAR_WIDTHS[']'] = 4;
        CHAR_WIDTHS['('] = 4;
        CHAR_WIDTHS[')'] = 4;
        CHAR_WIDTHS['{'] = 4;
        CHAR_WIDTHS['}'] = 4;
        CHAR_WIDTHS['t'] = 4;
        CHAR_WIDTHS['I'] = 4;
        CHAR_WIDTHS['"'] = 4;
        CHAR_WIDTHS['*'] = 4;
        CHAR_WIDTHS['f'] = 5;
        CHAR_WIDTHS['k'] = 5;
        CHAR_WIDTHS['<'] = 5;
        CHAR_WIDTHS['>'] = 5;
        CHAR_WIDTHS['@'] = 7;
        CHAR_WIDTHS['~'] = 7;
        CHAR_WIDTHS['Б'] = 4;
        CHAR_WIDTHS['Г'] = 8;
        CHAR_WIDTHS['Д'] = 7;
        CHAR_WIDTHS['Е'] = 7;
        CHAR_WIDTHS['Л'] = 7;
        CHAR_WIDTHS['Н'] = 8;
        CHAR_WIDTHS['Ъ'] = 8;
        CHAR_WIDTHS['Ь'] = 7;
        CHAR_WIDTHS['Ю'] = 8;
        CHAR_WIDTHS['Я'] = 9;
        CHAR_WIDTHS['а'] = 7;
        CHAR_WIDTHS['б'] = 8;
        CHAR_WIDTHS['д'] = 8;
        CHAR_WIDTHS['к'] = 5;
        CHAR_WIDTHS['л'] = 7;
        CHAR_WIDTHS['р'] = 5;
        CHAR_WIDTHS['я'] = 7;
    }

    private static int getPixelWidth(String text) {
        int width = 0;
        boolean bold = false;
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '<') {
                int end = text.indexOf('>', i);
                if (end == -1) { i++; continue; }
                String tag = text.substring(i + 1, end).toLowerCase().trim();
                if (tag.equals("bold") || tag.equals("b")) {
                    bold = true;
                } else if (tag.equals("/bold") || tag.equals("/b")) {
                    bold = false;
                } else if (tag.equals("reset")) {
                    bold = false;
                }
                i = end + 1;
            } else {
                char c = text.charAt(i);
                int cw = (c < CHAR_WIDTHS.length) ? CHAR_WIDTHS[c] : 6;
                if (bold) cw += 1;
                width += cw;
                i++;
            }
        }
        return width;
    }

    private static final String DEFAULT_PREFIX = "<bold><aqua>VNLLA</aqua></bold>";

    public static void main(String[] args) {
        String[] nicknames = {
            "Calladius", "Gruz800", "Gruz800_2", "xXDarkXx", "ProGamer228",
            "i", "ll", "III", "MWMWMW", "fkfkfk",
            "Алексей", "Борис", "Влад", "Ярослав", "Дмитрий",
            "test_player", "Mike_OBrien", "lIlIlI", "W@TCH", "k!ng",
            "ab", "abc", "abcd", "abcde", "abcdef",
            "Player1", "Player2", "Player3", "Player4", "Player5",
            "нуб", "про", "мид", "топ", "лол",
            "ttttt", "iiiii", "lllll", "WWWWW", "mmmmm",
        };

        String[] prefixes = {
            DEFAULT_PREFIX,
            "vnlla",
            "<bold>vnlla</bold>",
            "<italic>vnlla</italic>",
            "<bold><italic>vnlla</italic></bold>",
            "<bold>vn</bold>lla",
            "v<bold>nll</bold>a",
            "<bold><underlined>vnlla</underlined></bold>",
            "<bold><strikethrough>vnlla</strikethrough></bold>",
            "<obfuscated>vnlla</obfuscated>",
            "<bold><obfuscated>vnlla</obfuscated></bold>",
            "<bold><red>VNLLA</red></bold>",
            "<bold><green>vnlla</green></bold>",
            "<bold><gold>VNLLA</gold></bold>",
            "<bold><gradient:red:blue>VNLLA</gradient></bold>",
            "<rainbow>vnlla</rainbow>",
            "<bold><rainbow>VNLLA</rainbow></bold>",
            "<hover:show_text:'tip'><bold>VNLLA</bold></hover>",
            "<click:run_command:/cmd><bold>vnlla</bold></click>",
            "<bold><red>V</red><blue>N</blue><green>L</green><yellow>L</yellow><aqua>A</aqua></bold>",
            "<bold>VN</bold><reset>LLA",
            "<bold>VN</bold><reset><bold>LLA</bold>",
            "<bold>VN</bold>",
            "ab",
            "<italic>xyz</italic>",
            "<bold>i.f</bold>",
            "<bold>l:t</bold>",
            "<bold>ВНЛЛА</bold>",
            "<bold>ЯДЛ</bold>",
            "",
            "<bold><italic><underlined><red>VNLLA</red></underlined></italic></bold>",
            "<bold><aqua>VNLLA</aqua></bold>",
            "<b>VNLLA</b>",
            "<font:default><bold>VNLLA</bold></font>",
            "<insertion:text><bold>VNLLA</bold></insertion>",
        };

        // динамический maxPx — как в реальном плагине
        int maxPx = getPixelWidth(DEFAULT_PREFIX);
        for (String prefix : prefixes) {
            if (prefix == null || prefix.isEmpty()) continue;
            int w = getPixelWidth(prefix);
            if (w > maxPx) maxPx = w;
        }
        System.out.println("ширина префиксной зоны (динамическая) = " + maxPx + "px\n");

        int total = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (String nick : nicknames) {
            for (String prefix : prefixes) {
                total++;
                String effectivePrefix = (prefix == null || prefix.isEmpty()) ? DEFAULT_PREFIX : prefix;
                int prefixPx = getPixelWidth(effectivePrefix);
                int padPx = Math.max(0, maxPx - prefixPx);
                int totalPx = padPx + prefixPx + 4;

                if (totalPx != maxPx + 4) {
                    failed++;
                    String err = String.format("НЕСХОДСТВО! ник=%-15s префикс=%-60s prefixPx=%d padPx=%d totalPx=%d (ожидалось %d)",
                        nick, prefix, prefixPx, padPx, totalPx, maxPx + 4);
                    errors.add(err);
                }
            }
        }

        System.out.println("проверено комбинаций: " + total);
        System.out.println("ошибок: " + failed);

        if (!errors.isEmpty()) {
            System.out.println("\n--- ошибки ---");
            for (String e : errors) {
                System.out.println(e);
            }
        }

        System.out.println("\n--- проверка конкретных строк ---");
        String[] testCases = {
            DEFAULT_PREFIX,
            "vnlla",
            "<bold>vnlla</bold>",
            "<bold><italic>vnlla</italic></bold>",
            "<bold>vn</bold>lla",
            "<bold><red>VNLLA</red></bold>",
            "<bold><gradient:red:blue>VNLLA</gradient></bold>",
            "<bold><rainbow>VNLLA</rainbow></bold>",
            "<hover:show_text:'tip'><bold>VNLLA</bold></hover>",
            "<bold><red>V</red><blue>N</blue><green>L</green><yellow>L</yellow><aqua>A</aqua></bold>",
            "<bold>VN</bold><reset>LLA",
            "<bold>VN</bold><reset><bold>LLA</bold>",
            "<bold>ВНЛЛА</bold>",
            "<bold>ЯДЛ</bold>",
            "<bold><italic><underlined><red>VNLLA</red></underlined></italic></bold>",
            "<b>VNLLA</b>",
        };

        for (String tc : testCases) {
            int w = getPixelWidth(tc);
            String visible = tc.replaceAll("<[^>]+>", "");
            System.out.println(String.format("  \"%s\" -> %dpx (видимый: \"%s\")", tc, w, visible));
        }

        System.out.println("\n--- все bold VNLLA варианты должны давать одинаковую ширину ---");
        String[] boldVariants = {
            "<bold>VNLLA</bold>",
            "<bold><red>VNLLA</red></bold>",
            "<bold><aqua>VNLLA</aqua></bold>",
            "<bold><gradient:red:blue>VNLLA</gradient></bold>",
            "<bold><rainbow>VNLLA</rainbow></bold>",
            "<bold><italic>VNLLA</italic></bold>",
            "<bold><underlined>VNLLA</underlined></bold>",
            "<bold><strikethrough>VNLLA</strikethrough></bold>",
            "<bold><obfuscated>VNLLA</obfuscated></bold>",
            "<bold><italic><underlined>VNLLA</underlined></italic></bold>",
            "<hover:show_text:'x'><bold>VNLLA</bold></hover>",
            "<click:run_command:/x><bold>VNLLA</bold></click>",
            "<insertion:x><bold>VNLLA</bold></insertion>",
            "<b>VNLLA</b>",
        };
        int firstWidth = -1;
        boolean allSame = true;
        for (String v : boldVariants) {
            int w = getPixelWidth(v);
            if (firstWidth < 0) firstWidth = w;
            if (w != firstWidth) {
                System.out.println("  РАЗНИЦА! \"" + v + "\" = " + w + "px (ожидалось " + firstWidth + ")");
                allSame = false;
            }
        }
        if (allSame) {
            System.out.println("  все bold VNLLA варианты = " + firstWidth + "px OK");
        }

        if (failed == 0 && allSame) {
            System.out.println("\n=== ВСЁ ОК, ники будут ровно ===");
        } else {
            System.out.println("\n=== ЕСТЬ ПРОБЛЕМЫ, ники съедут ===");
        }
    }
}
