package com.github.brosander.kettle.docker.input;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.List;

/**
 * Created by bryan on 9/10/15.
 */
@Step(id = "DockerTransInput", image = "docker.png", i18nPackageName = "com.github.brosander.kettle.docker.input", name = "DockerTransInputMeta.TransName", description = "DockerTransInputMeta.TransDescription", categoryDescription = "DockerTransInputMeta.CategoryDescription")
public class DockerTransInputMeta extends BaseStepMeta implements StepMetaInterface {
    public static final String INPUT_ROW_META = "inputRowMeta";
    private RowMetaInterface inputRowMeta;

    @Override
    public void setDefault() {

    }

    @Override
    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        return new DockerTransInput(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new DockerTransInputData();
    }

    @Override
    public void getFields(RowMetaInterface inputRowMeta, String name, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException {
        if (this.inputRowMeta == null) {
            throw new KettleStepException("Need to do a get fields from parent trans first");
        }
        inputRowMeta.clear();
        inputRowMeta.setValueMetaList(this.inputRowMeta.getValueMetaList());
    }

    @Override
    public String getXML() throws KettleException {
        StringBuilder stringBuilder = new StringBuilder();
        if (inputRowMeta != null) {
            try {
                stringBuilder.append("   ").append(XMLHandler.addTagValue(INPUT_ROW_META, inputRowMeta.getMetaXML()));
            } catch (IOException e) {
                throw new KettleException(e);
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
        Node inputRowMetaNode = XMLHandler.getSubNode(stepnode, INPUT_ROW_META);
        if (inputRowMetaNode != null) {
            try {
                inputRowMeta = new RowMeta(inputRowMetaNode);
            } catch (KettleException e) {
                throw new KettleXMLException(e);
            }
        }
    }

    @Override
    public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step) throws KettleException {
        if (inputRowMeta != null) {
            try {
                rep.saveStepAttribute(id_transformation, id_step, INPUT_ROW_META, inputRowMeta.getMetaXML());
            } catch (IOException e) {
                throw new KettleException(e);
            }
        }
    }

    @Override
    public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases) throws KettleException {
        String inputRowMetaXml = rep.getStepAttributeString(id_step, INPUT_ROW_META);
        if (!Const.isEmpty(inputRowMetaXml)) {
            Document document = XMLHandler.loadXMLString(inputRowMetaXml);
            inputRowMeta = new RowMeta(document);
        }
    }

    public void setInputRowMeta(RowMetaInterface rowMeta) {
        inputRowMeta = rowMeta;
    }

    @Override
    public String getDialogClassName() {
        throw new RuntimeException("This step doesn't require configuration");
    }
}
