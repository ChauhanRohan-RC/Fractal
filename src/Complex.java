import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Complex {

    /**
     * The square root of -1. A number representing "0.0 + 1.0i"
     */
    public static final Complex I = new Complex(0.0, 1.0);

    /**
     * A complex number representing "NaN + NaNi"
     */
    public static final Complex NaN = new Complex(Double.NaN, Double.NaN);

    /**
     * A complex number representing "+INF + INFi"
     */
    public static final Complex INF = new Complex(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    /**
     * A complex number representing "1.0 + 0.0i"
     */
    public static final Complex ONE = new Complex(1.0, 0.0);

    /**
     * A complex number representing "0.0 + 0.0i"
     */
    public static final Complex ZERO = new Complex(0.0, 0.0);

    @NotNull
    public static Complex polar(double r, double theta) {
        return new Complex(r * Math.cos(theta), r * Math.sin(theta));
    }



    public final double re;
    public final double img;

    public Complex(double re, double img) {
        this.re = re;
        this.img = img;
    }

    public double modSq() {
        return re * re + img * img;
    }

    @NotNull
    public Complex add(@NotNull Complex c) {
        return new Complex(re + c.re, img + c.img);
    }

    @Override
    public String toString() {
        return String.format("%f %+fi", re, img);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Complex complex = (Complex) o;
        return Double.compare(re, complex.re) == 0 && Double.compare(img, complex.img) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(re) + Double.hashCode(img);
    }




    @NotNull
    public static Complex parse(@Nullable String s) throws NumberFormatException {
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
}
