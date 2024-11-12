$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use com.amazonaws.videoanalytics#DeviceId
use com.amazonaws.videoanalytics#InternalServerException
use com.amazonaws.videoanalytics#JobId
use com.amazonaws.videoanalytics#ResourceNotFoundException
use com.amazonaws.videoanalytics#S3Path
use com.amazonaws.videoanalytics#ValidationException

//TODO: Add idempotent token and validations
// POST method as device is not a string, and unions cannot be in the uri.
@http(code: 200, method: "PUT", uri: "/start-export-videos")
@tags(["Facade:VideoLogisticsRoute"])
@idempotent
operation StartExportVideos {
    input: StartExportVideosRequest,
    output: StartExportVideosResponse,
    errors: [ValidationException, ResourceNotFoundException, InternalServerException]
}

@input
structure StartExportVideosRequest {
  @required
  startTime: Timestamp,
  @required
  endTime: Timestamp,
  @required 
  device: DeviceId
  @required
  exportS3Path: S3Path
}

@output
structure StartExportVideosResponse {
    jobId: JobId
}
