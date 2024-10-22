$version: "2.0"

namespace com.aws.videoanalytic

use aws.protocols#restJson1


@restJson1
@title("Video Analytic Guidance Solution")
service VideoAnalytic {
    version: "2024-10-18"
    resources: [Device]
    errors: [ServiceError, ThrottlingError]
}

