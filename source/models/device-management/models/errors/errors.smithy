$version: "2.0"

namespace com.amazonaws.videoanalytics.devicemanagement

structure ValidationExceptionField {
    @required
    name: String
    @required
    message: String
}

list ValidationExceptionFieldList {
    member: ValidationExceptionField
}

enum ValidationExceptionReason {
    UNKNOWN_OPERATION = "unknownOperation"
    CANNOT_PARSE = "cannotParse"
    FIELD_VALIDATION_FAILED = "fieldValidationFailed"
    OTHER = "other"
}

@httpError(400)
@error("client")
structure ValidationException {
    @required
    message: String,
    @required
    reason: ValidationExceptionReason,
    // The field that caused the error, if applicable. If more than one field caused the error, pick one and elaborate in the message
    fieldList: ValidationExceptionFieldList
}

@httpError(404)
@error("client")
structure ResourceNotFoundException {
    @required
    message: String
}

@httpError(500)
@error("server")
structure InternalServerException {
    @required
    message: String
}

@httpError(403)
@error("client")
structure AccessDeniedException {
    @required
    message: String
}

@httpError(409)
@error("client")
structure ConflictException {
    @required
    message: String
}
