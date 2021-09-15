package org.stackstone.docker.deploy.util;

import org.fusesource.jansi.Ansi;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * AnsiUtil
 *
 * @author Lt5227
 * @date 2021/9/15
 * @since 1.0.0
 */
public class AnsiUtil {
    public static Ansi fgMsg(Ansi.Color color, String s) {
        return ansi().fg(color).a(s);
    }
}
