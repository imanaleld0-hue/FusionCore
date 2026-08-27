package dev.allofus.fusioncore.ide;

import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {
    private static final Pattern KEYWORDS = Pattern.compile("\\b(public|private|protected|class|void|int|boolean|string|static|new|return|if|else|try|catch)\\b");
    private static final Pattern STRINGS = Pattern.compile("\"[^\"]*\"");
    private static final Pattern COMMENTS = Pattern.compile("//.*|/\\*(.|\\n)*?\\*/");

    public static void highlight(Spannable spannable) {
        clearSpans(spannable);
        applyColor(spannable, KEYWORDS, 0xFF569CD6);
        applyColor(spannable, STRINGS, 0xFFCE9178);
        applyColor(spannable, COMMENTS, 0xFF6A9955);
    }

    private static void applyColor(Spannable spannable, Pattern pattern, int color) {
        Matcher matcher = pattern.matcher(spannable);
        while (matcher.find()) {
            spannable.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static void clearSpans(Spannable spannable) {
        ForegroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            spannable.removeSpan(span);
        }
    }
}
