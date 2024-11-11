$version: "2.0"

namespace com.amazonaws.videoanalytics.videologistics

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