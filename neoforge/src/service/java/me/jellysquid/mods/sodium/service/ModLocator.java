package me.jellysquid.mods.sodium.service;

import cpw.mods.jarhandling.JarContents;
import net.minecraftforgespi.ILaunchContext;
import net.minecraftforgespi.locating.IDiscoveryPipeline;
import net.minecraftforgespi.locating.IModFileCandidateLocator;
import net.minecraftforgespi.locating.IncompatibleFileReporting;
import net.minecraftforgespi.locating.ModFileDiscoveryAttributes;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class ModLocator implements IModFileCandidateLocator {
    @Override
    public void findCandidates(ILaunchContext iLaunchContext, IDiscoveryPipeline pipeline) {
        URL jarLocation = getClass().getProtectionDomain().getCodeSource().getLocation();
        try {
            Path path = Path.of(jarLocation.toURI()).resolve("META-INF").resolve("jarjar");
            System.out.println("Sodium: Looking in " + path);
            List<Path> directoryContent;
            try (var files = Files.list(path)) {
                directoryContent = files
                        .filter(p -> StringUtils.toLowerCase(p.getFileName().toString()).endsWith(".jar"))
                        .sorted(Comparator.comparing(p -> StringUtils.toLowerCase(p.getFileName().toString())))
                        .toList();
            } catch (UncheckedIOException | IOException e) {
                throw new ModLoadingException(ModLoadingIssue.error("fml.modloading.failed_to_list_folder_content").withAffectedPath(path).withCause(e));
            }

            for (var file : directoryContent) {
                if (!Files.isRegularFile(file)) {
                    pipeline.addIssue(ModLoadingIssue.warning("fml.modloading.brokenfile", file).withAffectedPath(file));
                    continue;
                }

                pipeline.addJarContent(JarContents.of(file), ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.WARN_ALWAYS);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}