package com.github.brosander.kettle.docker;

import org.pentaho.di.core.annotations.KettleLifecyclePlugin;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.lifecycle.KettleLifecycleListener;
import org.pentaho.di.core.lifecycle.LifecycleException;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.logging.LoggingObjectType;
import org.pentaho.di.core.logging.SimpleLoggingObject;
import org.pentaho.di.core.util.ExecutorUtil;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.www.Carte;
import org.pentaho.di.www.CarteObjectEntry;
import org.pentaho.di.www.CarteSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Created by bryan on 9/11/15.
 */
@KettleLifecyclePlugin(id = "DockerRunTransServer")
public class DockerRunTransServer implements KettleLifecycleListener, Runnable {
    public static final String CONTEXT_PATH = "/docker/runTrans";
    private Logger logger = LoggerFactory.getLogger(DockerRunTransServer.class);
    private ServerSocket serverSocket = null;

    private boolean isCarte() {
        String carteCanonical = Carte.class.getCanonicalName();
        for (StackTraceElement stackTraceElement : new Exception().getStackTrace()) {
            if (stackTraceElement.getClassName().equals(carteCanonical) && stackTraceElement.getMethodName().equals("main")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onEnvironmentInit() throws LifecycleException {
        if (isCarte()) {
            try {
                serverSocket = new ServerSocket(9001);
            } catch (IOException e) {
                throw new LifecycleException(e, true);
            }
            new Thread(this).start();
        }
    }

    @Override
    public void onEnvironmentShutdown() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore
            }
            serverSocket = null;
        }
    }

    @Override
    public void run() {
        final ExecutorService executorService = ExecutorUtil.getExecutor();
        while (true) {
            try {
                final Socket socket = serverSocket.accept();
                final DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                final DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                String id = dataInputStream.readUTF();
                String transName = dataInputStream.readUTF();
                final Trans trans = CarteSingleton.getInstance().getTransformationMap().getTransformation(new CarteObjectEntry(transName, id));
                KettleLogStore.discardLines(trans.getLogChannelId(), true);

                String carteObjectId = UUID.randomUUID().toString();
                SimpleLoggingObject servletLoggingObject =
                        new SimpleLoggingObject(CONTEXT_PATH, LoggingObjectType.CARTE, null);
                servletLoggingObject.setContainerObjectId(carteObjectId);
                servletLoggingObject.setLogLevel(trans.getLogLevel());
                trans.setParent(servletLoggingObject);
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        trans.getExtensionDataMap().put(DataInputStream.class.getCanonicalName(), dataInputStream);
                        trans.getExtensionDataMap().put(DataOutputStream.class.getCanonicalName(), dataOutputStream);
                        try {
                            trans.execute(null);
                            trans.waitUntilFinished();
                        } catch (KettleException e) {
                            e.printStackTrace();
                        } finally {
                            if (!trans.isFinishedOrStopped()) {
                                trans.stopAll();
                            }
                            try {
                                socket.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                });
            } catch (SocketException e) {
                logger.debug("Got socket exception, assuming shutdown", e);
            } catch (IOException e) {
                logger.error(e.getMessage());
                return;
            }
        }
    }
}
