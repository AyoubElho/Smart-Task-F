package mail;

import java.util.Random;

public class CodeGenerator {

    public static String generate() {
        Random r = new Random();
        return String.valueOf(100000 + r.nextInt(900000)); // 6 digit
    }
}
