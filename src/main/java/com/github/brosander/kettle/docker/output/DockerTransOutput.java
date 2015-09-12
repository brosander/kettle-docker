package com.github.brosander.kettle.docker.output;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by bryan on 9/10/15.
 */
public class DockerTransOutput extends BaseStep implements StepInterface {
    private DataOutputStream dataOutputStream;
    private RowMetaInterface inputRowMeta;

    public DockerTransOutput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        Object[] row = getRow();
        if (row == null) {
            try {
                dataOutputStream.writeBoolean(false);
                log.logBasic("Sent end of output");
            } catch (IOException e) {
                throw new KettleException(e);
            }
            setOutputDone();
            return false;
        }
        if (first) {
            inputRowMeta = getInputRowMeta();
            dataOutputStream = (DataOutputStream) getTrans().getExtensionDataMap().get(DataOutputStream.class.getCanonicalName());
            inputRowMeta.writeMeta(dataOutputStream);
            first = false;
        }
        try {
            dataOutputStream.writeBoolean(true);
        } catch (IOException e) {
            throw new KettleException(e);
        }
        inputRowMeta.writeData(dataOutputStream, row);
        return true;
    }

    @Override
    public void cleanup() {
        try {
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
