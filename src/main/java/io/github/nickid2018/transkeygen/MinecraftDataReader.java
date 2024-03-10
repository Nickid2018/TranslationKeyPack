package io.github.nickid2018.transkeygen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MinecraftDataReader implements AutoCloseable {

    private final ZipFile minecraftJar;

    public MinecraftDataReader(String minecraftJarLoc) throws IOException {
        minecraftJar = new ZipFile(minecraftJarLoc);
    }

    public String parseTranslations() throws IOException {
        ZipEntry langFile = minecraftJar.getEntry("assets/minecraft/lang/en_us.json");
        String sourceLangContent = IOUtils.toString(minecraftJar.getInputStream(langFile), StandardCharsets.UTF_8);
        JsonObject source = JsonParser.parseString(sourceLangContent).getAsJsonObject();
        JsonObject mapped = new JsonObject();

        source.entrySet().forEach(entry -> {
            String key = entry.getKey();
            String value = entry.getValue().getAsString();
            mapped.addProperty(key, mapSingleKey(key, value));
        });

        return mapped.toString();
    }

    public String mapSingleKey(String key, String value) {
        if (key.startsWith("translation.test.invalid"))
            return value;

        try {
            String.format(value);
            return key;
        } catch (Exception ignored) {
        }

        List<Object> testArguments = new ArrayList<>();
        List<Object> testedArguments = new ArrayList<>();

        testArguments.add("1");
        testedArguments.add("%s");

        String lastTranslation = value;
        String translationNow;
        while (true) {
            try {
                translationNow = String.format(value, testArguments.toArray());
                if (translationNow.equals(lastTranslation))
                    break;
                lastTranslation = translationNow;
                testArguments.add("1");
                testedArguments.add("%s");
            } catch (MissingFormatArgumentException ignored) {
                testArguments.add("1");
                testedArguments.add("%s");
            } catch (IllegalFormatConversionException ifce) {
                testedArguments.remove(testedArguments.size() - 1);
                testedArguments.add("%" + ifce.getConversion());
                testArguments.remove(testArguments.size() - 1);
                testArguments.add(1);
            }
        }

        testedArguments.remove(testedArguments.size() - 1);
        int args = testedArguments.size();
        if (args == 0)
            return key;
        else
            return String.format("%s %s", key, testedArguments.stream().map(s -> "(" + s + ")").collect(Collectors.joining(" ")));
    }

    public int readResourcePackVersion() throws IOException {
        ZipEntry versionInfo = minecraftJar.getEntry("version.json");
        String versionInfoContent = IOUtils.toString(minecraftJar.getInputStream(versionInfo), StandardCharsets.UTF_8);
        JsonObject versionInfoJson = JsonParser.parseString(versionInfoContent).getAsJsonObject();
        return versionInfoJson.getAsJsonObject("pack_version").get("resource").getAsInt();
    }

    @Override
    public void close() throws IOException {
        minecraftJar.close();
    }
}
