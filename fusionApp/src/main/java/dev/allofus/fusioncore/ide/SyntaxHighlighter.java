package dev.allofus.fusioncore.ide;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {
    private static final Pattern KEYWORDS = Pattern.compile(
            "\\b(using|namespace|class|public|private|protected|internal|void|return|if|else|for|while|"
                    + "new|try|catch|finally|throw|static|override|virtual|abstract|interface|struct|enum|get|set|"
                    + "bool|int|float|string|object|var|true|false|null)\\b");
    private static final Pattern STRINGS = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
    private static final Pattern COMMENTS = Pattern.compile("//.*");
    private static final Pattern NUMBERS = Pattern.compile("\\b\\d+\\b");

    public static Spannable highlightCs(String text) {
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        apply(sb, KEYWORDS, Color.parseColor("#569CD6"));
        apply(sb, STRINGS, Color.parseColor("#CE9178"));
        apply(sb, COMMENTS, Color.parseColor("#6A9955"));
        apply(sb, NUMBERS, Color.parseColor("#B5CEA8"));
        return sb;
    }

    private static void apply(SpannableStringBuilder sb, Pattern pattern, int color) {
        Matcher m = pattern.matcher(sb.toString());
        while (m.find()) {
            sb.setSpan(new ForegroundColorSpan(color), m.start(), m.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
