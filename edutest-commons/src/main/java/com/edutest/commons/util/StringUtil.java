package com.edutest.commons.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class StringUtil {

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }

        return input.trim()
                .replaceAll("[ąĄ]", "a")
                .replaceAll("[ćĆ]", "c")
                .replaceAll("[ęĘ]", "e")
                .replaceAll("[łŁ]", "l")
                .replaceAll("[ńŃ]", "n")
                .replaceAll("[óÓ]", "o")
                .replaceAll("[śŚ]", "s")
                .replaceAll("[źżŹŻ]", "z")
                .replaceAll("[^a-zA-Z]", "");
    }

}

