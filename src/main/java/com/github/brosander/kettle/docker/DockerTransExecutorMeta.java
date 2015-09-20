package com.github.brosander.kettle.docker;

import com.github.brosander.kettle.docker.input.DockerTransInput;
import com.github.brosander.kettle.docker.input.DockerTransInputMeta;
import com.github.brosander.kettle.docker.output.DockerTransOutput;
import com.github.brosander.kettle.docker.output.DockerTransOutputMeta;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleMissingPluginsException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
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
import org.w3c.dom.Node;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by bryan on 8/9/15.
 */
@Step(id = "DockerTransExecutor", image = "docker.png", i18nPackageName = "com.github.brosander.kettle.docker", name = "DockerTransExecutorMeta.TransName", description = "DockerTransExecutorMeta.TransDescription", categoryDescription = "DockerTransExecutorMeta.CategoryDescription")
public class DockerTransExecutorMeta extends BaseStepMeta implements StepMetaInterface {
    public static final Class<?> PKG = DockerTransExecutorMeta.class;
    public static final String DEFAULT_IMAGE = "";
    public static final TransSpecificationMethod DEFAULT_TRANS_SPECIFICATION_METHOD = TransSpecificationMethod.FILENAME;
    public static final String IMAGE = "image";
    public static final String DEFAULT_TRANSFORMATION = "";
    public static final String TRANSFORMATION = "transformation";
    public static final String DOCKER_TRANS_EXECUTOR_META_DOCKER_TRANS = "DockerTransExecutorMeta.DockerTrans";
    private String image = DEFAULT_IMAGE;
    private String transformation = DEFAULT_TRANSFORMATION;
    private TransSpecificationMethod transSpecificationMethod = DEFAULT_TRANS_SPECIFICATION_METHOD;
    private TransMeta subTransMeta;

    public String getTransformation() {
        return transformation;
    }

    public void setTransformation(String transformation) {
        if (!this.transformation.equals(transformation)) {
            subTransMeta = null;
            this.transformation = transformation;
        }
    }

    public void setTransSpecificationMethod(TransSpecificationMethod transSpecificationMethod) {
        if (transSpecificationMethod != this.transSpecificationMethod) {
            subTransMeta = null;
            this.transSpecificationMethod = transSpecificationMethod;
        }
    }

    @Override
    public void setDefault() {
        image = DEFAULT_IMAGE;
        transformation = DEFAULT_TRANSFORMATION;
        transSpecificationMethod = DEFAULT_TRANS_SPECIFICATION_METHOD;
    }

    @Override
    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        return new DockerTransExecutor(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new DockerTransExecutorData();
    }

    @Override
    public String getXML() throws KettleException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("    ").append(XMLHandler.addTagValue(IMAGE, image));
        stringBuilder.append("    ").append(XMLHandler.addTagValue(TRANSFORMATION, transformation));
        return stringBuilder.toString();
    }

    @Override
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
        image = XMLHandler.getTagValue(stepnode, IMAGE);
        transformation = XMLHandler.getTagValue(stepnode, TRANSFORMATION);
    }

    @Override
    public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases) throws KettleException {
        image = rep.getStepAttributeString(id_step, IMAGE);
        transformation = rep.getStepAttributeString(id_step, TRANSFORMATION);
    }

    @Override
    public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step) throws KettleException {
        rep.saveStepAttribute(id_transformation, id_step, IMAGE, image);
        rep.saveStepAttribute(id_transformation, id_step, TRANSFORMATION, transformation);
    }

    @Override
    public void getFields(RowMetaInterface inputRowMeta, String name, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException {
        TransMeta subTransMeta = null;
        try {
            subTransMeta = loadReferencedObject(0, repository, metaStore, space);
        } catch (KettleException e) {
            throw new KettleStepException(e);
        }
        DockerTransInputMeta dockerTransInputMeta = null;
        StepMeta dockerTransOutputStepMeta = null;
        for (StepMeta meta : subTransMeta.getSteps()) {
            StepMetaInterface stepMetaInterface = meta.getStepMetaInterface();
            if (stepMetaInterface instanceof DockerTransInputMeta) {
                if (dockerTransInputMeta != null) {
                    throw new KettleStepException("Only one " + DockerTransInput.class.getSimpleName() + " allowed.");
                }
                dockerTransInputMeta = (DockerTransInputMeta) stepMetaInterface;
            }
            if (stepMetaInterface instanceof DockerTransOutputMeta) {
                if (dockerTransOutputStepMeta != null) {
                    throw new KettleStepException("Only one " + DockerTransOutput.class.getSimpleName() + " allowed");
                }
                dockerTransOutputStepMeta = meta;
            }
        }
        if (dockerTransInputMeta == null) {
            throw new KettleStepException("Couldn't find " + DockerTransInput.class.getSimpleName() + " which is required");
        }
        if (dockerTransOutputStepMeta == null) {
            throw new KettleStepException("Couldn't find " + DockerTransOutput.class.getSimpleName() + " which is required");
        }
        dockerTransInputMeta.setInputRowMeta(inputRowMeta);
        RowMetaInterface subTransMetaStepFields = subTransMeta.getStepFields(dockerTransOutputStepMeta);
        try {
            transSpecificationMethod.saveTrans(subTransMeta, transformation, null, repository, metaStore, space);
        } catch (KettleException e) {
            throw new KettleStepException(e);
        }
        inputRowMeta.clear();
        inputRowMeta.setValueMetaList(subTransMetaStepFields.getValueMetaList());
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public boolean[] isReferencedObjectEnabled() {
        return new boolean[]{!Const.isEmpty(transformation)};
    }

    @Override
    public TransMeta loadReferencedObject(int index, Repository rep, IMetaStore metaStore, VariableSpace space) throws KettleException {
        if (subTransMeta == null) {
            subTransMeta = transSpecificationMethod.loadTrans(transformation, null, rep, metaStore, space);
        }
        return subTransMeta;
    }

    @Override
    public String[] getReferencedObjectDescriptions() {
        return new String[]{BaseMessages.getString(PKG, DOCKER_TRANS_EXECUTOR_META_DOCKER_TRANS)};
    }

    public enum TransSpecificationMethod implements TransLoader, TransSaver {
        FILENAME() {
            @Override
            public void saveTrans(TransMeta transMeta, String name, String directoryPath, Repository repository, IMetaStore metaStore, VariableSpace variableSpace) throws KettleException {
                String xml = XMLHandler.getXMLHeader() + transMeta.getXML();
                DataOutputStream dos = new DataOutputStream(KettleVFS.getOutputStream(name, false));
                try {
                    dos.write(xml.getBytes(Const.XML_ENCODING));
                    dos.close();
                } catch (IOException e) {
                    throw new KettleException(e);
                }
            }

            @Override
            public TransMeta loadTrans(String name, String directoryPath, Repository repository, IMetaStore metaStore, VariableSpace variableSpace) throws KettleXMLException, KettleMissingPluginsException {
                return new TransMeta(variableSpace.environmentSubstitute(name), metaStore, repository, true, null, null);
            }
        };
    }

    private interface TransLoader {
        TransMeta loadTrans(String name, String directoryPath, Repository repository, IMetaStore metaStore, VariableSpace variableSpace) throws KettleXMLException, KettleMissingPluginsException;
    }

    private interface TransSaver {
        void saveTrans(TransMeta transMeta, String name, String directoryPath, Repository repository, IMetaStore metaStore, VariableSpace variableSpace) throws KettleException;
    }
}
