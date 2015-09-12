package com.github.brosander.kettle.docker;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleEOFException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.resource.ResourceUtil;
import org.pentaho.di.resource.TopLevelResource;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransExecutionConfiguration;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.www.AddExportServlet;
import org.pentaho.di.www.WebResult;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bryan on 8/9/15.
 */
public class DockerTransExecutor extends BaseStep implements StepInterface {
    public static final String CONFIGURATION_IN_EXPORT_FILENAME = "__job_execution_configuration__.xml";
    private DockerTransExecutorMeta dockerTransExecutorMeta;
    private DockerTransExecutorData dockerTransExecutorData;
    private Thread readThread;

    public DockerTransExecutor(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        dockerTransExecutorMeta = (DockerTransExecutorMeta) smi;
        dockerTransExecutorData = (DockerTransExecutorData) sdi;

        Object[] row = getRow();
        if (row == null) {
            if (readThread != null) {
                try {
                    dockerTransExecutorData.dataOutputStream.writeBoolean(false);
                } catch (IOException e) {
                    throw new KettleException(e);
                }
                try {
                    logBasic("Waiting on results from Docker container");
                    readThread.join();
                } catch (InterruptedException e) {
                    throw new KettleException(e);
                }
            }
            return false;
        }
        RowMetaInterface inputRowMeta = getInputRowMeta();
        if (first) {
            dockerTransExecutorData.inputRowMeta = inputRowMeta;
            dockerTransExecutorData.process = null;//startContainer();
            try {
                dockerTransExecutorData.containerId = "77852b81dfbb"; //getContainerId(dockerTransExecutorData.process);
                TransMeta subTrans = dockerTransExecutorMeta.loadReferencedObject(0, getRepository(), getMetaStore(), this);

                TransExecutionConfiguration executionConfiguration = new TransExecutionConfiguration();
                Map<String, String> vars = new HashMap<String, String>();

                for (String var : Const.INTERNAL_TRANS_VARIABLES) {
                    vars.put(var, subTrans.getVariable(var));
                }
                for (String var : Const.INTERNAL_JOB_VARIABLES) {
                    vars.put(var, subTrans.getVariable(var));
                }

                SlaveServer slaveServer = new SlaveServer("container-slave-server", "localhost", getPort(dockerTransExecutorData.containerId, "8080"), "cluster", "cluster");

                executionConfiguration.getVariables().putAll(vars);
                slaveServer.injectVariables(executionConfiguration.getVariables());

                slaveServer.getLogChannel().setLogLevel(executionConfiguration.getLogLevel());

                // First export the job...
                //
                FileObject tempFile =
                        KettleVFS.createTempFile("transExport", ".zip", System.getProperty("java.io.tmpdir"), subTrans);

                TopLevelResource topLevelResource = ResourceUtil.serializeResourceExportInterface(
                        tempFile.getName().toString(), subTrans, subTrans, repository, metaStore, executionConfiguration
                                .getXML(), CONFIGURATION_IN_EXPORT_FILENAME);

                // Send the zip file over to the slave server...
                //
                String result = slaveServer.sendExport(
                        topLevelResource.getArchiveName(), AddExportServlet.TYPE_TRANS, topLevelResource
                                .getBaseResourceName());
                WebResult webResult = WebResult.fromXMLString(result);
                if (!webResult.getResult().equalsIgnoreCase(WebResult.STRING_OK)) {
                    throw new KettleException(
                            "There was an error passing the exported transformation to the remote server: "
                                    + Const.CR + webResult.getMessage());
                }
                String carteObjectId = webResult.getId();

                Socket rowSocket = new Socket("localhost", Integer.valueOf(getPort(dockerTransExecutorData.containerId, "9001")));
                log.logBasic("Connected socket at " + rowSocket);
                dockerTransExecutorData.dataInputStream = new DataInputStream(rowSocket.getInputStream());
                dockerTransExecutorData.dataOutputStream = new DataOutputStream(rowSocket.getOutputStream());
                dockerTransExecutorData.dataOutputStream.writeUTF(carteObjectId);
                dockerTransExecutorData.dataOutputStream.writeUTF(subTrans.getName());
                inputRowMeta.writeMeta(dockerTransExecutorData.dataOutputStream);
                readThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        RowMeta rowMeta;
                        try {
                            rowMeta = new RowMeta(dockerTransExecutorData.dataInputStream);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                        try {
                            while (dockerTransExecutorData.dataInputStream.readBoolean()) {
                                putRow(rowMeta, rowMeta.readData(dockerTransExecutorData.dataInputStream));
                            }
                        } catch (KettleEOFException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            setOutputDone();
                        }
                        System.out.println();
                    }
                });

                readThread.start();
            } catch (Exception e) {
                if (e instanceof KettleException) {
                    throw (KettleException) e;
                }
                throw new KettleException(e);
            }
            first = false;
        }
        if (inputRowMeta != dockerTransExecutorData.inputRowMeta && !dockerTransExecutorData.inputRowMeta.equals(inputRowMeta)) {
            throw new KettleException("Docker executor doesn't support metadata change during trans run");
        }
        try {
            dockerTransExecutorData.dataOutputStream.writeBoolean(true);
        } catch (IOException e) {
            throw new KettleException(e);
        }
        dockerTransExecutorData.inputRowMeta.writeData(dockerTransExecutorData.dataOutputStream, row);
        return true;
    }

    private String getPort(String containerId, String port) throws IOException, InterruptedException {
        return runCommandAndReturnOnlyLine("docker", "inspect", "-f", "{{(index (index .NetworkSettings.Ports \"" + port + "/tcp\") 0).HostPort}}", containerId);
    }

    private String runCommandAndReturnOnlyLine(String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(Arrays.asList(command));
        Process process = processBuilder.start();
        String readOnlyOutputLine = readOnlyOutputLine(process);
        process.waitFor();
        return readOnlyOutputLine;
    }

    private String readOnlyOutputLine(Process process) throws IOException {
        try (InputStream inputStream = process.getInputStream()) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
                try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                    return bufferedReader.readLine();
                }
            }
        }
    }

    private Process startContainer() throws KettleException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(Arrays.asList("docker", "run", "-d", "-P", "--rm=true", environmentSubstitute(dockerTransExecutorMeta.getImage())));
        try {
            return processBuilder.start();
        } catch (IOException e) {
            throw new KettleException("Unable to start docker container: " + processBuilder.command(), e);
        }
    }


}
