import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public final class RestoreBinaryAssets {
    private RestoreBinaryAssets() {}

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path manifest = root.resolve("binary_assets/manifest.tsv");
        if (!Files.isRegularFile(root.resolve("settings.gradle.kts"))) {
            throw new IllegalStateException("Run this command from the repository root.");
        }
        if (!Files.isRegularFile(manifest)) {
            throw new IllegalStateException("Asset manifest not found: " + manifest);
        }

        List<String> lines = Files.readAllLines(manifest, StandardCharsets.UTF_8);
        int restored = 0;
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }

            String[] fields = line.split("\\t", -1);
            if (fields.length != 4) {
                throw new IllegalStateException("Invalid manifest row: " + line);
            }

            String relativeOutput = fields[0];
            long expectedBytes = Long.parseLong(fields[1]);
            String expectedSha256 = fields[2];
            String[] partNames = fields[3].split(";");
            Path output = root.resolve(relativeOutput).normalize();
            if (!output.startsWith(root)) {
                throw new IllegalStateException("Unsafe output path: " + relativeOutput);
            }

            Files.createDirectories(output.getParent());
            Path temporary = output.resolveSibling(output.getFileName() + ".restore-tmp");
            MessageDigest digest = sha256();
            long written = 0;

            try (OutputStream destination = Files.newOutputStream(temporary)) {
                for (String partName : partNames) {
                    Path part = root.resolve(partName).normalize();
                    if (!part.startsWith(root) || !Files.isRegularFile(part)) {
                        throw new IllegalStateException("Missing or unsafe asset part: " + partName);
                    }
                    try (InputStream source = Files.newInputStream(part)) {
                        byte[] buffer = new byte[64 * 1024];
                        int count;
                        while ((count = source.read(buffer)) != -1) {
                            destination.write(buffer, 0, count);
                            digest.update(buffer, 0, count);
                            written += count;
                        }
                    }
                }
            }

            String actualSha256 = HexFormat.of().formatHex(digest.digest());
            if (written != expectedBytes || !actualSha256.equalsIgnoreCase(expectedSha256)) {
                Files.deleteIfExists(temporary);
                throw new IllegalStateException(
                    "Verification failed for " + relativeOutput
                        + ": expected " + expectedBytes + " bytes / " + expectedSha256
                        + ", restored " + written + " bytes / " + actualSha256
                );
            }

            moveIntoPlace(temporary, output);
            restored++;
            System.out.println("Restored " + relativeOutput);
        }

        System.out.println("Restored and verified " + restored + " binary assets successfully.");
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static void moveIntoPlace(Path source, Path destination) throws IOException {
        try {
            Files.move(
                source,
                destination,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
