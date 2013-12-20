package com.pmease.gitop.core.manager.impl;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.pmease.gitop.core.manager.ConfigManager;
import com.pmease.gitop.model.Project;
import com.pmease.gitop.model.User;
import com.pmease.gitop.model.storage.ProjectStorage;
import com.pmease.gitop.model.storage.StorageManager;

@Singleton
public class DefaultStorageManager implements StorageManager {

    private final ConfigManager configManager;

    @Inject
    public DefaultStorageManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public ProjectStorage getStorage(Project project) {
        return new ProjectStorage(new File(configManager.getSystemSetting().getDataPath(),
                "projects/" + project.getId().toString()));
    }

    @Override
    public File getStorage(User user) {
        return new File(configManager.getSystemSetting().getDataPath(),
                "users/" + user.getId().toString());
    }

}
