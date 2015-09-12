package com.github.brosander.kettle.docker;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.ui.trans.step.BaseStepXulDialog;
import org.pentaho.ui.xul.binding.Binding;

/**
 * Created by bryan on 9/10/15.
 */
public class DockerTransExecutorDialog extends BaseStepXulDialog {
    private String tempStepName;
    private String image;
    private String transformation;

    public DockerTransExecutorDialog(Shell parent, Object in, TransMeta transMeta, String stepname) {
        super("com/github/brosander/kettle/docker/dockerTransExecutorDialog.xul", parent, (BaseStepMeta) in, transMeta, stepname);
        loadMeta((DockerTransExecutorMeta) baseStepMeta);
        this.stepname = stepname;
        tempStepName = stepname;
        this.transMeta = transMeta;
        try {
            bf.setBindingType(Binding.Type.BI_DIRECTIONAL);
            bf.createBinding(this, "tempStepName", "step-name", "value").fireSourceChanged();
            bf.createBinding(this, DockerTransExecutorMeta.IMAGE, "image", "value").fireSourceChanged();
            bf.createBinding(this, DockerTransExecutorMeta.TRANSFORMATION, "transformation", "value").fireSourceChanged();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onAccept() {
        if (Const.isEmpty(tempStepName)) {
            return;
        }
        if (!stepname.equals(tempStepName)) {
            baseStepMeta.setChanged();
        }
        saveMeta((DockerTransExecutorMeta) baseStepMeta);
        dispose();
    }

    @Override
    public void onCancel() {
        dispose();
    }

    @Override
    protected Class<?> getClassForMessages() {
        return DockerTransExecutorDialog.class;
    }

    private boolean nullSafeEquals(String first, String second) {
        if (first == null) {
            if (second == null) {
                return true;
            }
            return false;
        }
        return first.equals(second);
    }

    public void saveMeta(DockerTransExecutorMeta meta) {
        if (!nullSafeEquals(meta.getImage(), image) || !nullSafeEquals(meta.getTransformation(), transformation)) {
            baseStepMeta.setChanged();
        }
        meta.setImage(image);
        meta.setTransformation(transformation);
    }

    public void loadMeta(DockerTransExecutorMeta meta) {
        image = meta.getImage();
        transformation = meta.getTransformation();
    }


    public String getTransformation() {
        return transformation;
    }

    public void setTransformation(String transformation) {
        String oldVal = this.transformation;
        this.transformation = transformation;
        firePropertyChange(DockerTransExecutorMeta.TRANSFORMATION, oldVal, transformation);
    }

    public void selectTransformationFile() {
        FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
        if (!Const.isEmpty(transformation)) {
            fileDialog.setFileName(transMeta.environmentSubstitute(transformation));
        }
        if (fileDialog.open() != null) {
            setTransformation(fileDialog.getFilterPath() + System.getProperty("file.separator") + fileDialog.getFileName());

        }
    }

    public String getTempStepName() {
        return tempStepName;
    }

    public void setTempStepName(String tempStepName) {
        this.tempStepName = tempStepName;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
