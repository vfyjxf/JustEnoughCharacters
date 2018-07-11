import me.towdium.jecharacters.JechConfig;
import me.towdium.jecharacters.util.Keyboard;
import me.towdium.jecharacters.util.StringMatcher;
import org.junit.jupiter.api.Test;

public class JechTest {
    @Test
    public void quanpin1() {
        assert StringMatcher.checkStr("测试文本", "ceshiwenben");
    }

    @Test
    public void quanpin2() {
        assert StringMatcher.checkStr("测试文本", "ceshiwenbe");
    }

    @Test
    public void quanpin3() {
        assert StringMatcher.checkStr("测试文本", "ceshiwben");
    }

    @Test
    public void quanpin4() {
        assert StringMatcher.checkStr("测试文本", "ce4shi4w2ben");
    }

    @Test
    public void quanpin5() {
        assert !StringMatcher.checkStr("测试文本", "ce2shi4w2ben");
    }

    @Test
    public void quanpin6() {
        assert !StringMatcher.checkStr("石台阶", "yj");
    }


    @Test
    public void daqian1() {
        JechConfig.keyboard = Keyboard.DAQIAN;
        StringMatcher.refresh();
        assert StringMatcher.checkStr("测试文本", "hk4g4jp61p3");
    }

    @Test
    public void daqian2() {
        JechConfig.keyboard = Keyboard.DAQIAN;
        StringMatcher.refresh();
        assert StringMatcher.checkStr("测试文本", "hkgjp1");
    }

    @Test
    public void daqian3() {
        JechConfig.keyboard = Keyboard.DAQIAN;
        StringMatcher.refresh();
        assert StringMatcher.checkStr("红石", "cj/2g");
    }
}
