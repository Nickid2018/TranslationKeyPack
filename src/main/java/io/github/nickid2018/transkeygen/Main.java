package io.github.nickid2018.transkeygen;

import joptsimple.*;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        OptionParser optionParser = new OptionParser();
        ArgumentAcceptingOptionSpec<String> input = optionParser
                .accepts("minecraft", "Input a minecraft jar").withRequiredArg().ofType(String.class);
        ArgumentAcceptingOptionSpec<String> output = optionParser
                .accepts("output", "Output resource pack").withRequiredArg()
                .defaultsTo("Translation Key.zip").ofType(String.class);
        OptionSpecBuilder includeAfter = optionParser
                .accepts("include-after", "Resource pack can be used after this version");
        OptionSpec<?> help = optionParser.accepts("help", "Show this help").forHelp();

        OptionSet optionSet = optionParser.parse(args);
        if (optionSet.has(help)) {
            optionParser.printHelpOn(System.out);
            return;
        }

        String minecraftJarLoc = optionSet.valueOf(input);
        String outputLoc = optionSet.valueOf(output);
        boolean includeAfterVersion = optionSet.has(includeAfter);

        if (minecraftJarLoc == null) {
            System.err.println("Please specify the minecraft jar location.");
            optionParser.printHelpOn(System.err);
            return;
        }

        try (MinecraftDataReader reader = new MinecraftDataReader(minecraftJarLoc);
             ResourcePackExport export = new ResourcePackExport(outputLoc)) {
            int resourcePackVersion = reader.readResourcePackVersion();
            export.generateMetaInfo(resourcePackVersion, includeAfterVersion);
            export.addLanguageFile(reader.parseTranslations());
            System.out.println("Resource pack generated successfully.");
        } catch (IOException e) {
            System.err.println("An unexpected error occurred.");
            e.printStackTrace();
        }
    }
}
