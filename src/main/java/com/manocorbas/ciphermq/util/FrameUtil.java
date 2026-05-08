package com.manocorbas.ciphermq.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FrameUtil {

    public static void send(OutputStream out, String json) throws IOException {

        byte[] data = json.getBytes();

        // escreve tamanho (4 bytes)
        out.write(intToBytes(data.length));

        // escreve payload
        out.write(data);

        out.flush();
    }

    public static String receive(InputStream in) throws IOException {

        // lê tamanho
        byte[] lenBytes = readNBytes(in, 4);
        int length = bytesToInt(lenBytes);

        // lê payload completo
        byte[] data = readNBytes(in, length);

        return new String(data);
    }

    private static byte[] readNBytes(InputStream in, int n) throws IOException {
        byte[] buffer = new byte[n];
        int read = 0;

        while (read < n) {
            int r = in.read(buffer, read, n - read);
            if (r == -1) throw new IOException("Conexão fechada");
            read += r;
        }

        return buffer;
    }


    private static byte[] intToBytes(int value) {
        return new byte[] {
            (byte)(value >> 24), // quebra manual de integer em 4 bytes
            (byte)(value >> 16), // '>>' desloca o inteiro em n bits, para a direita 
            (byte)(value >> 8),  // o cast pega apenas os 8 últimos bits (1 byte)
            (byte) value
        };
    }

    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) | // "& 0xFF" torna unsigned (0 a 255)
               ((bytes[1] & 0xFF) << 16) | // '|' junta os 4 bytes
               ((bytes[2] & 0xFF) << 8)  |
                (bytes[3] & 0xFF);
    }
}