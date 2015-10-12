package de.unijena.bioinf.sirius.data;

import java.io.*;
import java.util.Random;

public class GenerateMultiColumnCSV {

    public static void main(String[] args) {
        Random ra = new Random();
        try (BufferedReader reader = new BufferedReader(new FileReader("/media/Ext4_log/gnps/gnps_ms/Neuer Ordner/CCMSLIB00000001606.txt"));
             BufferedWriter writer = new BufferedWriter(new FileWriter("/media/Ext4_log/gnps/gnps_ms/Neuer Ordner/CCMSLIB00000001606_2.txt"))) {
            String temp = null;
            while ((temp = reader.readLine()) != null) {
                temp = temp.trim();
                if (temp.isEmpty()) continue;
                String[] parts = temp.split(" ");
                double column0 = ra.nextDouble() * 1000 + 100;
                double column2 = ra.nextDouble() * 1000 + 100;
                double column4 = ra.nextDouble() * 1000 + 100;
                writer.write(column0 + " " + parts[0] + " " + column2 + " " + parts[1] + " " + column4 + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
