package de.unijena.bioinf.sirius.core.errorReporting;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.09.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.hardware.CentralProcessor.TickType;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import oshi.util.Util;

import javax.tools.JavaFileManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SystemInformation {

    public static void main(String[] args){
        getTMPSystemInformationFile();
    }

    public static File getTMPSystemInformationFile() {
        try {
            File systemInfoFile = File.createTempFile("system", ".info");
            writeSystemInformationFile(systemInfoFile);
            return systemInfoFile;
        } catch (IOException e) {
            LoggerFactory.getLogger(SystemInformation.class).error("Could not write System info File",e);
            return null;
        }
    }

    public static void writeSystemInformationFile(File systemInfoFile) throws IOException {
        Logger LOG = LoggerFactory.getLogger(SystemInformation.class);

        try (BufferedWriter outputStream  = new BufferedWriter(new FileWriter(systemInfoFile))) {

            LOG.info("Initializing System...");
            SystemInfo si = new SystemInfo();

            HardwareAbstractionLayer hal = si.getHardware();

            LOG.info("OS...");
            OperatingSystem os = si.getOperatingSystem();
            outputStream.write("Operating System:");
            outputStream.newLine();
            outputStream.write(os.toString());
            outputStream.newLine();
            outputStream.newLine();

            outputStream.write("JRE/JDK:");
            outputStream.newLine();
            outputStream.write(System.getProperty("java.version"));
            outputStream.newLine();
            outputStream.newLine();

            outputStream.write("Paths: ");
            outputStream.newLine();
            outputStream.write("GUROBI_HOME = " + System.getenv("$GUROBI_HOME"));
            outputStream.newLine();
            outputStream.write("java.library.path = " + System.getProperty("java.library.path"));
            outputStream.newLine();
            outputStream.write("LD_LIBRARY_PATH = " + System.getenv("LD_LIBRARY_PATH"));
            outputStream.newLine();
            outputStream.write("java.class.path = " + System.getProperty("java.class.path"));
            outputStream.newLine();
            outputStream.write("USER_HOME = " + System.getProperty("user.home"));
            outputStream.newLine();
            outputStream.write("USER_DIR = " + System.getProperty("user.dir"));
//            outputStream.newLine();
//            outputStream.write("APP_DIR = " + ApplicationCore.class.getProtectionDomain().getCodeSource().getLocation().toString());
            outputStream.newLine();
            outputStream.newLine();



            LOG.info("CPU...");
            printProcessor(hal.getProcessor(),outputStream);
            outputStream.newLine();

            LOG.info("Checking Memory...");
            printMemory(hal.getMemory(),outputStream);
            outputStream.newLine();

//            LOG.info("Checking CPU...");
//            printCpu(hal.getProcessor(),outputStream);
//            outputStream.newLine();

            LOG.info("Checking Processes...");
            printProcesses(os, hal.getMemory(),outputStream);
            outputStream.newLine();

            LOG.info("Checking Sensors...");
            printSensors(hal.getSensors(),outputStream);
            outputStream.newLine();

            LOG.info("Checking Power sources...");
            printPowerSources(hal.getPowerSources(),outputStream);
            outputStream.newLine();

            LOG.info("Checking Disks...");
            printDisks(hal.getDiskStores(),outputStream);
            outputStream.newLine();

            LOG.info("Checking File System...");
            printFileSystem(os.getFileSystem(),outputStream);
            outputStream.newLine();

            LOG.info("Checking Network interfaces...");
            printNetworkInterfaces(hal.getNetworkIFs(),outputStream);
            outputStream.newLine();

            // hardware: displays
            LOG.info("Checking Displays...");
            printDisplays(hal.getDisplays(),outputStream);
            outputStream.newLine();

            // hardware: USB devices
            LOG.info("Checking USB Devices...");
            printUsbDevices(hal.getUsbDevices(true),outputStream);

            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            throw e;
        }


    }

    private static void printProcessor(CentralProcessor processor,BufferedWriter outputStream) throws IOException {
        outputStream.write("CPU:");
        outputStream.newLine();
        outputStream.write(processor.toString());
        outputStream.newLine();
        outputStream.write(" " + processor.getPhysicalProcessorCount() + " physical CPU(s)");
        outputStream.newLine();
        outputStream.write(" " + processor.getLogicalProcessorCount() + " logical CPU(s)");
        outputStream.newLine();

        outputStream.write("Identifier: " + processor.getIdentifier());
        outputStream.newLine();
        outputStream.write("Serial Num: " + processor.getSystemSerialNumber());
        outputStream.newLine();
    }

    private static void printMemory(GlobalMemory memory,BufferedWriter outputStream) throws IOException {
        outputStream.write("Memory: " + FormatUtil.formatBytes(memory.getAvailable()) + "/"
                + FormatUtil.formatBytes(memory.getTotal()));
        outputStream.newLine();
        outputStream.write("Swap used: " + FormatUtil.formatBytes(memory.getSwapUsed()) + "/"
                + FormatUtil.formatBytes(memory.getSwapTotal()));
        outputStream.newLine();
    }

    private static void printCpu(CentralProcessor processor,BufferedWriter outputStream) throws IOException {
        outputStream.write("Uptime: " + FormatUtil.formatElapsedSecs(processor.getSystemUptime()));
        outputStream.newLine();

        long[] prevTicks = processor.getSystemCpuLoadTicks();
        outputStream.write("CPU, IOWait, and IRQ ticks @ 0 sec:" + Arrays.toString(prevTicks));
        outputStream.newLine();
        // Wait a second...
        Util.sleep(1000);
        long[] ticks = processor.getSystemCpuLoadTicks();
        outputStream.write("CPU, IOWait, and IRQ ticks @ 1 sec:" + Arrays.toString(ticks));
        outputStream.newLine();

        long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
        long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
        long sys = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
        long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
        long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
        long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
        long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
        long totalCpu = user + nice + sys + idle + iowait + irq + softirq;

        outputStream.write(String.format(
                "User: %.1f%% Nice: %.1f%% System: %.1f%% Idle: %.1f%% IOwait: %.1f%% IRQ: %.1f%% SoftIRQ: %.1f%%%n",
                100d * user / totalCpu, 100d * nice / totalCpu, 100d * sys / totalCpu, 100d * idle / totalCpu,
                100d * iowait / totalCpu, 100d * irq / totalCpu, 100d * softirq / totalCpu));
        outputStream.write(String.format("CPU load: %.1f%% (counting ticks)%n", processor.getSystemCpuLoadBetweenTicks() * 100));
        outputStream.write(String.format("CPU load: %.1f%% (OS MXBean)%n", processor.getSystemCpuLoad() * 100));
        double[] loadAverage = processor.getSystemLoadAverage(3);
        outputStream.write("CPU load averages:" + (loadAverage[0] < 0 ? " N/A" : String.format(" %.2f", loadAverage[0]))
                + (loadAverage[1] < 0 ? " N/A" : String.format(" %.2f", loadAverage[1]))
                + (loadAverage[2] < 0 ? " N/A" : String.format(" %.2f", loadAverage[2])));

        outputStream.newLine();
        // per core CPU
        StringBuilder procCpu = new StringBuilder("CPU load per processor:");
        double[] load = processor.getProcessorCpuLoadBetweenTicks();
        for (double avg : load) {
            procCpu.append(String.format(" %.1f%%", avg * 100));
        }
        outputStream.write(procCpu.toString());
        outputStream.newLine();
    }

    private static void printProcesses(OperatingSystem os, GlobalMemory memory,BufferedWriter outputStream) throws IOException {
        outputStream.write("Processes: " + os.getProcessCount() + ", Threads: " + os.getThreadCount());
        outputStream.newLine();
        // Sort by highest CPU
        List<OSProcess> procs = Arrays.asList(os.getProcesses(5, OperatingSystem.ProcessSort.CPU));

        outputStream.write("   PID  %CPU %MEM       VSZ       RSS Name");
        outputStream.newLine();
        for (int i = 0; i < procs.size() && i < 5; i++) {
            OSProcess p = procs.get(i);
            outputStream.write(String.format(" %5d %5.1f %4.1f %9s %9s %s%n", p.getProcessID(),
                    100d * (p.getKernelTime() + p.getUserTime()) / p.getUpTime(),
                    100d * p.getResidentSetSize() / memory.getTotal(), FormatUtil.formatBytes(p.getVirtualSize()),
                    FormatUtil.formatBytes(p.getResidentSetSize()), p.getName()));
        }
    }

    private static void printSensors(Sensors sensors,BufferedWriter outputStream) throws IOException {
        outputStream.write("Sensors:");
        outputStream.newLine();
        outputStream.write(String.format(" CPU Temperature: %.1f°C%n", sensors.getCpuTemperature()));
        outputStream.write(" Fan Speeds: " + Arrays.toString(sensors.getFanSpeeds()));
        outputStream.newLine();
        outputStream.write(String.format(" CPU Voltage: %.1fV%n", sensors.getCpuVoltage()));
        outputStream.newLine();
    }

    private static void printPowerSources(PowerSource[] powerSources,BufferedWriter outputStream) throws IOException {
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
        outputStream.newLine();
    }

    private static void printDisks(HWDiskStore[] diskStores,BufferedWriter outputStream) throws IOException {
        outputStream.write("Disks:");
        outputStream.newLine();
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

    private static void printFileSystem(FileSystem fileSystem,BufferedWriter outputStream) throws IOException {
        outputStream.write("File System:");
        outputStream.newLine();

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

    private static void printNetworkInterfaces(NetworkIF[] networkIFs,BufferedWriter outputStream) throws IOException {
        outputStream.write("Network interfaces:");
        outputStream.newLine();

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

    private static void printDisplays(Display[] displays,BufferedWriter outputStream) throws IOException {
        outputStream.write("Displays:");
        outputStream.newLine();
        int i = 0;
        for (Display display : displays) {
            outputStream.write(" Display " + i + ":");
            outputStream.newLine();
            outputStream.write(display.toString());
            outputStream.newLine();
            i++;
        }
    }

    private static void printUsbDevices(UsbDevice[] usbDevices,BufferedWriter outputStream) throws IOException {
        outputStream.write("USB Devices:");
        outputStream.newLine();
        for (UsbDevice usbDevice : usbDevices) {
            outputStream.write(usbDevice.toString());
            outputStream.newLine();
        }
    }

}
