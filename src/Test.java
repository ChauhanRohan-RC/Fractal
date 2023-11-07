import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import processing.core.PApplet;

public class Test extends PApplet {

    @NotNull
    public static Complex parseComplex(@Nullable String s) throws NumberFormatException {
        if (s == null)
            return Complex.ZERO;

        s = Format.removeAllWhiteSpaces(s.trim());
        if (s.isEmpty())
            return Complex.ZERO;

        if (s.equals("i"))
            return Complex.I;

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

        if (temp[0].contains("i")) {
            real_str = temp[1];
            img_str = temp[0].split("i")[0];
        } else {
            real_str = temp[0];
            img_str = temp[1].split("i")[0];
        }

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
        println(parseComplex(" -89 +512.891i"));

//        final Test app = new Test();
//        PApplet.runSketch(PApplet.concat(new String[]{app.getClass().getName()}, args), app);
    }

}
