extern crate quickxml_to_serde;
use crate::constants::{
    BASIC_EVENT, EMPTY_STRING, EVENT_FRAME_KEY, EVENT_KEY, EVENT_SOURCE_KEY, EVENT_TIMESTAMP_KEY,
    INFERENCE_KEY, MESSAGE_KEY, METADATA_STREAM_KEY, MODEL_NAME_KEY, MODEL_VERSION,
    MODEL_VERSION_KEY, NAME_KEY, NOTIFICATION_MESSAGE_KEY, SIMPLE_ITEM_KEY, TIMESTAMP_KEY,
    TRAJECTORY_EVENT, UPDATE_ATTRIBUTES_EVENT, VA_ATTR, VA_ATTR_BASIC, VA_ATTR_BI, VA_HEAT,
    VA_HEAT_BASIC, VA_HEAT_BI, VA_TRAJ, VA_TRAJ_BASIC, VA_TRAJ_BI, VIDEO_ANALYTICS_KEY,
};
use async_trait::async_trait;
use chrono::NaiveDateTime;
use quickxml_to_serde::{xml_string_to_json, Config, NullValue};
use serde_json::{json, Value};
use std::collections::HashMap;
use std::error::Error;
use streaming_traits::error::MetadataStreamingError;
use streaming_traits::EventProcessor;
use tracing::{debug, trace};

/// Struct to store post-process onvif metadata event.
#[derive(Debug, Clone)]
pub struct OnvifMetadata {
    event_source_map: HashMap<String, String>,
}

/// Rust best practice to implement Default trait
impl Default for OnvifMetadata {
    fn default() -> Self {
        let event_source_map = HashMap::from([
            (VA_HEAT_BASIC.to_string(), BASIC_EVENT.to_string()),
            (VA_HEAT.to_string(), BASIC_EVENT.to_string()),
            (VA_HEAT_BI.to_string(), BASIC_EVENT.to_string()),
            (VA_ATTR_BASIC.to_string(), UPDATE_ATTRIBUTES_EVENT.to_string()),
            (VA_ATTR.to_string(), UPDATE_ATTRIBUTES_EVENT.to_string()),
            (VA_ATTR_BI.to_string(), UPDATE_ATTRIBUTES_EVENT.to_string()),
            (VA_TRAJ_BASIC.to_string(), TRAJECTORY_EVENT.to_string()),
            (VA_TRAJ.to_string(), TRAJECTORY_EVENT.to_string()),
            (VA_TRAJ_BI.to_string(), TRAJECTORY_EVENT.to_string()),
        ]);
        OnvifMetadata { event_source_map }
    }
}

impl OnvifMetadata {
    /// Instantiate OnvifMetadata.
    pub fn new() -> OnvifMetadata {
        Self::default()
    }

    /// Convert Onvif metadata xml event to json.
    pub fn xml_to_json(event_xml: String) -> Result<Value, Box<dyn Error>> {
        let config = Config::new_with_custom_values(true, "", "txt", NullValue::Null);
        Ok(xml_string_to_json(event_xml, &config)?)
    }
    pub fn is_motion_based_event(event_json: Value) -> bool {
        let name_key = &event_json[EVENT_SOURCE_KEY][SIMPLE_ITEM_KEY][NAME_KEY];
        return name_key.as_str().eq(&Some(EVENT_SOURCE_KEY));
    }
}

#[async_trait]
impl EventProcessor for OnvifMetadata {
    /// Post-process Metadata event to format compatible with Cloud.
    fn post_process_event(&self, event: String) -> Result<String, Box<dyn Error>> {
        let event_json = OnvifMetadata::xml_to_json(event)?;
        let event_source = &event_json[METADATA_STREAM_KEY][VIDEO_ANALYTICS_KEY][EVENT_FRAME_KEY]
            [EVENT_SOURCE_KEY];
        let event_time = &event_json[METADATA_STREAM_KEY][VIDEO_ANALYTICS_KEY][EVENT_FRAME_KEY]
            [EVENT_TIMESTAMP_KEY];
        let mut processed_json_event = json!({
            INFERENCE_KEY : event_json
        });
        if !event_source.is_string() {
            return Ok(EMPTY_STRING.to_string());
        }
        let event_source_string =
            event_source.as_str().ok_or(MetadataStreamingError::AIEventPostProcessingError)?;
        let event_time_string =
            event_time.as_str().ok_or(MetadataStreamingError::AIEventPostProcessingError)?;
        let event_time_in_utc =
            NaiveDateTime::parse_from_str(event_time_string, "%Y-%m-%dT%H:%M:%S")?;
        let event_timestamp = event_time_in_utc.and_utc().timestamp();

        // Filter if event does not have a model name.
        processed_json_event[MODEL_NAME_KEY] =
            match self.event_source_map.get(&event_source_string.to_string()) {
                None => {
                    return Ok(EMPTY_STRING.to_string());
                }
                Some(model_name) => {
                    debug!("{}", event_source_string.to_string());
                    json!(model_name)
                }
            };

        processed_json_event[MODEL_VERSION_KEY] = json!(MODEL_VERSION);
        processed_json_event[TIMESTAMP_KEY] = json!(event_timestamp);
        Ok(processed_json_event.to_string())
    }

    fn get_motion_based_event(&self, event: String) -> Result<Option<Value>, Box<dyn Error>> {
        // event logged may not be a motion detection event
        trace!("ONVIF metadata event {:?}", event.clone());
        let event_json = OnvifMetadata::xml_to_json(event)?;
        let simple_event_json = event_json[METADATA_STREAM_KEY][EVENT_KEY]
            [NOTIFICATION_MESSAGE_KEY][MESSAGE_KEY][MESSAGE_KEY]
            .clone();
        if OnvifMetadata::is_motion_based_event(simple_event_json.clone()) {
            return Ok(Some(simple_event_json));
        }
        return Ok(None);
    }
}

#[cfg(test)]
mod tests {
    use crate::constants::{
        BASIC_EVENT, DATA_KEY, EVENT_SOURCE_KEY, MODEL_NAME_KEY, MODEL_VERSION, MODEL_VERSION_KEY,
        NAME_KEY, SIMPLE_ITEM_KEY, VALUE_KEY,
    };
    use crate::onvif_metadata_processor::OnvifMetadata;
    use serde_json::Value;
    use streaming_traits::EventProcessor;

    #[tokio::test]
    async fn test_post_process_event() {
        let event_processor_client = OnvifMetadata::new();
        let event = r#"<?xml version="1.0" encoding="UTF-8"?>
<tt:MetadataStream
    xmlns:tt="http://www.onvif.org/ver10/schema"
    xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"
    xmlns:fc="http://www.onvif.org/ver20/analytics/humanface"
    xmlns:bd="http://www.onvif.org/ver20/analytics/humanbody"
    xmlns:acme="http://www.acme.com/schema">
    <tt:VideoAnalytics>
        <tt:Frame UtcTime="2023-09-27T18:40:35" Source="VA_HEAT_BASIC">
            <tt:Transformation>
                <tt:Translate x="-1.0" y="1.0"></tt:Translate>
                <tt:Scale x="1.0" y="1.0"></tt:Scale>
            </tt:Transformation>
            <tt:Object ObjectId="2">
                <tt:Appearance>
                    <tt:Shape>
                        <tt:BoundingBox bottom="0.4911024272441864" top="0.2667100727558136" right="0.4150390625" left="0.298583984375"></tt:BoundingBox>
                        <tt:CenterOfGravity x="0.3568115234375" y="0.37890625"></tt:CenterOfGravity>
                    </tt:Shape>
                    <tt:Class>
                        <tt:Type Likelihood="0.41111087799072266">Person</tt:Type>
                    </tt:Class>
                </tt:Appearance>
            </tt:Object>
        </tt:Frame>
    </tt:VideoAnalytics>
</tt:MetadataStream>"#;

        let processed_event = event_processor_client.post_process_event(event.to_string()).unwrap();
        let event_json: Value = serde_json::from_str(&processed_event).unwrap();
        let model_version = &event_json[MODEL_VERSION_KEY].as_str();
        let model_name = &event_json[MODEL_NAME_KEY].as_str();
        assert_eq!(model_version.unwrap(), MODEL_VERSION);
        assert_eq!(model_name.unwrap(), BASIC_EVENT);
    }

    #[tokio::test]
    async fn test_get_motion_based_event() {
        let event_processor_client = OnvifMetadata::new();
        let event = r#"<?xml version="1.0" encoding="UTF-8"?>
<tt:MetadataStream xmlns:tt="http://www.onvif.org/ver10/schema" xmlns:tns1="http://www.onvif.org/ver10/topics" xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2">
   <tt:Event>
      <wsnt:NotificationMessage>
         <wsnt:Topic Dialect="http://www.onvif.org/ver10/tev/topicExpression/ConcreteSet">tns1:VideoSource/MotionAlarm</wsnt:Topic>
         <wsnt:Message>
            <tt:Message UtcTime="2024-07-31T23:58:20" PropertyOperation="Changed">
               <tt:Source>
                  <tt:SimpleItem Name="Source" Value="vs1" />
               </tt:Source>
               <tt:Data>
                  <tt:SimpleItem Name="State" Value="true" />
               </tt:Data>
            </tt:Message>
         </wsnt:Message>
      </wsnt:NotificationMessage>
   </tt:Event>
</tt:MetadataStream>"#;
        let processed_event =
            event_processor_client.get_motion_based_event(event.to_string()).unwrap().unwrap();
        assert_eq!(
            processed_event[EVENT_SOURCE_KEY][SIMPLE_ITEM_KEY][NAME_KEY].as_str(),
            Some(EVENT_SOURCE_KEY)
        );
        assert_eq!(processed_event[DATA_KEY][SIMPLE_ITEM_KEY][VALUE_KEY].as_bool(), Some(true));
    }
}
