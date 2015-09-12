package com.github.brosander.kettle.docker.input;

import org.pentaho.di.core.exception.KettleEOFException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import javax.servlet.http.HttpServletRequest;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

/**
 * Created by bryan on 9/10/15.
 */
public class DockerTransInput extends BaseStep implements StepInterface {
    private DataInputStream dataInputStream;

    public DockerTransInput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        if (first) {
            dataInputStream = (DataInputStream) getTrans().getExtensionDataMap().get(DataInputStream.class.getCanonicalName());
            try {
                RowMeta inputRowMeta = new RowMeta(dataInputStream);
                while (dataInputStream.readBoolean()) {
                    try {
                        putRow(inputRowMeta, inputRowMeta.readData(dataInputStream));
                    } catch (KettleEOFException e) {
                        break;
                    }
                }
            } catch (SocketTimeoutException e) {
                throw new KettleException(e);
            } catch (IOException e) {
                throw new KettleException(e);
            }
            log.logBasic("Received end of input");
            setOutputDone();
            first = false;
        }
        return false;
    }
}
