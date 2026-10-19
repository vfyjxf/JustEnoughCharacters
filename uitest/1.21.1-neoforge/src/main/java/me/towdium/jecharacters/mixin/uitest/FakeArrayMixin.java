package me.towdium.jecharacters.mixin.uitest;

import me.towdium.jecharacters.uitest.match.MatchRecorder;
import me.towdium.jecharacters.utils.FakeArray;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = FakeArray.class, remap = false)
@SuppressWarnings({"PMD.UnusedPrivateMethod"})
public abstract class FakeArrayMixin {

    private static final String searchMethod = "FakeArray.search";

    @Inject(method = "add", at = @At("HEAD"))
    private void jechtestRememberKey(Object value, String key, CallbackInfo info) {
        MatchRecorder.rememberKey(value, key);
    }

    @Inject(method = "search", at = @At("RETURN"))
    private void jechtestRecordSearch(String key, CallbackInfoReturnable<List<?>> info) {
        List<?> results = info.getReturnValue();
        if (key.isEmpty() || results == null) return;
        MatchRecorder.record(searchMethod, key, names(results), !results.isEmpty());
    }

    private static @Nullable String names(List<?> results) {
        List<String> names = new ArrayList<>();
        for (Object value : results) {
            List<String> keys = MatchRecorder.keysOf(value);
            String name = keys.isEmpty() ? null : keys.get(0);
            if (name != null && !names.contains(name)) names.add(name);
            if (names.size() >= 16) break;
        }
        return names.isEmpty() ? results.size() + " result(s)" : String.join(" | ", names);
    }
}
