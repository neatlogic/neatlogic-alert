package neatlogic.module.alert.adaptor;

public class FullLockStateDemo {

    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    public static void main(String[] args) throws Exception {

        Thread thread1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("thread1 got lockA");
                sleep(1000);
                synchronized (lockB) {
                    System.out.println("thread1 got lockB");
                }
            }
        }, "deadlock-thread-1");

        Thread thread2 = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("thread2 got lockB");
                sleep(1000);
                synchronized (lockA) {
                    System.out.println("thread2 got lockA");
                }
            }
        }, "deadlock-thread-2");

        thread1.start();
        thread2.start();

        System.out.println("Deadlock threads started. Use jstack to inspect lock owners.");
        while (true) {
            Thread.sleep(10000);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}