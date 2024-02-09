package learn.jni;

import java.io.IOException;

public class Sample01 {
    public native int square(int n);

    public native boolean aBool(boolean bool);

    public native String text(String text);

    public native int sum(int[] array);

    static {
        try {
            LibLoader.load("libjni-lib");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Sample01 sample01 = new Sample01();
        int square = sample01.square(5);
        boolean bool = sample01.aBool(true);
        String text = sample01.text("JAVA");
        int sum = sample01.sum(new int[] { 1, 2, 3, 4 });

        System.out.println("square: " + square);
        System.out.println("aBool: " + bool);
        System.out.println("text: " + text);
        System.out.println("sum: " + sum);
    }
}
