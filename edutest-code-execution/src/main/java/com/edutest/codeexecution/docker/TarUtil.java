package com.edutest.codeexecution.docker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Minimal POSIX ustar writer — emits a single-file tar archive that docker-java's
 * copyArchiveToContainerCmd accepts. Avoids pulling commons-compress just for this.
 */
final class TarUtil {

    private static final int BLOCK_SIZE = 512;

    private TarUtil() {
    }

    static byte[] singleFileTar(String filename, byte[] content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(buildHeader(filename, content.length));
        out.write(content);
        int padding = BLOCK_SIZE - (content.length % BLOCK_SIZE);
        if (padding < BLOCK_SIZE) {
            out.write(new byte[padding]);
        }
        out.write(new byte[BLOCK_SIZE * 2]);
        return out.toByteArray();
    }

    private static byte[] buildHeader(String filename, int size) {
        byte[] header = new byte[BLOCK_SIZE];

        byte[] nameBytes = filename.getBytes(StandardCharsets.UTF_8);
        if (nameBytes.length > 100) {
            throw new IllegalArgumentException("Filename too long for tar header: " + filename);
        }
        System.arraycopy(nameBytes, 0, header, 0, nameBytes.length);

        writeOctal(header, 100, 8, 0644);
        writeOctal(header, 108, 8, 0);
        writeOctal(header, 116, 8, 0);
        writeOctal(header, 124, 12, size);
        writeOctal(header, 136, 12, System.currentTimeMillis() / 1000);

        Arrays.fill(header, 148, 156, (byte) ' ');
        header[156] = '0';

        byte[] magic = "ustar\0".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(magic, 0, header, 257, magic.length);
        header[263] = '0';
        header[264] = '0';

        long checksum = 0;
        for (byte b : header) {
            checksum += (b & 0xff);
        }
        writeOctal(header, 148, 7, checksum);
        header[155] = ' ';

        return header;
    }

    private static void writeOctal(byte[] header, int offset, int length, long value) {
        String octal = Long.toOctalString(value);
        int padLen = length - 1 - octal.length();
        if (padLen < 0) {
            throw new IllegalArgumentException("Octal value does not fit: " + value);
        }
        for (int i = 0; i < padLen; i++) {
            header[offset + i] = '0';
        }
        byte[] octalBytes = octal.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(octalBytes, 0, header, offset + padLen, octalBytes.length);
        header[offset + length - 1] = 0;
    }
}
