$version: "2.0"

namespace com.amazonaws.videoanalytics

@http(code: 200, method: "POST", uri: "/start-update-device")
@idempotent
operation StartUpdateDevice {
    input: StartUpdateDeviceRequest,
    output: StartUpdateDeviceResponse,
    errors: [AccessDeniedException, InternalServerException, ValidationException]
}

@input
structure StartUpdateDeviceRequest {
    @required deviceId: DeviceId,
    newDeviceState: DeviceState,
    deviceGroupPayload: DeviceGroupPayload,
    deviceTypePayload: DeviceTypePayload,
    configurationPayload: Document,
    commandPayload: CommandPayload
}

@output
structure StartUpdateDeviceResponse {
    jobId: JobId
}
