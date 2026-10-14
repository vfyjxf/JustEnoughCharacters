package me.towdium.jecharacters.uitest;

import me.towdium.jecharacters.uitest.api.ClientScenario;
import me.towdium.jecharacters.uitest.api.ScenarioBuilder;
import me.towdium.jecharacters.uitest.api.ScenarioOptions;
import me.towdium.jecharacters.uitest.api.Session;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every {@link ClientScenario} the loaded mods declare, found by scanning their class files and read
 * back through reflection.
 *
 * <p>Discovery uses {@code ModList#getAllScanData} so a downstream mod needs no registration call,
 * but the annotation is read from the loaded class rather than from the scan's raw values: a class
 * file only records the members an annotation actually sets, so defaults would read as absent.
 *
 * <p>Not thread-safe and not meant to be: the engine calls {@link #scan()} once, on the client
 * thread, before the first scenario starts.
 */
public final class ScenarioRegistry {

    private static final Logger log = LoggerFactory.getLogger("jechtest-uitest");

    private ScenarioRegistry() {}

    /**
     * One registered scenario.
     *
     * @param name          the unique name the selection grammar matches
     * @param group         the annotation's group label, empty when unset
     * @param tags          the annotation's tags
     * @param requiresWorld whether the scenario needs a loaded world
     * @param type          the annotated class
     * @param options       the per-scenario tuning
     */
    public record Entry(
        String name,
        String group,
        List<String> tags,
        boolean requiresWorld,
        Class<?> type,
        ScenarioOptions options
    ) {

        /** Whether the annotation's type is one of the scenario's types or an enclosing one. */
        public boolean declaredBy(String modId) {
            return type.getName().contains("." + modId + ".");
        }

        /**
         * Runs the scenario against a fresh builder.
         *
         * @throws IllegalStateException when the class cannot be instantiated or does not declare
         *                               {@code public void define(ScenarioBuilder)}
         */
        public void define(ScenarioBuilder builder) {
            try {
                Object instance = type.getDeclaredConstructor().newInstance();
                type.getMethod("define", ScenarioBuilder.class).invoke(instance, builder);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw new IllegalStateException(
                    "scenario " + name + " threw while defining its steps",
                    cause == null ? e : cause
                );
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                    "scenario " + name + " needs a public no-argument constructor and "
                            + "public void define(ScenarioBuilder)",
                    e
                );
            }
        }
    }

    /** Scans every loaded mod for annotated classes, sorted by name and deduplicated. */
    public static List<Entry> scan() {
        Type annotationType = Type.getType(ClientScenario.class);
        Map<String, Entry> byName = new LinkedHashMap<>();
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
                if (!annotation.annotationType().equals(annotationType)) continue;
                Entry entry = read(annotation.memberName());
                if (entry == null) continue;
                Entry previous = byName.putIfAbsent(entry.name(), entry);
                if (previous != null) {
                    log.warn(
                        "scenario name '{}' is declared by both {} and {}; keeping the first",
                        entry.name(),
                        previous.type().getName(),
                        entry.type().getName()
                    );
                }
            }
        }
        List<Entry> entries = new ArrayList<>(byName.values());
        entries.sort(Comparator.comparing(Entry::name));
        return entries;
    }

    /** The registered scenario names, sorted. */
    public static List<String> names() {
        return scan().stream().map(Entry::name).toList();
    }

    private static @Nullable Entry read(String className) {
        Class<?> type;
        try {
            type = load(className);
        } catch (ClassNotFoundException e) {
            log.warn("annotated scenario class {} is not on the classpath any more", className, e);
            return null;
        }
        ClientScenario annotation = type.getAnnotation(ClientScenario.class);
        if (annotation == null) return null;
        return new Entry(
            annotation.name(),
            annotation.group(),
            List.of(annotation.tags()),
            annotation.requiresWorld(),
            type,
            optionsOf(annotation)
        );
    }

    private static ScenarioOptions optionsOf(ClientScenario annotation) {
        ScenarioOptions options = ScenarioOptions.defaults();
        if (annotation.guiScale() > 0) options = options.withGuiScale(annotation.guiScale());
        options = options.withHideGui(annotation.hideGui());
        if (annotation.settleMs() > 0) options = options.withSettleMs(annotation.settleMs());
        if (annotation.timeoutMs() > 0) options = options.withDefaultTimeoutMs(annotation.timeoutMs());
        if (annotation.scenarioTimeoutMs() > 0) options = options.withScenarioTimeoutMs(annotation.scenarioTimeoutMs());
        if (annotation.session() != Session.shared) options = options.withSession(annotation.session());
        return options;
    }

    private static Class<?> load(String className) throws ClassNotFoundException {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            try {
                return Class.forName(className, false, context);
            } catch (ClassNotFoundException ignored) {
                // fall through to the harness's own loader, which is the mod's loader in dev runs
            }
        }
        return Class.forName(className, false, ScenarioRegistry.class.getClassLoader());
    }
}
