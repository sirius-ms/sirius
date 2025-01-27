import executable.BiotransformerExecutable3;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BiotransformerRunner {

    public static void main(String[] args) {
        System.out.println("Willkommen zur Biotransformer-Integration in SIRIUS!");

        Scanner scanner = new Scanner(System.in);

        // Schritt 1: Molekül-Eingabe
        System.out.println("Bitte geben Sie das Molekül ein (SMILES-Format, Mol-Datei oder SDF-Datei).");
        System.out.println("Geben Sie 'smi' für SMILES, 'mol' für MOL-Datei oder 'sdf' für SDF-Datei an:");
        String inputFormat = scanner.nextLine();
        String inputSource;
        if (inputFormat.equalsIgnoreCase("smi")) {
            System.out.println("Bitte geben Sie das Molekül im SMILES-Format ein:");
            inputSource = scanner.nextLine();
        } else if (inputFormat.equalsIgnoreCase("mol") || inputFormat.equalsIgnoreCase("sdf")) {
            System.out.println("Bitte geben Sie den Pfad zur Eingabedatei an:");
            inputSource = scanner.nextLine();
        } else {
            System.err.println("Ungültiges Format. Nur 'smi', 'mol' oder 'sdf' sind erlaubt.");
            return;
        }

        // Schritt 2: Aufgabe auswählen
        System.out.println("Wählen Sie eine Aufgabe aus ('pred' für Vorhersage oder 'cid' für Compound-Identifikation):");
        String task = scanner.nextLine();

        // Schritt 3: Biotransformer-Typ oder Sequenz auswählen
        System.out.println("Wählen Sie den Biotransformer-Typ ('allHuman', 'cyp450', 'ecbased', 'phaseII', 'env', 'hgut', 'superbio') oder geben Sie eine Sequenz (z.B. 'cyp450:2; phaseII:1') ein:");
        String biotransformerTypeOrSequence = scanner.nextLine();

        // Schritt 4: Zusätzliche Optionen für 'cid'
        String massesInput = "", formulasInput = "";
        if (task.equalsIgnoreCase("cid")) {
            System.out.println("Möchten Sie spezifische Massen oder Formeln zur Identifikation angeben? (ja/nein):");
            if (scanner.nextLine().equalsIgnoreCase("ja")) {
                System.out.println("Massen eingeben (durch Semikolon getrennt, z.B. '150;200'):");
                massesInput = scanner.nextLine();
                System.out.println("Alternativ: Formeln eingeben (durch Semikolon getrennt, z.B. 'C6H12O6;H2O'):");
                formulasInput = scanner.nextLine();
            }
        }

        // Schritt 5: Anzahl der Schritte (für bestimmte Aufgaben notwendig)
        int steps = 1; // Standardwert
        if (task.equalsIgnoreCase("pred")) {
            System.out.println("Bitte geben Sie die Anzahl der Schritte ein (Standard: 1):");
            steps = scanner.hasNextInt() ? scanner.nextInt() : 1;
            scanner.nextLine(); // Puffer leeren
        }

        // Schritt 6: Massen-Toleranz
        System.out.println("Geben Sie eine Massentoleranz für die Vorhersage (standardmäßig 0.01):");
        String massTolerance = scanner.nextLine();

        // Schritt 7: Ausgabe-Annotation
        System.out.println("Möchten Sie die Ergebnisse annotieren? (ja/nein):");
        boolean annotate = scanner.nextLine().equalsIgnoreCase("ja");

        // Schritt 8: Datenbanksuche aktivieren
        System.out.println("Möchten Sie die Datenbanksuche aktivieren? (ja/nein):");
        boolean useDB = scanner.nextLine().equalsIgnoreCase("ja");

        // Schritt 9: Ausgabeformat auswählen
        System.out.println("Wählen Sie das gewünschte Ausgabeformat ('csv' oder 'sdf'):");
        String outputFormat = scanner.nextLine();
        System.out.println("Bitte geben Sie den Pfad für die Ausgabedatei an:");
        String outputFile = scanner.nextLine();

        // Biotransformer mit allen Optionen starten
        String[] biotransformerArgs = prepareBiotransformerArguments(
                task, biotransformerTypeOrSequence, inputFormat, inputSource,
                outputFormat, outputFile, steps, annotate, useDB,
                massesInput, formulasInput, massTolerance
        );

        try {
            System.out.println("Starte Biotransformer mit den angegebenen Parametern...");
            BiotransformerExecutable3.main(biotransformerArgs);
        } catch (Exception e) {
            System.err.println("Fehler beim Ausführen von Biotransformer: " + e.getMessage());
        }

        System.out.println("Biotransformer-Verarbeitung abgeschlossen.");
    }

    private static String[] prepareBiotransformerArguments(
            String task,
            String biotransformerTypeOrSequence,
            String inputFormat,
            String inputSource,
            String outputFormat,
            String outputFile,
            int steps,
            boolean annotate,
            boolean useDB,
            String massesInput,
            String formulasInput,
            String massTolerance
    ) {
        List<String> args = new ArrayList<>();

        // Pflichtargumente
        args.add("-k");
        args.add(task);

        // Biotransformer-Typ oder Sequenz
        if (biotransformerTypeOrSequence.contains(":")) {
            args.add("-q");
            args.add(biotransformerTypeOrSequence);
        } else {
            args.add("-b");
            args.add(biotransformerTypeOrSequence);
        }

        // Eingabeformat verarbeiten
        switch (inputFormat.toLowerCase()) {
            case "smi":
                args.add("-ismi");
                args.add(inputSource);
                break;
            case "mol":
                args.add("-imol");
                args.add(inputSource);
                break;
            case "sdf":
                args.add("-isdf");
                args.add(inputSource);
                break;
        }

        // Ausgabeformat
        if (outputFormat.equalsIgnoreCase("csv")) {
            args.add("-ocsv");
            args.add(outputFile);
        } else {
            args.add("-osdf");
            args.add(outputFile);
        }

        // Schritte und Massentoleranz
        if (steps > 0) {
            args.add("-s");
            args.add(String.valueOf(steps));
        }
        if (!massTolerance.isEmpty()) {
            args.add("-t");
            args.add(massTolerance);
        }

        // Annotation und Datenbankoptionen
        if (annotate) {
            args.add("-a");
        }
        if (useDB) {
            args.add("-useDB");
            args.add("true");
        }

        // Massen und Formeln (falls relevant)
        if (!massesInput.isEmpty()) {
            args.add("-m");
            args.add(massesInput);
        }
        if (!formulasInput.isEmpty()) {
            args.add("-f");
            args.add(formulasInput);
        }

        return args.toArray(new String[0]);
    }
}