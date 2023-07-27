import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class test {
    public static void main(String args[]) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String path1="tests/test1.sysy";
        String path2="tests/test.ll";
        Method method=Main.class.getMethod("main", String[].class);
        method.invoke(null, (Object) new String[] { path1 , path2 });
        System.out.println("test done!");
    }
}
