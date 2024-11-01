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

    fn get_named_topic_helper() -> TopicHelper {
        TopicHelper::new(CLIENT_ID.to_owned(), Some(SHADOW_NAME.to_owned()))
    }

    fn get_unnamed_topic_helper() -> TopicHelper {
        TopicHelper::new(CLIENT_ID.to_owned(), None)
    }
}
