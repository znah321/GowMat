package interpreter;

import com.sun.xml.internal.ws.api.ha.StickyFeature;

import java.util.regex.Pattern;

public class testclass {
    public static void main(String[] args) {
        String regex = "a";
        System.out.println(Pattern.matches("\".*", "\"aaa "));
    }
}
