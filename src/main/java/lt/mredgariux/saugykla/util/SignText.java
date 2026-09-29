package lt.mredgariux.saugykla.util;

import java.util.ArrayList;
import java.util.List;

public final class SignText {
    private static final int MAX_LINE_LENGTH = 15;
    private static final int MAX_LINES = 4;

    private SignText() {
    }

    public static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (String word : text.split(" ")) {
            if (!currentLine.isEmpty() && currentLine.length() + word.length() + 1 > MAX_LINE_LENGTH) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
            }
            if (!currentLine.isEmpty()) {
                currentLine.append(' ');
            }
            currentLine.append(word);
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return List.copyOf(lines.subList(0, Math.min(lines.size(), MAX_LINES)));
    }
}
