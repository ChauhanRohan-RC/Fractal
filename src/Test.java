import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processing.core.PApplet;

import java.util.Arrays;

public class Test extends PApplet {

    @NotNull
    public static Complex parseComplex(@Nullable String s) throws NumberFormatException {
        if (s == null || s.isEmpty())
            return Complex.ZERO;

        s = Format.removeAllWhiteSpaces(s.trim());
        if (s.isEmpty())
            return Complex.ZERO;

        if (s.equals("i") || s.equals("+i"))
            return Complex.PLUS_I;

        if (s.equals("-i"))
            return Complex.MINUS_I;

        String[] temp;
        double res_real, res_img;
        boolean real_neg = false, img_neg = false;
        String real_str;
        String img_str;

        if (s.charAt(0) == '+' || s.charAt(0) == '-') {
            real_neg = s.charAt(0) == '-';
            s = s.substring(1);
        }

        if (s.contains("+")) {
            temp = s.split("\\+");
        } else if (s.contains("-")) {
            temp = s.split("-");
            img_neg = true;
        } else {
            if (s.contains("i")) {
                temp = new String[2];
                temp[0] = "0";
                temp[1] = s.split("i")[0];
                img_neg = real_neg;
                real_neg = false;
            } else {
                temp = new String[2];
                temp[0] = s;
                temp[1] = "0";
            }
        }


        final String[] img_tmp;
        if (temp[0].contains("i")) {
            real_str = temp[1];
            img_tmp = temp[0].split("i");
        } else {
            real_str = temp[0];
            img_tmp = temp[1].split("i");
        }

        img_str = img_tmp.length > 0? img_tmp[0]: "";

        res_real = Format.isEmpty(real_str)? 0: Double.parseDouble(real_str);
        res_img = Format.isEmpty(img_str)? 0: Double.parseDouble(img_str);

        if (real_neg)
            res_real = -res_real;
        if (img_neg)
            res_img = -res_img;
        return new Complex(res_real, res_img);
    }

    public static void main(String[] args) {

//        ComplexFormat cm = new ComplexFormat();
//        println(parseComplex(" -89 +512.891i"));
        println(parseComplex("-2i"));
//        println(Arrays.toString(" i ".split("i")));

//        final Test app = new Test();
//        PApplet.runSketch(PApplet.concat(new String[]{app.getClass().getName()}, args), app);
    }

}
