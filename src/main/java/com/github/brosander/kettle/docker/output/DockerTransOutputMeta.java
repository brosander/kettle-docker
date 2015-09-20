package com.github.brosander.kettle.docker.output;

import com.github.brosander.kettle.docker.input.DockerTransInputData;
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
@Step(id = "DockerTransOutput", image = "docker.png", i18nPackageName = "com.github.brosander.kettle.docker.output", name = "DockerTransOutputMeta.TransName", description = "DockerTransOutputMeta.TransDescription", categoryDescription = "DockerTransOutputMeta.CategoryDescription")
public class DockerTransOutputMeta extends BaseStepMeta implements StepMetaInterface {
    @Override
    public void setDefault() {

    }

    @Override
    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
        return new DockerTransOutput(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new DockerTransOutputData();
    }

    @Override
    public String getDialogClassName() {
        throw new RuntimeException("This step doesn't require configuration");
    }
}
