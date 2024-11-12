$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

use com.amazonaws.videoanalytics#JobId
use com.amazonaws.videoanalytics#Status


structure ExportVideoJobElement {
    creationTime: Timestamp,
    exportVideoJobStatus: String,
    exportVideoJobId: JobId,
    failureReason: String
}

list ExportVideoJobLists {
    member: ExportVideoJobElement
}

list ExportVideosResults {
    member: ExportVideosResult
}

structure ExportVideosResult {
    @required
    status: Status,
    failureReason: FailureReason,
    @required
    videoExportedPath: String,
    @required
    startTime: Timestamp,
    endTime: Timestamp
}

double JobRunTimeInMinutes
document DetailedFailureReason

@length(min: 1, max: 1024)
string FailureReason
