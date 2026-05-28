package com.zcpu.tzzmod.stabilization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class CodeQualityGuardSupport {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private CodeQualityGuardSupport() {
    }

    static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("build.gradle")) && Files.exists(current.resolve("src"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate project root from " + Path.of("").toAbsolutePath());
    }

    static Path path(String relative) {
        return projectRoot().resolve(relative.replace("/", java.io.File.separator));
    }

    static String read(String relative) throws IOException {
        return Files.readString(path(relative), StandardCharsets.UTF_8);
    }

    static List<String> readLines(String relative) throws IOException {
        return Files.readAllLines(path(relative), StandardCharsets.UTF_8);
    }

    static long bytes(String relative) throws IOException {
        return Files.size(path(relative));
    }

    static long lineCount(String relative) throws IOException {
        try (Stream<String> lines = Files.lines(path(relative), StandardCharsets.UTF_8)) {
            return lines.count();
        }
    }

    static int count(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    static int countRegex(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    static String lineSlice(String relative, int startInclusive, int endInclusive) throws IOException {
        List<String> lines = readLines(relative);
        int start = Math.max(0, startInclusive - 1);
        int end = Math.min(lines.size(), endInclusive);
        return String.join("\n", lines.subList(start, end));
    }

    static boolean selectorBlockContains(String css, String selector, String declaration) {
        String normalized = WHITESPACE.matcher(css).replaceAll("");
        String selectorNeedle = WHITESPACE.matcher(selector).replaceAll("");
        String declarationNeedle = WHITESPACE.matcher(declaration).replaceAll("");
        int index = 0;
        while ((index = normalized.indexOf(selectorNeedle, index)) >= 0) {
            int open = normalized.indexOf('{', index);
            int close = normalized.indexOf('}', index);
            if (open >= 0 && close > open) {
                String block = normalized.substring(open + 1, close);
                if (block.contains(declarationNeedle)) {
                    return true;
                }
            }
            index += selectorNeedle.length();
        }
        return false;
    }

    static List<String> selectorPointerEventsValues(String css, String selector) {
        String normalized = WHITESPACE.matcher(css).replaceAll("");
        String selectorNeedle = WHITESPACE.matcher(selector).replaceAll("");
        List<String> values = new ArrayList<>();
        int blockStart = 0;
        while (blockStart < normalized.length()) {
            int open = normalized.indexOf('{', blockStart);
            if (open < 0) {
                break;
            }
            int close = normalized.indexOf('}', open);
            if (close < 0) {
                break;
            }
            String selectorList = normalized.substring(blockStart, open);
            if (selectorListContains(selectorList, selectorNeedle)) {
                String block = normalized.substring(open + 1, close);
                Matcher matcher = Pattern.compile("pointer-events:([^;}]+)").matcher(block);
                while (matcher.find()) {
                    values.add(matcher.group(1));
                }
            }
            blockStart = close + 1;
        }
        return values;
    }

    private static boolean selectorListContains(String selectorList, String selectorNeedle) {
        for (String token : selectorList.split(",")) {
            if (selectorTokenMatches(token, selectorNeedle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean selectorTokenMatches(String token, String selectorNeedle) {
        return token.equals(selectorNeedle)
                || token.startsWith(selectorNeedle + ":")
                || token.startsWith(selectorNeedle + ".")
                || token.startsWith(selectorNeedle + "[");
    }

    static String findNodeExecutable() {
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, System.getProperty("node.path"));
        addCandidate(candidates, System.getenv("CODEX_NODE"));
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        candidates.add(windows ? "node.exe" : "node");
        Path home = Path.of(System.getProperty("user.home", ""));
        candidates.add(home.resolve(".cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/" + (windows ? "node.exe" : "node")).toString());
        candidates.add(home.resolve(".cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node").toString());
        candidates.add(home.resolve(".cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe").toString());
        for (String candidate : candidates) {
            if (isRunnable(candidate)) {
                return candidate;
            }
        }
        throw new AssertionError("Node.js is required for WebAdmin bundle guard but no runnable node executable was found");
    }

    private static void addCandidate(List<String> candidates, String value) {
        if (value != null && !value.isBlank()) {
            candidates.add(value);
        }
    }

    private static boolean isRunnable(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        try {
            CommandResult result = runCommand(Duration.ofSeconds(5), candidate, "--version");
            return result.exitCode == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    static CommandResult runCommand(Duration timeout, String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("Command timed out: " + String.join(" ", command));
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new CommandResult(process.exitValue(), output.trim());
    }

    static List<Path> javaFiles(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    static final class CommandResult {
        final int exitCode;
        final String output;

        CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    static final class GuardReport {
        private final String name;
        private final List<String> metrics = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> hardFailures = new ArrayList<>();

        GuardReport(String name) {
            this.name = name;
        }

        void metric(String key, Object value) {
            metrics.add(key + "=" + value);
        }

        void warning(String message) {
            warnings.add(message);
        }

        void fail(String message) {
            hardFailures.add(message);
        }

        void require(boolean condition, String message) {
            if (!condition) {
                fail(message);
            }
        }

        void requireContains(String text, String needle, String message) {
            require(text.contains(needle), message + " missing `" + needle + "`");
        }

        void printAndFail() {
            System.out.println("== " + name + " ==");
            System.out.println("Metrics:");
            metrics.stream().sorted().forEach(metric -> System.out.println("  " + metric));
            System.out.println("Warnings: " + warnings.size());
            warnings.stream().sorted().forEach(warning -> System.out.println("  WARNING: " + warning));
            System.out.println("Hard failures: " + hardFailures.size());
            hardFailures.stream().sorted().forEach(failure -> System.out.println("  HARD_FAIL: " + failure));
            System.out.println("Guard metrics collected: " + metrics.size());
            if (!hardFailures.isEmpty()) {
                throw new AssertionError(name + " failed with " + hardFailures.size() + " hard failure(s)");
            }
            System.out.println(name + " checks passed.");
        }
    }

    static final class MethodMetric {
        final String location;
        final String name;
        final int lines;
        final int chars;

        MethodMetric(String location, String name, int lines) {
            this(location, name, lines, 0);
        }

        MethodMetric(String location, String name, int lines, int chars) {
            this.location = location;
            this.name = name;
            this.lines = lines;
            this.chars = chars;
        }
    }

    static List<MethodMetric> collectLargeJavaMethods(Path root, int limit) throws IOException {
        Pattern methodStart = Pattern.compile("^\\s*(?:(?:public|private|protected|static|final|synchronized|native|abstract|strictfp|default)\\s+)*(?:<[\\w\\s,? extends super&.]+>\\s*)?(?:[\\w\\[\\]<>?,.]+\\s+)?(\\w+)\\s*\\([^;{}]*\\)\\s*(?:throws\\s+[\\w\\s,.$]+)?\\s*\\{\\s*$");
        List<MethodMetric> methods = new ArrayList<>();
        for (Path file : javaFiles(root.resolve("src"))) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                Matcher matcher = methodStart.matcher(lines.get(i));
                if (!matcher.matches()) {
                    continue;
                }
                String name = matcher.group(1);
                if (isControlKeyword(name)) {
                    continue;
                }
                int depth = 0;
                boolean inTextBlock = false;
                int end = i;
                for (; end < lines.size(); end++) {
                    BraceDelta delta = braceDelta(lines.get(end), inTextBlock);
                    inTextBlock = delta.inTextBlock;
                    depth += delta.delta;
                    if (end > i && depth <= 0) {
                        break;
                    }
                }
                String className = file.getFileName().toString().replace(".java", "");
                String relative = root.relativize(file).toString().replace('\\', '/');
                methods.add(new MethodMetric(relative + ":" + (i + 1), className + "." + name, end - i + 1));
                i = Math.max(i, end);
            }
        }
        return methods.stream()
                .sorted(Comparator.comparingInt((MethodMetric metric) -> metric.lines).reversed()
                        .thenComparing(metric -> metric.location))
                .limit(limit)
                .toList();
    }

    static List<MethodMetric> collectLargeJsFunctions(String appJs, int limit) {
        List<String> lines = List.of(appJs.split("\\R", -1));
        List<Pattern> startPatterns = List.of(
                Pattern.compile("^\\s*(?:async\\s+)?function\\s+([A-Za-z_$][\\w$]*)\\s*\\([^)]*\\)\\s*\\{"),
                Pattern.compile("^\\s*(?:const|let|var)\\s+([A-Za-z_$][\\w$]*)\\s*=\\s*(?:async\\s+)?function(?:\\s+[A-Za-z_$][\\w$]*)?\\s*\\([^)]*\\)\\s*\\{"),
                Pattern.compile("^\\s*(?:const|let|var)\\s+([A-Za-z_$][\\w$]*)\\s*=\\s*(?:async\\s*)?(?:\\([^)]*\\)|[A-Za-z_$][\\w$]*)\\s*=>\\s*\\{"),
                Pattern.compile("^\\s*([A-Za-z_$][\\w$]*)\\s*=\\s*(?:async\\s+)?function\\s*\\([^)]*\\)\\s*\\{")
        );
        List<JsFunctionStart> starts = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String name = jsFunctionName(lines.get(i), startPatterns);
            if (name != null) {
                starts.add(new JsFunctionStart(name, i));
            }
        }
        List<MethodMetric> functions = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            JsFunctionStart start = starts.get(i);
            int nextStart = i + 1 < starts.size() ? starts.get(i + 1).lineIndex : lines.size();
            int chars = 0;
            for (int line = start.lineIndex; line < nextStart; line++) {
                chars += lines.get(line).length() + 1;
            }
            functions.add(new MethodMetric("app.js:" + (start.lineIndex + 1), start.name, nextStart - start.lineIndex, chars));
        }
        return functions.stream()
                .sorted(Comparator.comparingInt((MethodMetric metric) -> metric.chars).reversed()
                        .thenComparing(Comparator.comparingInt((MethodMetric metric) -> metric.lines).reversed())
                        .thenComparing(metric -> metric.location))
                .limit(limit)
                .toList();
    }

    private static final class JsFunctionStart {
        final String name;
        final int lineIndex;

        JsFunctionStart(String name, int lineIndex) {
            this.name = name;
            this.lineIndex = lineIndex;
        }
    }

    private static String jsFunctionName(String line, List<Pattern> startPatterns) {
        for (Pattern pattern : startPatterns) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static boolean isControlKeyword(String name) {
        return List.of("if", "for", "while", "switch", "catch", "try", "return", "throw", "new", "else", "do").contains(name);
    }

    private static BraceDelta braceDelta(String line, boolean inTextBlock) {
        int delta = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            if (inTextBlock) {
                if (line.startsWith("\"\"\"", i)) {
                    inTextBlock = false;
                    i += 2;
                }
                continue;
            }
            if (!inString && !inChar && line.startsWith("\"\"\"", i)) {
                inTextBlock = true;
                i += 2;
                continue;
            }
            char ch = line.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (inChar) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '\'') {
                    inChar = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
            } else if (ch == '\'') {
                inChar = true;
            } else if (ch == '{') {
                delta++;
            } else if (ch == '}') {
                delta--;
            }
        }
        return new BraceDelta(delta, inTextBlock);
    }

    private static final class BraceDelta {
        final int delta;
        final boolean inTextBlock;

        BraceDelta(int delta, boolean inTextBlock) {
            this.delta = delta;
            this.inTextBlock = inTextBlock;
        }
    }
}
