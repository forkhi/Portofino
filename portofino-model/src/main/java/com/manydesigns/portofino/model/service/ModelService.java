package com.manydesigns.portofino.model.service;

import com.manydesigns.portofino.model.Model;
import com.manydesigns.portofino.model.io.ModelIO;
import com.manydesigns.portofino.model.io.dsl.DefaultModelIO;
import io.reactivex.subjects.PublishSubject;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ModelService {

    protected Model model = new Model();
    public final PublishSubject<EventType> modelEvents = PublishSubject.create();
    public static final String APP_MODEL_DIRECTORY = "portofino-model";
    protected final FileObject applicationDirectory;
    protected final Configuration configuration;
    protected final FileBasedConfigurationBuilder<PropertiesConfiguration> configurationFile;
    private static final Logger logger = LoggerFactory.getLogger(ModelService.class);

    public ModelService(FileObject applicationDirectory, Configuration configuration, FileBasedConfigurationBuilder<PropertiesConfiguration> configurationFile) {
        this.applicationDirectory = applicationDirectory;
        this.configuration = configuration;
        this.configurationFile = configurationFile;
    }

    public enum EventType {
        LOADED, SAVED
    }

    public Model getModel() {
        return model;
    }

    public synchronized Model loadModel(ModelIO modelIO) throws IOException {
        Model loaded = modelIO.load();
        if(loaded != null) {
            model = loaded;
            model.init();
            modelEvents.onNext(EventType.LOADED);
            return model;
        } else {
            return null;
        }
    }

    public synchronized void loadModel() throws IOException {
        loadModel(new DefaultModelIO(getModelDirectory()));
    }

    public FileObject getModelDirectory() throws FileSystemException {
        return applicationDirectory.resolveFile(APP_MODEL_DIRECTORY);
    }

    public FileObject getApplicationDirectory() {
        return applicationDirectory;
    }

    public synchronized void saveModel() throws IOException, ConfigurationException {
        model.init();
        new DefaultModelIO(getModelDirectory()).save(model);
        if (configurationFile != null) {
            configurationFile.save();
            logger.info("Saved configuration file {}", configurationFile.getFileHandler().getFile().getAbsolutePath());
        }
        modelEvents.onNext(EventType.SAVED);
    }
}
