package lt.mredgariux.saugykla.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SignTextTest {
    @Test
    void wrapsMaterialNamesWithoutSplittingWords() {
        assertEquals(List.of("POLISHED BLACK", "STONE PRESSURE", "PLATE"),
                SignText.wrap("POLISHED BLACK STONE PRESSURE PLATE"));
    }

    @Test
    void keepsShortNamesOnOneLine() {
        assertEquals(List.of("RAW IRON"), SignText.wrap("RAW IRON"));
    }
}
