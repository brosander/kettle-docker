package com.github.brosander.kettle.docker.input;

import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Created by bryan on 9/10/15.
 */
@Step(id = "DockerTransInput", image = "docker-input.png", i18nPackageName = "com.github.brosander.kettle.docker.input", name = "DockerTransInputMeta.TransName", description = "DockerTransInputMeta.TransDescription", categoryDescription = "DockerTransInputMeta.CategoryDescription")
public class DockerTransInputMeta extends BaseStepMeta implements StepMetaInterface {
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
}
