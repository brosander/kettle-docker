package com.github.brosander.kettle.docker;

import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by bryan on 8/9/15.
 */
public class DockerTransExecutorData extends BaseStepData implements StepDataInterface {
    public Process process;
    public DataOutputStream dataOutputStream;
    public DataInputStream dataInputStream;
    private OutputStream outputStream;
    private BufferedOutputStream bufferedOutputStream;
    private InputStream inputStream;
    private BufferedInputStream bufferedInputStream;
    public RowMetaInterface inputRowMeta;
    public String containerId;

    public void startTrans(SlaveServer slaveServer, String carteId) throws UnsupportedEncodingException {
        String url = slaveServer.constructUrl(DockerRunTransServer.CONTEXT_PATH);
    }

    public void createDataOutputStream(String inputDirectory, String inputFifo) throws KettleException {
        String pathname = inputDirectory + "/" + inputFifo;
        try {
            outputStream = new FileOutputStream(new File(pathname));
        } catch (FileNotFoundException e) {
            throw new KettleException("Unable to create outputStream to " + pathname, e);
        }
        bufferedOutputStream = new BufferedOutputStream(outputStream);
        dataOutputStream = new DataOutputStream(bufferedOutputStream);
    }

    public void createDataInputStream(String outputDirectory, String outputFifo) throws KettleException {
        String pathname = outputDirectory + "/" + outputFifo;
        try {
            inputStream = new FileInputStream(new File(pathname));
        } catch (FileNotFoundException e) {
            throw new KettleException("Unable to create inputStream to " + pathname, e);
        }
        bufferedInputStream = new BufferedInputStream(inputStream);
        dataInputStream = new DataInputStream(bufferedInputStream);
    }

    public void closeOutput() {
        closeQuietly(dataOutputStream);
        closeQuietly(bufferedOutputStream);
        closeQuietly(outputStream);
    }

    public void closeInput() {
        closeQuietly(dataInputStream);
        closeQuietly(bufferedInputStream);
        closeQuietly(inputStream);
    }

    private void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            // Ignore
        }
    }
}
