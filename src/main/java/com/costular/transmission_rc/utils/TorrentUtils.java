package com.costular.transmission_rc.utils;


import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by costular on 21/07/17.
 */
public class TorrentUtils {

    public static String getTorrentB64Encoded(File file) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())), StandardCharsets.UTF_8);
            return new String(Base64.encode(content.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
