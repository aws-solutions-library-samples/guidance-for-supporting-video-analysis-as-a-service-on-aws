#[derive(Debug, Clone)]
pub(crate) struct TopicHelper {
    client_id: String,
    shadow_name: Option<String>,
}

impl TopicHelper {
    pub(crate) fn new(client_id: String, shadow_name: Option<String>) -> Self {
        TopicHelper { client_id, shadow_name }
    }
    pub(crate) fn get_shadow_update_delta_topic(&self) -> String {
        match self.shadow_name.to_owned() {
            None => {
                format!("$aws/things/{}/shadow/update/delta", self.client_id)
            }
            Some(name) => {
                format!("$aws/things/{}/shadow/name/{}/update/delta", self.client_id, name)
            }
        }
    }
    pub(crate) fn get_shadow_update_accepted(&self) -> String {
        match self.shadow_name.to_owned() {
            None => {
                format!("$aws/things/{}/shadow/update/accepted", self.client_id)
            }
            Some(name) => {
                format!("$aws/things/{}/shadow/name/{}/update/accepted", self.client_id, name)
            }
        }
    }
    pub(crate) fn get_shadow_update_rejected(&self) -> String {
        match self.shadow_name.to_owned() {
            None => {
                format!("$aws/things/{}/shadow/update/rejected", self.client_id)
            }
            Some(name) => {
                format!("$aws/things/{}/shadow/name/{}/update/rejected", self.client_id, name)
            }
        }
    }
    pub(crate) fn get_jobs_notify_topic(&self) -> String {
        format!("$aws/things/{}/jobs/notify", self.client_id)
    }
    pub(crate) fn get_jobs_start_next_topic(&self) -> String {
        format!("$aws/things/{}/jobs/start-next", self.client_id)
    }
    pub(crate) fn get_jobs_start_next_accepted(&self) -> String {
        format!("$aws/things/{}/jobs/start-next/accepted", self.client_id)
    }
    pub(crate) fn get_jobs_start_next_rejected(&self) -> String {
        format!("$aws/things/{}/jobs/start-next/rejected", self.client_id)
    }
    pub(crate) fn get_jobs_update_topic(&self, job_id: String) -> String {
        format!("$aws/things/{}/jobs/{}/update", self.client_id, job_id)
    }
    pub(crate) fn get_jobs_update_accepted(&self, job_id: Option<String>) -> String {
        match job_id {
            None => {
                format!("$aws/things/{}/jobs/+/update/accepted", self.client_id)
            }
            Some(id) => {
                format!("$aws/things/{}/jobs/{}/update/accepted", self.client_id, id)
            }
        }
    }
    pub(crate) fn get_jobs_update_rejected(&self, job_id: Option<String>) -> String {
        match job_id {
            None => {
                format!("$aws/things/{}/jobs/+/update/rejected", self.client_id)
            }
            Some(id) => {
                format!("$aws/things/{}/jobs/{}/update/rejected", self.client_id, id)
            }
        }
    }
    pub(crate) fn get_next_job_topic(&self) -> String {
        format!("$aws/things/{}/jobs/$next/get", self.client_id)
    }
    pub(crate) fn get_next_job_get_accepted(&self) -> String {
        format!("$aws/things/{}/jobs/$next/get/accepted", self.client_id)
    }
    pub(crate) fn get_next_job_get_rejected(&self) -> String {
        format!("$aws/things/{}/jobs/$next/get/rejected", self.client_id)
    }
}

#[cfg(test)]
mod tests {
    use crate::client::topics::TopicHelper;

    const CLIENT_ID: &str = "ThingName";
    const SHADOW_NAME: &str = "ShadowName";
    const UPDATE_DELTA_CLASSIC: &str = r"$aws/things/ThingName/shadow/update/delta";
    const UPDATE_ACCEPTED_CLASSIC: &str = r"$aws/things/ThingName/shadow/update/accepted";
    const UPDATE_REJECTED_CLASSIC: &str = r"$aws/things/ThingName/shadow/update/rejected";
    const UPDATE_DELTA_NAMED: &str = r"$aws/things/ThingName/shadow/name/ShadowName/update/delta";
    const UPDATE_ACCEPTED_NAMED: &str =
        r"$aws/things/ThingName/shadow/name/ShadowName/update/accepted";
    const UPDATE_REJECTED_NAMED: &str =
        r"$aws/things/ThingName/shadow/name/ShadowName/update/rejected";

    const JOB_ID: &str = "12345";
    const JOBS_NOTIFY: &str = r"$aws/things/ThingName/jobs/notify";
    const JOBS_START_NEXT: &str = r"$aws/things/ThingName/jobs/start-next";
    const JOBS_START_NEXT_ACCEPTED: &str = r"$aws/things/ThingName/jobs/start-next/accepted";
    const JOBS_START_NEXT_REJECTED: &str = r"$aws/things/ThingName/jobs/start-next/rejected";
    const JOBS_UPDATE: &str = r"$aws/things/ThingName/jobs/12345/update";
    const JOBS_UPDATE_ACCEPTED: &str = r"$aws/things/ThingName/jobs/+/update/accepted";
    const JOBS_UPDATE_REJECTED: &str = r"$aws/things/ThingName/jobs/+/update/rejected";
    const JOBS_UPDATE_ACCEPTED_WITH_JOBID: &str =
        r"$aws/things/ThingName/jobs/12345/update/accepted";
    const JOBS_UPDATE_REJECTED_WITH_JOBID: &str =
        r"$aws/things/ThingName/jobs/12345/update/rejected";
    const JOBS_GET_ACCEPTED: &str = r"$aws/things/ThingName/jobs/get/accepted";
    const JOBS_GET_REJECTED: &str = r"$aws/things/ThingName/jobs/get/rejected";
    const JOBS_NEXT_PENDING_JOB_EXEC: &str = r"$aws/things/ThingName/jobs/$next/get";
    const JOBS_NEXT_PENDING_JOB_EXEC_ACCEPTED: &str =
        r"$aws/things/ThingName/jobs/$next/get/accepted";
    const JOBS_NEXT_PENDING_JOB_EXEC_REJECTED: &str =
        r"$aws/things/ThingName/jobs/$next/get/rejected";

    #[test]
    fn get_update_delta_topic_named_and_unnamed() {
        let unnamed_topic_helper = get_unnamed_topic_helper();
        assert_eq!(unnamed_topic_helper.get_shadow_update_delta_topic(), UPDATE_DELTA_CLASSIC);

        let named_topic_helper = get_named_topic_helper();
        assert_eq!(named_topic_helper.get_shadow_update_delta_topic(), UPDATE_DELTA_NAMED);
    }

    #[test]
    fn get_update_accepted_topic_named_and_unnamed() {
        let unnamed_topic_helper = get_unnamed_topic_helper();
        assert_eq!(unnamed_topic_helper.get_shadow_update_accepted(), UPDATE_ACCEPTED_CLASSIC);

        let named_topic_helper = get_named_topic_helper();
        assert_eq!(named_topic_helper.get_shadow_update_accepted(), UPDATE_ACCEPTED_NAMED);
    }

    #[test]
    fn get_update_rejected_topic_named_and_unnamed() {
        let unnamed_topic_helper = get_unnamed_topic_helper();
        assert_eq!(unnamed_topic_helper.get_shadow_update_rejected(), UPDATE_REJECTED_CLASSIC);

        let named_topic_helper = get_named_topic_helper();
        assert_eq!(named_topic_helper.get_shadow_update_rejected(), UPDATE_REJECTED_NAMED);
    }

    #[test]
    fn get_jobs_notify_topic() {
        let topic_helper = get_iot_job_topic_helper();
        assert_eq!(topic_helper.get_jobs_notify_topic(), JOBS_NOTIFY);
    }

    #[test]
    fn get_jobs_start_next_topic() {
        let topic_helper = get_iot_job_topic_helper();
        assert_eq!(topic_helper.get_jobs_start_next_topic(), JOBS_START_NEXT);
    }

    #[test]
    fn get_jobs_start_next_accepted_topic() {
        let topic_helper = get_iot_job_topic_helper();
        assert_eq!(topic_helper.get_jobs_start_next_accepted(), JOBS_START_NEXT_ACCEPTED);
    }

    #[test]
    fn get_jobs_start_next_rejected_topic() {
        let topic_helper = get_iot_job_topic_helper();
        assert_eq!(topic_helper.get_jobs_start_next_rejected(), JOBS_START_NEXT_REJECTED);
    }

    #[test]
    fn get_jobs_update_topic() {
        let topic_helper = get_iot_job_topic_helper();
        assert_eq!(topic_helper.get_jobs_update_topic(JOB_ID.to_string()), JOBS_UPDATE);
    }

    #[test]
    fn get_jobs_update_accepted_topic() {
        let topic_helper = get_iot_job_topic_helper();
        assert_eq!(topic_helper.get_jobs_update_accepted(None), JOBS_UPDATE_ACCEPTED);
        assert_eq!(
            topic_helper.get_jobs_update_accepted(Some(JOB_ID.to_owned())),
            JOBS_UPDATE_ACCEPTED_WITH_JOBID
        )
    }

    #[test]
    fn get_jobs_update_rejected_topic() {
        let topic_helper = get_iot_job_topic_helper();
        assert_eq!(topic_helper.get_jobs_update_rejected(None), JOBS_UPDATE_REJECTED);
        assert_eq!(
            topic_helper.get_jobs_update_rejected(Some(JOB_ID.to_owned())),
            JOBS_UPDATE_REJECTED_WITH_JOBID
        );
    }

    #[test]
    fn get_next_job_execution_accepted_topic() {
        let topic_helper = get_iot_job_topic_helper();
        assert_eq!(topic_helper.get_next_job_get_accepted(), JOBS_NEXT_PENDING_JOB_EXEC_ACCEPTED);
    }

    #[test]
    fn get_next_job_execution_rejected_topic() {
        let topic_helper = get_iot_job_topic_helper();
        assert_eq!(topic_helper.get_next_job_get_rejected(), JOBS_NEXT_PENDING_JOB_EXEC_REJECTED);
    }

    #[test]
    fn get_next_job_execution_topic() {
        let topic_helper = get_iot_job_topic_helper();
        assert_eq!(topic_helper.get_next_job_topic(), JOBS_NEXT_PENDING_JOB_EXEC);
    }

    fn get_named_topic_helper() -> TopicHelper {
        TopicHelper::new(CLIENT_ID.to_owned(), Some(SHADOW_NAME.to_owned()))
    }

    fn get_unnamed_topic_helper() -> TopicHelper {
        TopicHelper::new(CLIENT_ID.to_owned(), None)
    }

    fn get_iot_job_topic_helper() -> TopicHelper {
        TopicHelper::new(CLIENT_ID.to_owned(), None)
    }
}
