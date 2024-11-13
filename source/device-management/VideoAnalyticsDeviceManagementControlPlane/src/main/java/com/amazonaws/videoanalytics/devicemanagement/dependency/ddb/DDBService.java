package com.amazonaws.videoanalytics.devicemanagement.dependency.ddb;

import javax.inject.Inject;
import com.amazonaws.videoanalytics.devicemanagement.dao.StartCreateDeviceDAO;
import com.amazonaws.videoanalytics.devicemanagement.schema.CreateDevice;

public class DDBService {
    private final StartCreateDeviceDAO startCreateDeviceDAO;

    @Inject
    public DDBService(StartCreateDeviceDAO startCreateDeviceDAO) {
        this.startCreateDeviceDAO = startCreateDeviceDAO;
    }

    public CreateDevice getCreateDevice(String jobId) {
        return startCreateDeviceDAO.load(jobId);
    }

    public void saveCreateDevice(CreateDevice createDevice) {
        startCreateDeviceDAO.save(createDevice);
    }
}


