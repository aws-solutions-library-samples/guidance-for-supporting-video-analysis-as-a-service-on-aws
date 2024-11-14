$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#ValidationException
use com.amazonaws.videoanalytics#DeviceId

@http(code: 200, method: "POST", uri: "/create-snapshot-upload-path")
@idempotent
operation CreateSnapshotUploadPath {
    input: CreateSnapshotUploadPathRequest,
    errors: [InternalServerException, ValidationException]
}

@input
structure CreateSnapshotUploadPathRequest {
    @required
    deviceId: DeviceId,
    @required
    checksum: String,
    @required
    contentLength: Long,
}
