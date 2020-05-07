package de.unijena.bioinf.utils.systemInfo;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.09.16.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;

import javax.swing.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SystemInformation {
    public static void main(String[] args) {
        getTMPSystemInformationFile();
    }

    public static File getTMPSystemInformationFile() {
        try {
            File systemInfoFile = File.createTempFile("system", ".info");
            writeSystemInformationTo(new FileOutputStream(systemInfoFile));
            return systemInfoFile;
        } catch (IOException e) {
            LoggerFactory.getLogger(SystemInformation.class).error("Could not write System info File", e);
            return null;
        }
    }

    public static SwingWorker<Integer, String> writeSystemInformationTo(final OutputStream stream) throws IOException {

        final SwingWorker<Integer, String> s = new SystemInfoCollector(stream);
        s.execute();
        return s;
    }

    private static class SystemInfoCollector extends SwingWorker<Integer, String> {
        final Logger LOG = LoggerFactory.getLogger(this.getClass());
        final OutputStream stream;

        private SystemInfoCollector(OutputStream stream) {
            this.stream = stream;
        }

        private void publishAndWarn(final String m) {
            LOG.warn(m);
            publish(m);
        }

        private void publishAndError(final String m, Throwable e) {
            LOG.error(m, e);
            publish(m);
        }

        private void publishAndInfo(final String m) {
            LOG.info(m);
            publish(m);
        }

        @Override
        protected Integer doInBackground() throws Exception {
            try (OutputStreamWriter outputStream = new OutputStreamWriter(stream)) {
                setProgress(0);
                publishAndInfo("Initializing System...");
                SystemInfo si = new SystemInfo();

                HardwareAbstractionLayer hal = si.getHardware();
                setProgress(2);

                publishAndInfo("OS...");
                OperatingSystem os = si.getOperatingSystem();
                outputStream.write("Operating System:");
                outputStream.write(System.lineSeparator());
                outputStream.write(os.toString());
                outputStream.write(System.lineSeparator());
                outputStream.write(System.lineSeparator());

                outputStream.write("JRE/JDK:");
                outputStream.write(System.lineSeparator());
                outputStream.write(System.getProperty("java.version"));
                outputStream.write(System.lineSeparator());
                outputStream.write(System.lineSeparator());

                outputStream.write("Paths: ");
                outputStream.write(System.lineSeparator());
                outputStream.write("GUROBI_HOME = " + System.getenv("$GUROBI_HOME"));
                outputStream.write(System.lineSeparator());
                outputStream.write("java.library.path = " + System.getProperty("java.library.path"));
                outputStream.write(System.lineSeparator());
                outputStream.write("LD_LIBRARY_PATH = " + System.getenv("LD_LIBRARY_PATH"));
                outputStream.write(System.lineSeparator());
                outputStream.write("java.class.path = " + System.getProperty("java.class.path"));
                outputStream.write(System.lineSeparator());
                outputStream.write("USER_HOME = " + System.getProperty("user.home"));
                outputStream.write(System.lineSeparator());
                outputStream.write("USER_DIR = " + System.getProperty("user.dir"));
//            outputStream.write(System.lineSeparator());
//            outputStream.write("APP_DIR = " + ApplicationCore.class.getProtectionDomain().getCodeSource().getLocation().toString());
                outputStream.write(System.lineSeparator());
                outputStream.write(System.lineSeparator());
                setProgress(5);

                publishAndInfo("CPU...");
                printProcessor(hal.getProcessor(), outputStream);
                outputStream.write(System.lineSeparator());
                setProgress(10);

                publishAndInfo("Checking Memory...");
                printMemory(hal.getMemory(), outputStream);
                outputStream.write(System.lineSeparator());
                setProgress(20);
//            publishAndInfo("Checking CPU...");
//            printCpu(hal.getProcessor(),outputStream);
//            outputStream.write(System.lineSeparator());
                setProgress(25);

                publishAndInfo("Checking Processes...");
                printProcesses(os, hal.getMemory(), outputStream);
                outputStream.write(System.lineSeparator());
                setProgress(30);

                publishAndInfo("Checking Sensors...");
                printSensors(hal.getSensors(), outputStream);
                outputStream.write(System.lineSeparator());
                setProgress(40);

                publishAndInfo("Checking Power sources...");
                printPowerSources(hal.getPowerSources(), outputStream);
                outputStream.write(System.lineSeparator());
                setProgress(50);

                publishAndInfo("Checking Disks...");
                printDisks(hal.getDiskStores(), outputStream);
                outputStream.write(System.lineSeparator());
                setProgress(60);

                publishAndInfo("Checking File System...");
                printFileSystem(os.getFileSystem(), outputStream);
                outputStream.write(System.lineSeparator());
                setProgress(70);

                publishAndInfo("Checking Network interfaces...");
                printNetworkInterfaces(hal.getNetworkIFs(), outputStream);
                outputStream.write(System.lineSeparator());
                setProgress(80);

                // hardware: displays
                publishAndInfo("Checking Displays...");
                printDisplays(hal.getDisplays(), outputStream);
                outputStream.write(System.lineSeparator());
                setProgress(90);

                // hardware: USB devices
                publishAndInfo("Checking USB Devices...");
                printUsbDevices(hal.getUsbDevices(true), outputStream);
                setProgress(99);

                outputStream.flush();
                outputStream.close();

                setProgress(100);
            } catch (IOException e) {
                publishAndError("Could not write System information", e);
                return 1;
            }
            return 0;
        }
    }

    private static void printProcessor(CentralProcessor processor, OutputStreamWriter outputStream) throws IOException {
        outputStream.write("CPU:");
        outputStream.write(System.lineSeparator());
        outputStream.write(processor.toString());
        outputStream.write(System.lineSeparator());
        outputStream.write(" " + processor.getPhysicalProcessorCount() + " physical CPU(s)");
        outputStream.write(System.lineSeparator());
        outputStream.write(" " + processor.getLogicalProcessorCount() + " logical CPU(s)");
        outputStream.write(System.lineSeparator());

        outputStream.write("Identifier: " + processor.getIdentifier());
        outputStream.write(System.lineSeparator());
        outputStream.write("Serial Num: " + processor.getProcessorID());
        outputStream.write(System.lineSeparator());
    }

    private static void printMemory(GlobalMemory memory, OutputStreamWriter outputStream) throws IOException {
        outputStream.write("Memory: " + FormatUtil.formatBytes(memory.getAvailable()) + "/"
                + FormatUtil.formatBytes(memory.getTotal()));
        outputStream.write(System.lineSeparator());
//        outputStream.write("Swap used: " + FormatUtil.formatBytes(memory.getSwapUsed()) + "/"
//                + FormatUtil.formatBytes(memory.getSwapTotal()));
//        outputStream.write(System.lineSeparator());
    }

   /* private static void printCpu(CentralProcessor processor, OutputStreamWriter outputStream) throws IOException {
//        outputStream.write("Uptime: " + FormatUtil.formatElapsedSecs(processor.getS));
//        outputStream.write(System.lineSeparator());

        long[] prevTicks = processor.getSystemCpuLoadTicks();
        outputStream.write("CPU, IOWait, and IRQ ticks @ 0 sec:" + Arrays.toString(prevTicks));
        outputStream.write(System.lineSeparator());
        // Wait a second...
        Util.sleep(1000);
        long[] ticks = processor.getSystemCpuLoadTicks();
        outputStream.write("CPU, IOWait, and IRQ ticks @ 1 sec:" + Arrays.toString(ticks));
        outputStream.write(System.lineSeparator());

//        long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
//        long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
//        long sys = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
//        long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
//        long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
//        long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
//        long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
//        long totalCpu = user + nice + sys + idle + iowait + irq + softirq;

//        outputStream.write(String.format(
//                "User: %.1f%% Nice: %.1f%% System: %.1f%% Idle: %.1f%% IOwait: %.1f%% IRQ: %.1f%% SoftIRQ: %.1f%%%n",
//                100d * user / totalCpu, 100d * nice / totalCpu, 100d * sys / totalCpu, 100d * idle / totalCpu,
//                100d * iowait / totalCpu, 100d * irq / totalCpu, 100d * softirq / totalCpu));
//        outputStream.write(String.format("CPU load: %.1f%% (counting ticks)%n", processor.getSystemCpuLoadBetweenTicks() * 100));
//        outputStream.write(String.format("CPU load: %.1f%% (OS MXBean)%n", processor.geS * 100));
        double[] loadAverage = processor.getSystemLoadAverage(3);
        outputStream.write("CPU load averages:" + (loadAverage[0] < 0 ? " N/A" : String.format(" %.2f", loadAverage[0]))
                + (loadAverage[1] < 0 ? " N/A" : String.format(" %.2f", loadAverage[1]))
                + (loadAverage[2] < 0 ? " N/A" : String.format(" %.2f", loadAverage[2])));

        outputStream.write(System.lineSeparator());
        // per core CPU
        StringBuilder procCpu = new StringBuilder("CPU load per processor:");
        double[] load = processor.getProcessorCpuLoadBetweenTicks();
        for (double avg : load) {
            procCpu.append(String.format(" %.1f%%", avg * 100));
        }
        outputStream.write(procCpu.toString());
        outputStream.write(System.lineSeparator());
    }*/

    private static void printProcesses(OperatingSystem os, GlobalMemory memory, OutputStreamWriter outputStream) throws IOException {
        outputStream.write("Processes: " + os.getProcessCount() + ", Threads: " + os.getThreadCount());
        outputStream.write(System.lineSeparator());
        // Sort by highest CPU
        List<OSProcess> procs = Arrays.asList(os.getProcesses(5, OperatingSystem.ProcessSort.CPU));

        outputStream.write("   PID  %CPU %MEM       VSZ       RSS Name");
        outputStream.write(System.lineSeparator());
        for (int i = 0; i < procs.size() && i < 5; i++) {
            OSProcess p = procs.get(i);
            outputStream.write(String.format(" %5d %5.1f %4.1f %9s %9s %s%n", p.getProcessID(),
                    100d * (p.getKernelTime() + p.getUserTime()) / p.getUpTime(),
                    100d * p.getResidentSetSize() / memory.getTotal(), FormatUtil.formatBytes(p.getVirtualSize()),
                    FormatUtil.formatBytes(p.getResidentSetSize()), p.getName()));
        }
    }

    private static void printSensors(Sensors sensors, OutputStreamWriter outputStream) throws IOException {
        outputStream.write("Sensors:");
        outputStream.write(System.lineSeparator());
        outputStream.write(String.format(" CPU Temperature: %.1f°C%n", sensors.getCpuTemperature()));
        outputStream.write(" Fan Speeds: " + Arrays.toString(sensors.getFanSpeeds()));
        outputStream.write(System.lineSeparator());
        outputStream.write(String.format(" CPU Voltage: %.1fV%n", sensors.getCpuVoltage()));
        outputStream.write(System.lineSeparator());
    }

    private static void printPowerSources(PowerSource[] powerSources, OutputStreamWriter outputStream) throws IOException {
        StringBuilder sb = new StringBuilder("Power: ");
        if (powerSources.length == 0) {
            sb.append("Unknown");
        } else {
            double timeRemaining = powerSources[0].getTimeRemaining();
            if (timeRemaining < -1d) {
                sb.append("Charging");
            } else if (timeRemaining < 0d) {
                sb.append("Calculating time remaining");
            } else {
                sb.append(String.format("%d:%02d remaining", (int) (timeRemaining / 3600),
                        (int) (timeRemaining / 60) % 60));
            }
        }
        for (PowerSource pSource : powerSources) {
            sb.append(String.format("%n %s @ %.1f%%", pSource.getName(), pSource.getRemainingCapacity() * 100d));
        }
        outputStream.write(sb.toString());
        outputStream.write(System.lineSeparator());
    }

    private static void printDisks(HWDiskStore[] diskStores, OutputStreamWriter outputStream) throws IOException {
        outputStream.write("Disks:");
        outputStream.write(System.lineSeparator());
        for (HWDiskStore disk : diskStores) {
            boolean readwrite = disk.getReads() > 0 || disk.getWrites() > 0;
            outputStream.write(String.format(" %s: (model: %s - S/N: %s) size: %s, reads: %s (%s), writes: %s (%s), xfer: %s ms%n",
                    disk.getName(), disk.getModel(), disk.getSerial(),
                    disk.getSize() > 0 ? FormatUtil.formatBytesDecimal(disk.getSize()) : "?",
                    readwrite ? disk.getReads() : "?", readwrite ? FormatUtil.formatBytes(disk.getReadBytes()) : "?",
                    readwrite ? disk.getWrites() : "?", readwrite ? FormatUtil.formatBytes(disk.getWriteBytes()) : "?",
                    readwrite ? disk.getTransferTime() : "?"));
            HWPartition[] partitions = disk.getPartitions();
            if (partitions == null) {
                // TODO Remove when all OS's implemented
                continue;
            }
            for (HWPartition part : partitions) {
                outputStream.write(String.format(" |-- %s: %s (%s) Maj:Min=%d:%d, size: %s%s%n", part.getIdentification(),
                        part.getName(), part.getType(), part.getMajor(), part.getMinor(),
                        FormatUtil.formatBytesDecimal(part.getSize()),
                        part.getMountPoint().isEmpty() ? "" : " @ " + part.getMountPoint()));
            }
        }
    }

    private static void printFileSystem(FileSystem fileSystem, OutputStreamWriter outputStream) throws IOException {
        outputStream.write("File System:");
        outputStream.write(System.lineSeparator());

        outputStream.write(String.format(" File Descriptors: %d/%d%n", fileSystem.getOpenFileDescriptors(),
                fileSystem.getMaxFileDescriptors()));

        OSFileStore[] fsArray = fileSystem.getFileStores();
        for (OSFileStore fs : fsArray) {
            long usable = fs.getUsableSpace();
            long total = fs.getTotalSpace();
            outputStream.write(String.format(" %s (%s) [%s] %s of %s free (%.1f%%) is %s and is mounted at %s%n", fs.getName(),
                    fs.getDescription().isEmpty() ? "file system" : fs.getDescription(), fs.getType(),
                    FormatUtil.formatBytes(usable), FormatUtil.formatBytes(fs.getTotalSpace()), 100d * usable / total,
                    fs.getVolume(), fs.getMount()));
        }
    }

    private static void printNetworkInterfaces(NetworkIF[] networkIFs, OutputStreamWriter outputStream) throws IOException {
        outputStream.write("Network interfaces:");
        outputStream.write(System.lineSeparator());

        for (NetworkIF net : networkIFs) {
            outputStream.write(String.format(" Name: %s (%s)%n", net.getName(), net.getDisplayName()));
            outputStream.write(String.format("   MAC Address: %s %n", net.getMacaddr()));
            outputStream.write(String.format("   MTU: %s, Speed: %s %n", net.getMTU(), FormatUtil.formatValue(net.getSpeed(), "bps")));
            outputStream.write(String.format("   IPv4: %s %n", Arrays.toString(net.getIPv4addr())));
            outputStream.write(String.format("   IPv6: %s %n", Arrays.toString(net.getIPv6addr())));
            boolean hasData = net.getBytesRecv() > 0 || net.getBytesSent() > 0 || net.getPacketsRecv() > 0
                    || net.getPacketsSent() > 0;
            outputStream.write(String.format("   Traffic: received %s/%s; transmitted %s/%s %n",
                    hasData ? net.getPacketsRecv() + " packets" : "?",
                    hasData ? FormatUtil.formatBytes(net.getBytesRecv()) : "?",
                    hasData ? net.getPacketsSent() + " packets" : "?",
                    hasData ? FormatUtil.formatBytes(net.getBytesSent()) : "?"));
        }
    }

    private static void printDisplays(Display[] displays, OutputStreamWriter outputStream) throws IOException {
        outputStream.write("Displays:");
        outputStream.write(System.lineSeparator());
        int i = 0;
        for (Display display : displays) {
            outputStream.write(" Display " + i + ":");
            outputStream.write(System.lineSeparator());
            outputStream.write(display.toString());
            outputStream.write(System.lineSeparator());
            i++;
        }
    }

    private static void printUsbDevices(UsbDevice[] usbDevices, OutputStreamWriter outputStream) throws IOException {
        outputStream.write("USB Devices:");
        outputStream.write(System.lineSeparator());
        for (UsbDevice usbDevice : usbDevices) {
            outputStream.write(usbDevice.toString());
            outputStream.write(System.lineSeparator());
        }
    }

    public static SystemKey generateSystemKey() {
        return new SystemKey();
    }

    public static String getSystemHexHash() {
        return generateSystemKey().getSystemHexHash();
    }

   /* public static String getInstanceHexHash() {
        return generateSystemKey().getInstanceHexHash();
    }*/

    private static class SystemKey {
        //system hash fields
        public final String cpuID;
        public final Set<String> macs;
        public final String systemSerial;
        public final String baseBoardSerial;
        public final String hostname;

        //additional instance hash field
        public final String processID;

        public final long systemHash;
//        public final long instanceHash;

        public SystemKey() {
            HardwareAbstractionLayer ha = new SystemInfo().getHardware();
            cpuID = ha.getProcessor().getProcessorID();

            Set<String> macsTm = new HashSet<>();
            for (NetworkIF netIF : ha.getNetworkIFs()) {
                macsTm.add(netIF.getMacaddr());
            }
            macs = Collections.unmodifiableSet(macsTm);

            ComputerSystem system = ha.getComputerSystem();
            systemSerial = system.getSerialNumber();
            baseBoardSerial = system.getBaseboard().getSerialNumber();

            String h = "unknown";
            try {
                h = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LoggerFactory.getLogger(this.getClass()).warn("Could not find a valid hostname");
            }
            hostname = h;

            processID = ManagementFactory.getRuntimeMXBean().getName();

            systemHash = makeSystemHash();
//            instanceHash = makeInstanceHash();
        }

        public String getSystemHexHash() {
            return Long.toHexString(systemHash);
        }

        /*public String getInstanceHexHash() {
            return Long.toHexString(instanceHash);
        }*/

        private long makeSystemHash() {
            long result = cpuID != null ? cpuID.hashCode() : 0;
            result = 31 * result + (macs != null ? macs.hashCode() : 0);
            result = 31 * result + (systemSerial != null ? systemSerial.hashCode() : 0);
            result = 31 * result + (baseBoardSerial != null ? baseBoardSerial.hashCode() : 0);
            result = 31 * result + hostname.hashCode();
            return result;
        }


       /* private long makeInstanceHash() {
            long result = systemHash;
            result = 31 * result + (processID != null ? processID.hashCode() : 0);
            result = 31 * result + ;

            return result;
        }*/
    }
}
