package br.ufmg.app;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

public class FileWriter {
    private FileOutputStream fos;
    private OutputStreamWriter osw;
    private BufferedWriter bw;
    private PrintWriter wr;

    public FileWriter (Path filePath, boolean autoFlush) throws FileNotFoundException, UnsupportedEncodingException {
        fos = new FileOutputStream(filePath.toFile(), true);
        osw = new OutputStreamWriter(fos, "UTF-8");
        bw = new BufferedWriter(osw);
        wr = new PrintWriter(bw, autoFlush);
    }

    public void write(String text) {
        wr.write(text);
    }

    public void close() throws IOException {
        wr.close();
        bw.close();
        osw.close();
        fos.close();
    }
}
