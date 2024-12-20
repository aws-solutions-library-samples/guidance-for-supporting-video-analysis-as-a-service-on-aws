/// SQL uses signed integers of up to 8 bytes.  So max non-negative is used.
pub(crate) const MAX_TIME_FOR_DB: u64 = i64::MAX as u64;
/// Query size for delete (Limit to 3 although 2 should be the max needed)
pub(crate) const QUERY_SIZE_FOR_DELETE: u64 = 3;
/// SQLite DB file name
pub(crate) const DB_FILE_NAME: &str = "frame_metadata.txt";
/// SQLite DB journal file name, based on DB_FILE_NAME + "-journal"
pub(crate) const DB_JOURNAL_NAME: &str = "frame_metadata.txt-journal";
/// SQLite DB file name
pub(crate) const FRAGMENT_BASE_NAME: &str = "video-fragment";
