use event_processor::onvif_metadata_processor::OnvifMetadata;
use streaming_traits::EventProcessor;

pub fn get_event_processor_client() -> Box<dyn EventProcessor + Send + Sync> {
    Box::new(OnvifMetadata::new())
}
