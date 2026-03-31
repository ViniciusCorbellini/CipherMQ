package com.manocorbas.ciphermq.util.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void debug(String component, String msg) {
        log(LogLevel.DEBUG, component, msg, null);
    }

    public static void info(String component, String msg) {
        log(LogLevel.INFO, component, msg, null);
    }

    public static void warn(String component, String msg) {
        log(LogLevel.WARN, component, msg, null);
    }

    public static void error(String component, String msg, Throwable t) {
        log(LogLevel.ERROR, component, msg, t);
    }

    private static void log(LogLevel level, String component, String msg, Throwable t) {

        if (level.ordinal() < LogConfig.CURRENT_LEVEL.ordinal()) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        // timestamp
        if (LogConfig.SHOW_TIME) {
            sb.append("[").append(LocalDateTime.now().format(FORMAT)).append("]");
        }

        // level
        sb.append("[").append(level).append("]");

        // thread
        if (LogConfig.SHOW_THREAD) {
            sb.append("[")
                    .append(Thread.currentThread().getName())
                    .append("]");
        }

        // componente
        sb.append("[").append(component).append("] ");

        // mensagem
        sb.append(msg);

        System.out.println(sb);

        // error message
        if (t != null) {
            t.printStackTrace();
        }
    }
}