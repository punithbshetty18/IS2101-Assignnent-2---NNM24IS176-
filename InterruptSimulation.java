package fileee;


import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

enum Device {
    KEYBOARD(1), MOUSE(2), PRINTER(3);
    int priority;
    Device(int priority) { this.priority = priority; }
}

class InterruptController {
    private final Map<Device, Boolean> mask = new HashMap<>();
    private final List<Device> queue = new ArrayList<>();
    private final List<String> log = new ArrayList<>();

    public InterruptController() {
        for (Device d : Device.values()) mask.put(d, false);
    }

    public synchronized void triggerInterrupt(Device device) {
        if (mask.get(device)) {
            System.out.println("[IGNORED] " + device + " interrupt masked.");
        } else {
            System.out.println("[TRIGGERED] " + device + " interrupt received.");
            queue.add(device);
            queue.sort(Comparator.comparingInt(d -> d.priority));
            notify();
        }
    }

    public void setMask(Device device, boolean status) {
        mask.put(device, status);
        System.out.println("[MASK STATUS] " + device + " → " + (status ? "MASKED" : "UNMASKED"));
    }

    public void startISRHandler() {
        Thread handlerThread = new Thread(() -> {
            while (true) {
                Device device = null;
                synchronized (this) {
                    while (queue.isEmpty()) {
                        try { wait(); } catch (InterruptedException e) { return; }
                    }
                    device = queue.remove(0);
                }
                handleISR(device);
            }
        });
        handlerThread.setDaemon(true);
        handlerThread.start();
    }

    private void handleISR(Device device) {
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[ISR START] Handling " + device + " interrupt at " + startTime);
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        String endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[ISR COMPLETE] " + device + " processed successfully at " + endTime);
        log.add(endTime + " → " + device + " interrupt handled.");
    }

    public void printLog() {
        System.out.println("\n========== ISR EXECUTION LOG ==========");
        for (String entry : log) {
            System.out.println(entry);
        }
        System.out.println("=======================================");
    }
}

public class InterruptSimulation {
    public static void main(String[] args) throws InterruptedException {
        InterruptController ic = new InterruptController();
        ic.startISRHandler();

        Device[] devices = Device.values();
        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            Device randomDevice = devices[rand.nextInt(devices.length)];
            ic.triggerInterrupt(randomDevice);
            Thread.sleep(1000);
        }

        Thread.sleep(3000);
        ic.printLog();
    }
}
