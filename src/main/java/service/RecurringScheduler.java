package service;

public class RecurringScheduler implements Runnable {

    private final RecurringTaskService service =
            new RecurringTaskService();

    @Override
    public void run() {

        while (true) {

            try {
                System.out.println("Checking recurring tasks...");

                service.generateAll();

                // run every 30 seconds
                Thread.sleep(30_000);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
