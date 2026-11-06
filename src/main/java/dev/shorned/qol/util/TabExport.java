package dev.shorned.qol.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Pure file format + writer for /sqol debug tab export. No Minecraft dependencies (unit-tested). */
public final class TabExport {
    public static final String MARKER = "# ---";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private TabExport() {}

    public static String filename(LocalDateTime t, int attempt) {
        return "tab-list-" + STAMP.format(t) + (attempt <= 1 ? "" : "-" + attempt) + ".txt";
    }

    /** Header (metadata lines starting with "# "), the marker line, then one entry per line, blank lines kept. */
    public static String format(List<String> lines, LocalDateTime t) {
        StringBuilder sb = new StringBuilder(lines.size() * 24 + 256);
        sb.append("# Shorned's QoL tab-list export\n");
        sb.append("# Exported: ").append(ISO.format(t)).append('\n');
        sb.append("# Lines: ").append(lines.size()).append('\n');
        sb.append("# Everything below the marker is one tab-list entry per line, in display order. Blank lines are real.\n");
        sb.append("# Text is plain: colour/format codes are not included.\n");
        sb.append(MARKER).append('\n');
        for (String l : lines) {
            sb.append(l == null ? "" : l.replace("\r", " ").replace("\n", " ")).append('\n');
        }
        return sb.toString();
    }

    /** Inverse of {@link #format}: the exported entries, header removed. Returns empty if there is no marker. */
    public static List<String> parse(String text) {
        String norm = text.replace("\r\n", "\n");
        String m = MARKER + "\n";
        int i = norm.startsWith(m) ? 0 : norm.indexOf("\n" + m);
        if (i < 0) return List.of();
        String body = norm.substring(i + (i == 0 ? m.length() : m.length() + 1));
        if (body.isEmpty()) return List.of();
        if (body.endsWith("\n")) body = body.substring(0, body.length() - 1);
        return new ArrayList<>(List.of(body.split("\n", -1)));
    }

    /** Writes a new timestamped file in {@code dir} (created if needed); never overwrites an existing export. */
    public static Path write(Path dir, LocalDateTime t, List<String> lines) throws IOException {
        Files.createDirectories(dir);
        byte[] data = format(lines, t).getBytes(StandardCharsets.UTF_8);
        for (int attempt = 1; attempt <= 99; attempt++) {
            Path f = dir.resolve(filename(t, attempt));
            try {
                Files.write(f, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                return f;
            } catch (FileAlreadyExistsException e) { /* try the next suffix */ }
        }
        throw new IOException("too many exports with the same timestamp in " + dir);
    }
}
